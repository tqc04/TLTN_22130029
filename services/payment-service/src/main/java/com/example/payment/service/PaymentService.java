package com.example.payment.service;

import com.example.payment.entity.Payment;
import com.example.payment.entity.PaymentMethod;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.entity.RiskLevel;
import com.example.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@EnableCaching
@Transactional(rollbackFor = Exception.class)
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private StripeService stripeService;

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private BankTransferService bankTransferService;

    @Autowired
    private OrderServiceClient orderServiceClient;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${services.order.base-url:http://localhost:8084}")
    private String orderServiceUrl;

    @Value("${services.notification.base-url:http://localhost:8086}")
    private String notificationServiceUrl;

    @Value("${interservice.username:service}")
    private String interServiceUsername;

    @Value("${interservice.password:service123}")
    private String interServicePassword;

    /**
     * Fetch order by orderNumber from order service using circuit breaker
     */
    public VNPayService.OrderDTO fetchOrderByNumber(String orderNumber) {
        try {
            // Use orderServiceClient with circuit breaker
            var orderDetails = orderServiceClient.getOrderByNumber(orderNumber);
            if (orderDetails != null) {
                VNPayService.OrderDTO orderDTO = new VNPayService.OrderDTO();
                orderDTO.id = Long.valueOf(orderDetails.get("id").toString());
                orderDTO.orderNumber = (String) orderDetails.get("orderNumber");
                orderDTO.totalAmount = orderDetails.get("totalAmount") != null ?
                        new BigDecimal(orderDetails.get("totalAmount").toString()) : null;
                return orderDTO;
            }
            return null;
        } catch (Exception e) {
            System.err.println("Failed to fetch order by number " + orderNumber + " from order service: " + e.getMessage());
            return null;
        }
    }

    /**
     * Update order status after successful payment with retry logic
     */
    private void updateOrderStatus(Long orderId, String status) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                String url = orderServiceUrl + "/api/orders/" + orderId + "/confirm";

                // Add authentication headers for inter-service communication
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setBasicAuth(interServiceUsername, interServicePassword); // Use configurable service account credentials

                org.springframework.http.HttpEntity<Map<String, Object>> requestEntity =
                        new org.springframework.http.HttpEntity<>(null, headers);

                ParameterizedTypeReference<Map<String, Object>> typeRef =
                        new ParameterizedTypeReference<Map<String, Object>>() {};
                ResponseEntity<Map<String, Object>> responseEntity =
                        restTemplate.exchange(url, HttpMethod.POST, requestEntity, typeRef);
                Map<String, Object> response = responseEntity.getBody();

                if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                    logger.info("Successfully updated order {} status to {}", orderId, status);
                    return; // Success, exit retry loop
                } else {
                    throw new RuntimeException("Order service returned failure response: " + response);
                }

            } catch (Exception e) {
                retryCount++;
                logger.error("Failed to update order status for orderId {} (attempt {}/{}): {}",
                        orderId, retryCount, maxRetries, e.getMessage());

                if (retryCount >= maxRetries) {
                    // Final failure - this is critical for e-commerce
                    logger.error("CRITICAL: Failed to update order status after {} attempts for order: {}",
                            maxRetries, orderId);
                    handleCriticalOperationFailure(orderId, status, e);
                    throw new RuntimeException("Failed to update order status after " + maxRetries + " attempts");
                }

                // Wait before retry (exponential backoff)
                try {
                    Thread.sleep(1000 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
    }

    /**
     * Rollback payment and update order status
     */
    public void rollbackPayment(Payment payment, String reason) {
        try {
            // Mark payment as failed
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            paymentRepository.save(payment);

            // Send payment failed notification
            sendPaymentNotification(payment, "FAILED");

            // Update order status to payment failed
            updateOrderStatus(payment.getOrderId(), "PAYMENT_FAILED");

            logger.info("Payment rollback completed for payment: {}", payment.getId());
        } catch (Exception e) {
            logger.error("Failed to rollback payment {}: {}", payment.getId(), e.getMessage());
        }
    }

    /**
     * Create payment record
     */
    @CacheEvict(value = "payments", allEntries = true)
    public Payment createPayment(Long orderId, String orderNumber, String userId,
                                 PaymentMethod paymentMethod, BigDecimal amount,
                                 String currency, String ipAddress, String userAgent) {
        // Handle null userId - set default for anonymous users
        if (userId == null || userId.isEmpty()) {
            userId = "anonymous"; // Default user ID for anonymous users
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setOrderNumber(orderNumber);
        payment.setUserId(userId);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setIpAddress(ipAddress);
        payment.setUserAgent(userAgent);

        return paymentRepository.save(payment);
    }

    /**
     * Process payment based on method
     */
    public Map<String, Object> processPayment(Payment payment) {
        try {
            // Validate payment object
            if (payment == null) {
                return Map.of("success", false, "error", "Payment object is null");
            }

            if (payment.getPaymentMethod() == null) {
                return Map.of("success", false, "error", "Payment method is null");
            }

            Map<String, Object> result = new HashMap<>();

            switch (payment.getPaymentMethod()) {
                case STRIPE:
                case CREDIT_CARD:
                    result = processStripePayment(payment);
                    break;
                case VNPAY:
                    result = processVNPayPayment(payment);
                    break;
                case MOMO:
                    result = processMoMoPayment(payment);
                    break;
                case BANK_TRANSFER:
                    result = processBankTransferPayment(payment);
                    break;
                case CASH_ON_DELIVERY:
                case COD:
                    result = processCODPayment(payment);
                    break;
                default:
                    result = Map.of("success", false, "error", "Unsupported payment method: " + payment.getPaymentMethod());
            }

            return result;
        } catch (Exception e) {
            logger.error("Error processing payment: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Payment processing failed: " + e.getMessage());
        }
    }

    /**
     * Process Stripe payment
     */
    private Map<String, Object> processStripePayment(Payment payment) {
        try {
            Long amountCents = payment.getAmount().multiply(new BigDecimal("100")).longValue();
            Map<String, Object> stripeResult = stripeService.createPaymentIntent(amountCents, payment.getCurrency());

            if ((Boolean) stripeResult.get("success")) {
                payment.setTransactionId((String) stripeResult.get("id"));
                payment.setGatewayResponse(stripeResult.toString());
                payment.setStatus(PaymentStatus.PROCESSING);
                paymentRepository.save(payment);

                return Map.of(
                        "success", true,
                        "paymentId", payment.getId(),
                        "clientSecret", stripeResult.get("clientSecret"),
                        "paymentIntentId", stripeResult.get("id")
                );
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Stripe payment creation failed");
                paymentRepository.save(payment);

                // Send payment failed notification
                sendPaymentNotification(payment, "FAILED");

                return Map.of("success", false, "error", "Payment creation failed");
            }
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);

            // Send payment failed notification
            sendPaymentNotification(payment, "FAILED");

            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Process VNPay payment
     */
    public Map<String, Object> processVNPayPayment(Payment payment) {
        try {
            // Validate payment object
            if (payment == null) {
                return Map.of("success", false, "error", "Payment object is null");
            }

            if (payment.getOrderId() == null) {
                return Map.of("success", false, "error", "Payment orderId is null");
            }

            // Validate VNPay configuration BEFORE creating payment URL
            Map<String, Object> configValidation = vnPayService.validateConfiguration();
            if (!(Boolean) configValidation.get("success")) {
                String errorMsg = (String) configValidation.getOrDefault("error", "VNPay configuration is invalid");
                logger.error("VNPay configuration validation failed: {}", errorMsg);

                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("VNPay configuration error: " + errorMsg);
                paymentRepository.save(payment);

                // Send payment failed notification
                sendPaymentNotification(payment, "FAILED");

                // Auto-cancel order if VNPay is not properly configured
                try {
                    if (payment.getOrderNumber() != null) {
                        orderServiceClient.cancelOrderByNumber(payment.getOrderNumber(),
                                "VNPay configuration invalid: " + errorMsg);
                        logger.info("Cancelled order {} due to VNPay configuration error", payment.getOrderNumber());
                    }
                } catch (Exception cancelEx) {
                    logger.error("Failed to cancel order {} after VNPay config error: {}",
                            payment.getOrderNumber(), cancelEx.getMessage());
                }

                return Map.of("success", false, "error", errorMsg);
            }

            String ipAddress = payment.getIpAddress() != null ? payment.getIpAddress() : "127.0.0.1";
            Map<String, Object> vnpayResult = vnPayService.createPaymentUrl(payment.getOrderId(), payment.getOrderNumber(), ipAddress);

            if ((Boolean) vnpayResult.get("success")) {
                payment.setTransactionId((String) vnpayResult.get("orderNumber"));
                payment.setGatewayResponse(vnpayResult.toString());
                payment.setStatus(PaymentStatus.PROCESSING);
                paymentRepository.save(payment);

                return Map.of(
                        "success", true,
                        "paymentId", payment.getId(),
                        "paymentUrl", vnpayResult.get("paymentUrl"),
                        "orderNumber", vnpayResult.get("orderNumber")
                );
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason((String) vnpayResult.get("error"));
                paymentRepository.save(payment);

                // Send payment failed notification
                sendPaymentNotification(payment, "FAILED");

                // Auto-cancel order if payment URL creation failed
                try {
                    if (payment.getOrderNumber() != null) {
                        orderServiceClient.cancelOrderByNumber(payment.getOrderNumber(),
                                "VNPay payment URL creation failed: " + vnpayResult.get("error"));
                        logger.info("Cancelled order {} due to VNPay URL creation failure", payment.getOrderNumber());
                    }
                } catch (Exception cancelEx) {
                    logger.error("Failed to cancel order {} after VNPay URL creation failure: {}",
                            payment.getOrderNumber(), cancelEx.getMessage());
                }

                return Map.of("success", false, "error", vnpayResult.get("error"));
            }
        } catch (Exception e) {
            logger.error("Error processing VNPay payment: {}", e.getMessage(), e);

            if (payment != null) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(e.getMessage());
                try {
                    paymentRepository.save(payment);
                    // Send payment failed notification
                    sendPaymentNotification(payment, "FAILED");
                } catch (Exception saveError) {
                    logger.error("Failed to save failed payment: {}", saveError.getMessage());
                }
            }

            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Process MoMo payment (placeholder - basic implementation)
     */
    public Map<String, Object> processMoMoPayment(Payment payment) {
        try {
            // Validate payment object
            if (payment == null) {
                return Map.of("success", false, "error", "Payment object is null");
            }

            if (payment.getOrderNumber() == null) {
                payment.setOrderNumber(String.valueOf(payment.getOrderId()));
            }

            // For now, we'll treat MoMo as a redirect-based payment like VNPay
            // In a real implementation, this would integrate with MoMo's API

            // Generate a mock payment URL for testing
            String orderNumber = payment.getOrderNumber() != null ? payment.getOrderNumber() : String.valueOf(payment.getOrderId());
            BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            String currency = payment.getCurrency() != null ? payment.getCurrency() : "VND";

            String mockPaymentUrl = "https://momo.vn/checkout?order=" + orderNumber +
                    "&amount=" + amount + "&currency=" + currency;

            payment.setTransactionId("MOMO_" + orderNumber);
            payment.setGatewayResponse("MoMo payment initiated");
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            return Map.of(
                    "success", true,
                    "paymentId", payment.getId(),
                    "paymentUrl", mockPaymentUrl,
                    "orderNumber", orderNumber,
                    "message", "MoMo payment initiated - redirect to payment gateway"
            );
        } catch (Exception e) {
            logger.error("Error processing MoMo payment: {}", e.getMessage(), e);

            if (payment != null) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(e.getMessage());
                try {
                    paymentRepository.save(payment);
                } catch (Exception saveError) {
                    logger.error("Failed to save failed payment: {}", saveError.getMessage());
                }
            }

            return Map.of("success", false, "error", "MoMo payment initialization failed: " + e.getMessage());
        }
    }

    /**
     * Process Bank Transfer payment
     */
    private Map<String, Object> processBankTransferPayment(Payment payment) {
        try {
            Map<String, Object> bankResult = bankTransferService.createBankTransferInfo(payment);

            if ((Boolean) bankResult.get("success")) {
                payment.setTransactionId((String) bankResult.get("transactionId"));
                payment.setGatewayResponse(bankResult.toString());
                payment.setStatus(PaymentStatus.PENDING);
                paymentRepository.save(payment);

                return Map.of(
                        "success", true,
                        "paymentId", payment.getId(),
                        "bankInfo", bankResult.get("bankInfo"),
                        "qrCode", bankResult.get("qrCode")
                );
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason((String) bankResult.get("error"));
                paymentRepository.save(payment);

                // Send payment failed notification
                sendPaymentNotification(payment, "FAILED");

                return Map.of("success", false, "error", bankResult.get("error"));
            }
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);

            // Send payment failed notification
            sendPaymentNotification(payment, "FAILED");

            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Process Cash on Delivery payment
     */
    private Map<String, Object> processCODPayment(Payment payment) {
        try {
            payment.setTransactionId("COD_" + payment.getOrderNumber());
            payment.setPaymentDate(LocalDateTime.now());
            payment.setGatewayResponse("COD payment - no gateway interaction required");
            payment.setStatus(PaymentStatus.COMPLETED); // COD is automatically completed
            paymentRepository.save(payment);

            // Perform fraud analysis
            performFraudAnalysis(payment);

            // Send payment success notification
            sendPaymentNotification(payment, "SUCCESS");

            // Order status will be updated by Order Service

            return Map.of(
                    "success", true,
                    "paymentId", payment.getId(),
                    "status", payment.getStatus().name(),
                    "transactionId", payment.getTransactionId(),
                    "message", "Order confirmed for cash on delivery"
            );
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);

            // Send payment failed notification
            sendPaymentNotification(payment, "FAILED");

            return Map.of("success", false, "error", "COD payment processing failed: " + e.getMessage());
        }
    }

    /**
     * Confirm payment
     */
    public Map<String, Object> confirmPayment(Long paymentId, String transactionId) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (paymentOpt.isEmpty()) {
                return Map.of("success", false, "error", "Payment not found");
            }

            Payment payment = paymentOpt.get();

            // Update payment with transaction details
            payment.setTransactionId(transactionId);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            // Perform fraud analysis
            performFraudAnalysis(payment);

            // Send payment success notification
            sendPaymentNotification(payment, "SUCCESS");

            // Update order status to confirmed
            updateOrderStatus(payment.getOrderId(), "CONFIRMED");

            return Map.of(
                    "success", true,
                    "paymentId", payment.getId(),
                    "status", payment.getStatus().name(),
                    "transactionId", transactionId
            );
        } catch (Exception e) {
            logger.error("Error confirming payment: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Process VNPay callback
     */
    public Map<String, Object> processVNPayCallback(Map<String, String> callbackParams) {
        try {
            String orderNumber = callbackParams.get("vnp_TxnRef");
            String responseCode = callbackParams.get("vnp_ResponseCode");
            String transactionId = callbackParams.get("vnp_TransactionNo");

            // Validate signature first
            Map<String, Object> vnpayResult = vnPayService.processCallback(callbackParams);
            boolean signatureValid = (Boolean) vnpayResult.get("success");

            // Process payment even if signature validation fails (for error cases like code=71)
            // We still need to update payment status and cancel order
            if (orderNumber != null && !orderNumber.isEmpty()) {
                List<Payment> payments = paymentRepository.findByOrderNumber(orderNumber);
                if (!payments.isEmpty()) {
                    Payment payment = payments.get(0);

                    if (transactionId != null) {
                        payment.setTransactionId(transactionId);
                    }
                    payment.setPaymentDate(LocalDateTime.now());
                    payment.setGatewayResponse(callbackParams.toString());

                    if (signatureValid && "00".equals(responseCode)) {
                        // Thanh toán thành công (signature valid + responseCode = 00)
                        payment.setStatus(PaymentStatus.COMPLETED);
                        paymentRepository.save(payment);

                        // Phân tích fraud và gửi thông báo thành công
                        performFraudAnalysis(payment);
                        sendPaymentNotification(payment, "SUCCESS");

                        // Order status sẽ được Order Service cập nhật
                    } else {
                        // Thanh toán thất bại từ VNPay (kể cả code=71, 24, ...)
                        // Hoặc signature validation failed
                        String reason;
                        if (responseCode != null) {
                            // Map error codes to descriptive messages
                            switch (responseCode) {
                                case "03":
                                    reason = "VNPay Error Code 03: Invalid data format. Check payment parameters (vnp_TxnRef, vnp_OrderInfo, vnp_Amount, etc.)";
                                    logger.error("VNPay Error Code 03 for order {}: Invalid data format. Check all payment parameters.", orderNumber);
                                    break;
                                case "71":
                                    reason = "VNPay Error Code 71: Website/Application chưa được VNPAY phê duyệt. Cần đăng ký domain/IP trong merchant account.";
                                    logger.error("VNPay Error Code 71 for order {}: Website not approved. Please register domain/IP in VNPay merchant account.", orderNumber);
                                    break;
                                case "07":
                                    reason = "VNPay Error Code 07: Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)";
                                    break;
                                case "09":
                                    reason = "VNPay Error Code 09: Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking";
                                    break;
                                case "10":
                                    reason = "VNPay Error Code 10: Xác thực thông tin thẻ/tài khoản không đúng";
                                    break;
                                case "11":
                                    reason = "VNPay Error Code 11: Đã hết hạn chờ thanh toán";
                                    break;
                                case "12":
                                    reason = "VNPay Error Code 12: Thẻ/Tài khoản bị khóa";
                                    break;
                                case "13":
                                    reason = "VNPay Error Code 13: Nhập sai mật khẩu xác thực giao dịch (OTP)";
                                    break;
                                case "51":
                                    reason = "VNPay Error Code 51: Tài khoản không đủ số dư để thực hiện giao dịch";
                                    break;
                                case "65":
                                    reason = "VNPay Error Code 65: Tài khoản đã vượt quá hạn mức giao dịch trong ngày";
                                    break;
                                case "75":
                                    reason = "VNPay Error Code 75: Ngân hàng thanh toán đang bảo trì";
                                    break;
                                case "79":
                                    reason = "VNPay Error Code 79: Nhập sai mật khẩu thanh toán quá số lần quy định";
                                    break;
                                case "99":
                                    reason = "VNPay Error Code 99: Lỗi không xác định từ ngân hàng";
                                    break;
                                default:
                                    reason = "VNPay response code: " + responseCode;
                                    break;
                            }
                        } else if (!signatureValid) {
                            reason = "VNPay signature validation failed - possible configuration error (check Merchant ID and Secret Key)";
                            logger.error("VNPay signature validation failed for order {}: Possible configuration error", orderNumber);
                        } else {
                            reason = "VNPay signature validation failed or missing response code";
                        }

                        payment.setStatus(PaymentStatus.FAILED);
                        payment.setFailureReason(reason);
                        paymentRepository.save(payment);

                        // Gửi thông báo thất bại
                        sendPaymentNotification(payment, "FAILED");

                        // Tự động hủy đơn hàng tương ứng để "rollback" luồng mua hàng
                        try {
                            Map<String, Object> cancelResult =
                                    orderServiceClient.cancelOrderByNumber(orderNumber, reason);
                            logger.info("Cancelled order {} due to VNPay failure (code={}, signatureValid={}): {}",
                                    orderNumber, responseCode, signatureValid, cancelResult);
                        } catch (Exception cancelEx) {
                            logger.error("Failed to cancel order {} after VNPay failure (code={}): {}",
                                    orderNumber, responseCode, cancelEx.getMessage(), cancelEx);
                        }
                    }
                } else {
                    logger.warn("Payment not found for orderNumber: {}", orderNumber);
                }
            } else {
                logger.warn("Order number not found in VNPay callback params: {}", callbackParams);
            }

            // Return result with signature validation status
            return vnpayResult;
        } catch (Exception e) {
            logger.error("Error processing VNPay callback: {}", e.getMessage(), e);

            // Try to cancel order even if callback processing fails
            String orderNumber = callbackParams.get("vnp_TxnRef");
            if (orderNumber != null && !orderNumber.isEmpty()) {
                try {
                    orderServiceClient.cancelOrderByNumber(orderNumber,
                            "VNPay callback processing error: " + e.getMessage());
                    logger.info("Cancelled order {} due to callback processing error", orderNumber);
                } catch (Exception cancelEx) {
                    logger.error("Failed to cancel order {} after callback error: {}",
                            orderNumber, cancelEx.getMessage());
                }
            }

            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Process Stripe payment confirmation
     */
    public Map<String, Object> confirmStripePayment(String paymentIntentId) {
        try {
            Map<String, Object> stripeResult = stripeService.confirmPayment(paymentIntentId);

            if ((Boolean) stripeResult.get("success")) {
                // Find payment by transaction ID
                Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(paymentIntentId);
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setPaymentDate(LocalDateTime.now());
                    payment.setGatewayResponse(stripeResult.toString());
                    paymentRepository.save(payment);

                    // Perform fraud analysis
                    performFraudAnalysis(payment);

                    // Send payment success notification
                    sendPaymentNotification(payment, "SUCCESS");

                    // Order status will be updated by Order Service
                }
            }

            return stripeResult;
        } catch (Exception e) {
            logger.error("Error confirming Stripe payment: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send payment notification to Notification Service
     */
    private void sendPaymentNotification(Payment payment, String status) {
        try {
            String url = notificationServiceUrl + "/api/notifications/payment";
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", payment.getUserId());
            notification.put("paymentId", payment.getId().toString());
            notification.put("orderId", payment.getOrderId());
            notification.put("orderNumber", payment.getOrderNumber());
            notification.put("status", status);
            notification.put("amount", payment.getAmount() != null ? payment.getAmount().doubleValue() : 0);
            notification.put("transactionId", payment.getTransactionId());
            notification.put("paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "UNKNOWN");

            restTemplate.postForEntity(url, notification, Map.class);
            logger.info("Payment {} notification sent for payment: {}", status, payment.getId());
        } catch (Exception e) {
            // Log error but don't fail payment processing
            logger.error("Failed to send payment {} notification for payment {}: {}",
                    status, payment.getId(), e.getMessage());
        }
    }

    /**
     * Perform fraud analysis
     */
    private void performFraudAnalysis(Payment payment) {
        try {
            Map<String, Object> analysis = analyzeFraudRisk(payment);

            Double riskScore = (Double) analysis.get("riskScore");
            String riskLevel = (String) analysis.get("riskLevel");

            payment.setRiskScore(riskScore);
            payment.setRiskLevel(RiskLevel.valueOf(riskLevel));
            payment.setFraudAnalysis(analysis.toString());

            paymentRepository.save(payment);
        } catch (Exception e) {
            logger.warn("Failed to perform fraud analysis for payment {}: {}", payment.getId(), e.getMessage());
        }
    }

    /**
     * Analyze fraud risk
     */
    public Map<String, Object> analyzeFraudRisk(Payment payment) {
        Map<String, Object> analysis = new HashMap<>();
        double riskScore = 0.0;

        // Basic risk factors
        if (payment.getAmount().compareTo(new BigDecimal("1000000")) > 0) {
            riskScore += 0.3; // High amount
        }

        if (payment.getAmount().compareTo(new BigDecimal("10000000")) > 0) {
            riskScore += 0.5; // Very high amount
        }

        // Check for suspicious patterns
        List<Payment> recentPayments = paymentRepository.findByUserIdAndCreatedAtBetween(
                payment.getUserId(),
                LocalDateTime.now().minusHours(24),
                LocalDateTime.now()
        );

        if (recentPayments.size() > 10) {
            riskScore += 0.4; // Too many payments in 24h
        }

        // Check for failed payments
        long failedCount = paymentRepository.countByUserIdAndStatus(payment.getUserId(), PaymentStatus.FAILED);
        if (failedCount > 5) {
            riskScore += 0.3; // High failure rate
        }

        // Determine risk level
        String riskLevel;
        if (riskScore >= 0.8) {
            riskLevel = "CRITICAL";
        } else if (riskScore >= 0.6) {
            riskLevel = "HIGH";
        } else if (riskScore >= 0.3) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        analysis.put("riskScore", riskScore);
        analysis.put("riskLevel", riskLevel);
        analysis.put("factors", List.of(
                "Amount: " + payment.getAmount(),
                "Recent payments: " + recentPayments.size(),
                "Failed payments: " + failedCount
        ));

        return analysis;
    }

    /**
     * Get payment by ID
     */
    @Cacheable(value = "payments", key = "#id")
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    /**
     * Get payments by order ID
     */
    public List<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * Get payments by user ID
     */
    public List<Payment> findByUserId(String userId) {
        return paymentRepository.findByUserId(userId);
    }

    /**
     * Get payments by status
     */
    public List<Payment> findByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    /**
     * Get all payments with pagination
     */
    public Page<Payment> findAll(Pageable pageable) {
        return paymentRepository.findAll(pageable);
    }

    /**
     * Get payment statistics
     */
    public Map<String, Object> getPaymentStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalPayments", paymentRepository.count());
        stats.put("completedPayments", paymentRepository.countByUserIdAndStatus(null, PaymentStatus.COMPLETED));
        stats.put("failedPayments", paymentRepository.countByUserIdAndStatus(null, PaymentStatus.FAILED));
        stats.put("pendingPayments", paymentRepository.countByUserIdAndStatus(null, PaymentStatus.PENDING));

        return stats;
    }

    /**
     * Refund payment
     */
    public Map<String, Object> refundPayment(Long paymentId, BigDecimal amount, String reason) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (paymentOpt.isEmpty()) {
                return Map.of("success", false, "error", "Payment not found");
            }

            Payment payment = paymentOpt.get();

            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                return Map.of("success", false, "error", "Only completed payments can be refunded");
            }

            if (amount.compareTo(payment.getAmount()) > 0) {
                return Map.of("success", false, "error", "Refund amount cannot exceed payment amount");
            }

            // Process refund based on payment method
            Map<String, Object> refundResult = processRefund(payment, amount, reason);

            if ((Boolean) refundResult.get("success")) {
                payment.setRefundAmount(amount);
                payment.setRefundReason(reason);
                payment.setRefundDate(LocalDateTime.now());
                payment.setRefundReference((String) refundResult.get("refundReference"));

                if (amount.compareTo(payment.getAmount()) == 0) {
                    payment.setStatus(PaymentStatus.REFUNDED);
                } else {
                    payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
                }

                paymentRepository.save(payment);
            }

            return refundResult;
        } catch (Exception e) {
            logger.error("Error refunding payment: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Refund payment by order number (used by Order Service when cancelling orders)
     */
    public Map<String, Object> refundPaymentByOrderNumber(String orderNumber, String reason) {
        try {
            if (orderNumber == null || orderNumber.isBlank()) {
                return Map.of("success", false, "error", "Order number is required");
            }

            List<Payment> payments = paymentRepository.findByOrderNumber(orderNumber);
            if (payments.isEmpty()) {
                return Map.of("success", false, "error", "No payment found for order " + orderNumber);
            }

            // Prefer completed VNPay payment if available
            Optional<Payment> targetOpt = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.COMPLETED
                            && p.getPaymentMethod() == PaymentMethod.VNPAY)
                    .findFirst();

            if (targetOpt.isEmpty()) {
                return Map.of("success", false, "error", "No completed VNPay payment found for order " + orderNumber);
            }

            Payment payment = targetOpt.get();
            BigDecimal amount = payment.getAmount();
            if (amount == null) {
                return Map.of("success", false, "error", "Payment amount is null for order " + orderNumber);
            }

            return refundPayment(payment.getId(), amount, reason != null ? reason : "Order cancelled");
        } catch (Exception e) {
            logger.error("Error refunding payment by orderNumber {}: {}", orderNumber, e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Process refund based on payment method
     */
    private Map<String, Object> processRefund(Payment payment, BigDecimal amount, String reason) {
        switch (payment.getPaymentMethod()) {
            case STRIPE:
            case CREDIT_CARD:
                return stripeService.refundPayment(payment.getTransactionId(), amount);
            case VNPAY:
                return vnPayService.refundPayment(payment.getOrderNumber(), amount, reason);
            case BANK_TRANSFER:
                return bankTransferService.processRefund(payment, amount, reason);
            default:
                return Map.of("success", false, "error", "Refund not supported for this payment method");
        }
    }

    /**
     * Get high risk payments
     */
    public List<Payment> getHighRiskPayments() {
        return paymentRepository.findHighRiskPayments(RiskLevel.HIGH);
    }

    /**
     * Get failed payments
     */
    public List<Payment> getFailedPayments() {
        return paymentRepository.findFailedPayments(LocalDateTime.now().minusDays(7));
    }

    /**
     * Handle critical operation failures that require manual intervention
     * This method provides a centralized way to handle failures in critical operations
     * such as order status updates, payment processing, etc.
     */
    private void handleCriticalOperationFailure(Long orderId, String targetStatus, Exception lastException) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String failureId = UUID.randomUUID().toString();

        // Log detailed failure information for manual intervention
        logger.error("=== CRITICAL OPERATION FAILURE ===");
        logger.error("Failure ID: {}", failureId);
        logger.error("Timestamp: {}", timestamp);
        logger.error("Order ID: {}", orderId);
        logger.error("Target Status: {}", targetStatus);
        logger.error("Last Exception Type: {}", lastException.getClass().getSimpleName());
        logger.error("Last Exception Message: {}", lastException.getMessage());
        logger.error("Stack Trace: {}", (Object) java.util.Arrays.toString(lastException.getStackTrace()));
        logger.error("=== END CRITICAL FAILURE ===");



        // For now, we'll create a structured log that can be easily parsed by log aggregation tools
        Map<String, Object> failureData = new HashMap<>();
        failureData.put("failureId", failureId);
        failureData.put("timestamp", timestamp);
        failureData.put("service", "payment-service");
        failureData.put("operation", "updateOrderStatus");
        failureData.put("orderId", orderId);
        failureData.put("targetStatus", targetStatus);
        failureData.put("exceptionType", lastException.getClass().getSimpleName());
        failureData.put("exceptionMessage", lastException.getMessage());
        failureData.put("requiresManualIntervention", true);
        failureData.put("priority", "CRITICAL");
        failureData.put("orderServiceUrl", orderServiceUrl);

        // Log as JSON for easy parsing by monitoring systems
        logger.error("STRUCTURED_FAILURE_DATA: {}", failureData);

        // In a real production system, you might want to:
        // 1. Send this to a message queue for async processing
        // 2. Store in a dedicated failure tracking table
        // 3. Send email/SMS notifications to admins
        // 4. Create support tickets automatically

        logger.error("MANUAL INTERVENTION REQUIRED: Order {} status update failed. " +
                "Please check order service connectivity and manually update order status if necessary. " +
                "Failure details logged above with ID: {}", orderId, failureId);
    }
}
