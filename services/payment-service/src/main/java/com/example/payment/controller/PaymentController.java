package com.example.payment.controller;

import com.example.payment.entity.Payment;
import com.example.payment.entity.PaymentMethod;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.service.BankTransferService;
import com.example.payment.service.OrderServiceClient;
import com.example.payment.service.PaymentService;
import com.example.payment.service.StripeService;
import com.example.payment.service.VNPayService;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private StripeService stripeService;
    
    @Autowired
    private VNPayService vnPayService;
    
    @Autowired
    private BankTransferService bankTransferService;

    @Autowired
    private OrderServiceClient orderServiceClient;

    @GetMapping("/health")
    @PermitAll
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    /**
     * Create payment
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPayment(
            @RequestBody Map<String, Object> paymentRequest,
            HttpServletRequest request) {
        try {
            String orderNumber = (String) paymentRequest.get("orderNumber");
            Long orderId = null;

            // Try to get orderId from request, fallback to parsing orderNumber if it's a number
            if (paymentRequest.get("orderId") != null) {
                orderId = Long.valueOf(paymentRequest.get("orderId").toString());
            } else if (orderNumber != null && orderNumber.matches("\\d+")) {
                // If orderNumber is a number (like from parseInt), use it as orderId
                orderId = Long.valueOf(orderNumber);
            } else if (orderNumber != null && !orderNumber.isEmpty()) {
                // For orderNumber like "ORD-123456", try to fetch the actual orderId from order service
                var orderDetails = orderServiceClient.getOrderByNumber(orderNumber);
                if (orderDetails != null && orderDetails.get("id") != null) {
                    orderId = Long.valueOf(orderDetails.get("id").toString());
                } else {
                    // If can't fetch order, use a temporary orderId
                    orderId = 1L; // Temporary fallback
                }
            }
            String userId = paymentRequest.get("userId").toString();
            String paymentMethodStr = (String) paymentRequest.get("paymentMethod");
            BigDecimal amount = new BigDecimal(paymentRequest.get("amount").toString());
            String currency = (String) paymentRequest.getOrDefault("currency", "VND");

            // Ensure we have the correct orderNumber from order service
            if (orderNumber != null && orderNumber.matches("ORD-\\d+")) {
                // orderNumber is already in correct format, use it
            } else if (orderId != null) {
                // Try to fetch orderNumber from order service using circuit breaker
                var orderDetails = orderServiceClient.getOrderById(orderId);
                if (orderDetails != null && orderDetails.get("orderNumber") != null) {
                    orderNumber = (String) orderDetails.get("orderNumber");
                }
            }

            PaymentMethod paymentMethod = PaymentMethod.valueOf(paymentMethodStr.toUpperCase());
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // Create payment record
            Payment payment = paymentService.createPayment(orderId, orderNumber, userId,
                paymentMethod, amount, currency, ipAddress, userAgent);
            
            // Process payment
            Map<String, Object> result = paymentService.processPayment(payment);
            result.put("paymentId", payment.getId());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Confirm payment
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirmPayment(
            @RequestBody Map<String, String> request) {
        try {
            Long paymentId = Long.valueOf(request.get("paymentId"));
            String transactionId = request.get("transactionId");
            
            Map<String, Object> result = paymentService.confirmPayment(paymentId, transactionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * VNPay callback
     */
    @GetMapping("/vnpay/callback")
    @PermitAll
    public ResponseEntity<Map<String, Object>> vnpayCallback(
            @RequestParam Map<String, String> callbackParams) {
        try {
            Map<String, Object> result = paymentService.processVNPayCallback(callbackParams);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * VNPay return URL (from VNPay directly)
     * Redirects to frontend payment result page instead of returning JSON
     */
    @GetMapping("/vnpay/return")
    @PermitAll
    public void vnpayReturn(
            @RequestParam Map<String, String> returnParams,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        try {
            // Process VNPay callback (validate signature, update payment status)
            paymentService.processVNPayCallback(returnParams);
            
            // Extract order number from transaction reference
            String orderNumber = returnParams.get("vnp_TxnRef");
            String responseCode = returnParams.get("vnp_ResponseCode");
            String transactionNo = returnParams.get("vnp_TransactionNo");
            String amount = returnParams.get("vnp_Amount");
            
            boolean isSuccess = "00".equals(responseCode);
            
            // Build redirect URL to frontend
            String redirectUrl;
            if (isSuccess && orderNumber != null) {
                String message = String.format("Thanh toán thành công với mã giao dịch %s. Số tiền: %s VND", 
                    transactionNo != null ? transactionNo : "N/A",
                    amount != null ? String.valueOf(Long.parseLong(amount) / 100) : "0");
                redirectUrl = String.format("http://localhost:3000/payment-result?orderNumber=%s&success=true&status=SUCCESS&message=%s&paymentMethod=VNPAY",
                    orderNumber,
                    java.net.URLEncoder.encode(message, "UTF-8"));
            } else {
                // Get user-friendly error message based on response code
                String errorMessage = getVNPayErrorMessage(responseCode);
                redirectUrl = String.format("http://localhost:3000/payment-result?success=false&status=FAILED&message=%s&paymentMethod=VNPAY",
                    java.net.URLEncoder.encode(errorMessage, "UTF-8"));
            }
            
            // Redirect to frontend
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            try {
                String errorMessage = "Có lỗi xảy ra khi xử lý thanh toán: " + e.getMessage();
                String redirectUrl = String.format("http://localhost:3000/payment-result?success=false&status=ERROR&message=%s",
                    java.net.URLEncoder.encode(errorMessage, "UTF-8"));
                response.sendRedirect(redirectUrl);
            } catch (Exception redirectException) {
                // Fallback: return JSON if redirect fails
                response.setStatus(500);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    /**
     * Get user-friendly error message for VNPay response codes
     */
    private String getVNPayErrorMessage(String responseCode) {
        if (responseCode == null || responseCode.isEmpty()) {
            return "Thanh toán không thành công. Vui lòng thử lại hoặc liên hệ hỗ trợ.";
        }
        
        // Map VNPay error codes to user-friendly messages
        switch (responseCode) {
            case "00":
                return "Thanh toán thành công";
            case "03":
                return "Định dạng dữ liệu không hợp lệ. Vui lòng kiểm tra lại thông tin thanh toán hoặc liên hệ hỗ trợ.";
            case "07":
                return "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09":
                return "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking";
            case "10":
                return "Xác thực thông tin thẻ/tài khoản không đúng. Vui lòng thử lại.";
            case "11":
                return "Đã hết hạn chờ thanh toán. Vui lòng thực hiện lại giao dịch.";
            case "12":
                return "Thẻ/Tài khoản bị khóa.";
            case "13":
                return "Nhập sai mật khẩu xác thực giao dịch (OTP). Vui lòng thử lại.";
            case "51":
                return "Tài khoản không đủ số dư để thực hiện giao dịch.";
            case "65":
                return "Tài khoản đã vượt quá hạn mức giao dịch trong ngày.";
            case "75":
                return "Ngân hàng thanh toán đang bảo trì.";
            case "79":
                return "Nhập sai mật khẩu thanh toán quá số lần quy định. Vui lòng thử lại sau.";
            case "99":
                return "Lỗi không xác định. Vui lòng liên hệ ngân hàng hoặc thử lại sau.";
            case "71":
                return "Website/Ứng dụng chưa được VNPAY phê duyệt. Vui lòng liên hệ quản trị viên hoặc sử dụng phương thức thanh toán khác.";
            default:
                return String.format("Thanh toán không thành công (Mã lỗi: %s). Vui lòng thử lại hoặc liên hệ hỗ trợ.", responseCode);
        }
    }

    /**
     * VNPay return processing (from frontend)
     */
    @PostMapping("/vnpay/return")
    @PermitAll
    public ResponseEntity<Map<String, Object>> processVNPayReturn(
            @RequestBody Map<String, String> returnParams) {
        try {
            Map<String, Object> result = paymentService.processVNPayCallback(returnParams);

            // Add orderNumber for frontend redirect
            if ((Boolean) result.get("success")) {
                String orderNumber = returnParams.get("vnp_TxnRef");
                result.put("orderNumber", orderNumber);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Validate VNPay configuration
     */
    @PermitAll
    @GetMapping("/vnpay/validate")
    public ResponseEntity<Map<String, Object>> validateVNPay() {
        try {
            Map<String, Object> result = vnPayService.validateConfiguration();
            // Add debug info to see actual config values (masked for security)
            Map<String, Object> debugInfo = new HashMap<>(result);
            debugInfo.put("merchantId", vnPayService.getMerchantId() != null ? 
                vnPayService.getMerchantId().substring(0, Math.min(3, vnPayService.getMerchantId().length())) + "***" : "null");
            debugInfo.put("merchantIdLength", vnPayService.getMerchantId() != null ? vnPayService.getMerchantId().length() : 0);
            debugInfo.put("secretKeyLength", vnPayService.getSecretKey() != null ? vnPayService.getSecretKey().length() : 0);
            debugInfo.put("returnUrl", vnPayService.getReturnUrl());
            debugInfo.put("paymentUrl", vnPayService.getPaymentUrl());
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Process payment (called by Order Service)
     */
    @PermitAll
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> request) {
        try {
            // Validate required parameters
            if (request.get("orderId") == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "orderId is required"));
            }
            if (request.get("amount") == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "amount is required"));
            }
            if (request.get("userId") == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "userId is required"));
            }

            Long orderId = Long.valueOf(request.get("orderId").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String userId = request.get("userId").toString();
            String paymentMethod = (String) request.getOrDefault("paymentMethod", "VNPAY");

            if (paymentMethod == null) {
                paymentMethod = "VNPAY";
            }

            PaymentMethod method = PaymentMethod.valueOf(paymentMethod.toUpperCase());

            // Get real orderNumber from order service
            String orderNumber = null;
            try {
                var orderDetails = orderServiceClient.getOrderById(orderId);
                if (orderDetails != null && orderDetails.get("orderNumber") != null) {
                    Object orderNumberObj = orderDetails.get("orderNumber");
                    if (orderNumberObj != null) {
                        orderNumber = orderNumberObj.toString();
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch order details for orderId {}, using orderId as orderNumber", orderId);
            }

            // Fallback to orderId if orderNumber not found
            if (orderNumber == null) {
                orderNumber = String.valueOf(orderId);
            }

            Payment payment = paymentService.createPayment(orderId, orderNumber, userId,
                    method, amount, "VND", "127.0.0.1", "OrderService/1.0");

            Map<String, Object> result = paymentService.processPayment(payment);

            // Merge payment result with basic success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment processed");
            response.put("paymentId", payment.getId());
            response.putAll(result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing payment: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Create VNPay payment (FE expects this endpoint)
     */
    @PermitAll
    @PostMapping("/vnpay/create")
    public ResponseEntity<Map<String, Object>> createVNPay(@RequestBody Map<String, Object> payload,
                                                           HttpServletRequest request) {
        try {
            Long orderId = Long.valueOf(payload.get("orderId").toString());
            String userId = payload.get("userId") != null ? payload.get("userId").toString() : "anonymous"; // Default user ID for anonymous
            BigDecimal amount = payload.get("amount") != null ? new BigDecimal(payload.get("amount").toString()) : new BigDecimal("0");

            // Fetch order details from order service to get the real orderNumber
            String orderNumber = null;
            try {
                RestTemplate rest = new RestTemplate();
                String url = "http://localhost:8084/api/orders/" + orderId;

                HttpHeaders headers = new HttpHeaders();
                headers.setBasicAuth("service", "service123");

                HttpEntity<String> entity = new HttpEntity<>(headers);
                ParameterizedTypeReference<Map<String, Object>> typeRef = 
                    new ParameterizedTypeReference<Map<String, Object>>() {};
                ResponseEntity<Map<String, Object>> resp = 
                    rest.exchange(url, HttpMethod.GET, entity, typeRef);

                Map<String, Object> responseBody = resp.getBody();
                if (responseBody != null && responseBody.get("orderNumber") != null) {
                    orderNumber = (String) responseBody.get("orderNumber");
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch order details for orderId " + orderId + ": " + e.getMessage());
            }

            // Use fetched orderNumber, fallback to orderId as string
            if (orderNumber == null) {
                orderNumber = String.valueOf(orderId);
            }

            Payment p = paymentService.createPayment(orderId, orderNumber, userId,
                    PaymentMethod.VNPAY, amount, "VND", getClientIpAddress(request), request.getHeader("User-Agent"));
            Map<String, Object> result = paymentService.processVNPayPayment(p);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Create MoMo payment (placeholder to satisfy FE contract)
     */
    @PermitAll
    @PostMapping("/momo/create")
    public ResponseEntity<Map<String, Object>> createMoMo(@RequestBody Map<String, Object> payload,
                                                          HttpServletRequest request) {
        try {
            Long orderId = Long.valueOf(payload.get("orderId").toString());
            String userId = payload.get("userId") != null ? payload.get("userId").toString() : "anonymous"; // Default user ID for anonymous
            BigDecimal amount = payload.get("amount") != null ? new BigDecimal(payload.get("amount").toString()) : new BigDecimal("0");

            // Fetch order details from order service to get the real orderNumber
            String orderNumber = null;
            try {
                RestTemplate rest = new RestTemplate();
                String url = "http://localhost:8084/api/orders/" + orderId;

                HttpHeaders headers = new HttpHeaders();
                headers.setBasicAuth("service", "service123");

                HttpEntity<String> entity = new HttpEntity<>(headers);
                ParameterizedTypeReference<Map<String, Object>> typeRef = 
                    new ParameterizedTypeReference<Map<String, Object>>() {};
                ResponseEntity<Map<String, Object>> resp = 
                    rest.exchange(url, HttpMethod.GET, entity, typeRef);

                Map<String, Object> responseBody = resp.getBody();
                if (responseBody != null && responseBody.get("orderNumber") != null) {
                    orderNumber = (String) responseBody.get("orderNumber");
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch order details for orderId " + orderId + ": " + e.getMessage());
            }

            // Use fetched orderNumber, fallback to orderId as string
            if (orderNumber == null) {
                orderNumber = String.valueOf(orderId);
            }

            Payment p = paymentService.createPayment(orderId, orderNumber, userId,
                    PaymentMethod.MOMO, amount, "VND", getClientIpAddress(request), request.getHeader("User-Agent"));
            Map<String, Object> result = paymentService.processMoMoPayment(p);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Stripe payment confirmation
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/stripe/confirm")
    public ResponseEntity<Map<String, Object>> confirmStripePayment(
            @RequestBody Map<String, String> request) {
        try {
            String paymentIntentId = request.get("paymentIntentId");
            Map<String, Object> result = paymentService.confirmStripePayment(paymentIntentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPayment(@PathVariable Long id) {
        try {
            Optional<Payment> paymentOpt = paymentService.findById(id);
            if (paymentOpt.isPresent()) {
                return ResponseEntity.ok(createPaymentResponse(paymentOpt.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get payments by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Map<String, Object>>> getPaymentsByOrder(@PathVariable Long orderId) {
        try {
            List<Payment> payments = paymentService.findByOrderId(orderId);
            List<Map<String, Object>> responses = payments.stream()
                .map(this::createPaymentResponse)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get payments by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getPaymentsByUser(@PathVariable String userId) {
        try {
            List<Payment> payments = paymentService.findByUserId(userId);
            List<Map<String, Object>> responses = payments.stream()
                .map(this::createPaymentResponse)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all payments with pagination
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("")
    public ResponseEntity<Page<Map<String, Object>>> getAllPayments(Pageable pageable) {
        try {
            Page<Payment> payments = paymentService.findAll(pageable);
            Page<Map<String, Object>> responses = payments.map(this::createPaymentResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get payments by status
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Map<String, Object>>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        try {
            List<Payment> payments = paymentService.findByStatus(status);
            List<Map<String, Object>> responses = payments.stream()
                .map(this::createPaymentResponse)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cancel payment (when user cancels during checkout/payment process)
     * This will cancel the order and release inventory
     */
    @PostMapping("/cancel")
    @PermitAll
    public ResponseEntity<Map<String, Object>> cancelPayment(@RequestBody Map<String, Object> request) {
        try {
            String orderNumber = (String) request.get("orderNumber");
            String reason = (String) request.getOrDefault("reason", "Payment cancelled by user");
            
            if (orderNumber == null || orderNumber.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "error", "orderNumber is required"
                ));
            }

            logger.info("Cancelling payment for order: {}, reason: {}", orderNumber, reason);
            
            // Cancel the order (which will also release inventory)
            Map<String, Object> cancelResult = orderServiceClient.cancelOrderByNumber(orderNumber, reason);
            
            if (cancelResult != null && Boolean.TRUE.equals(cancelResult.get("success"))) {
                logger.info("Successfully cancelled order: {}", orderNumber);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment and order cancelled successfully",
                    "orderNumber", orderNumber,
                    "deleted", cancelResult.getOrDefault("deleted", false)
                ));
            } else {
                logger.error("Failed to cancel order: {}, response: {}", orderNumber, cancelResult);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to cancel order",
                    "details", cancelResult
                ));
            }
        } catch (Exception e) {
            logger.error("Error cancelling payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false, 
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Refund payment
     */
	@PreAuthorize("hasAnyRole('ADMIN','SERVICE')")
	@PostMapping("/{id}/refund")
    public ResponseEntity<Map<String, Object>> refundPayment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> refundRequest) {
        try {
            BigDecimal amount = new BigDecimal(refundRequest.get("amount").toString());
            String reason = (String) refundRequest.getOrDefault("reason", "Refund requested");
            
            Map<String, Object> result = paymentService.refundPayment(id, amount, reason);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
	
	/**
	 * Internal refund by order number (used by Order Service)
	 */
	@PreAuthorize("hasAnyRole('ADMIN','SERVICE')")
	@PostMapping("/internal/refund-by-order/{orderNumber}")
	public ResponseEntity<Map<String, Object>> refundPaymentByOrderNumber(
			@PathVariable String orderNumber,
			@RequestBody(required = false) Map<String, Object> refundRequest) {
		try {
			String reason = refundRequest != null
				? (String) refundRequest.getOrDefault("reason", "Order cancelled")
				: "Order cancelled";
			
			Map<String, Object> result = paymentService.refundPaymentByOrderNumber(orderNumber, reason);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
		}
	}

    /**
     * Validate card
     */
    @PostMapping("/validate-card")
    public ResponseEntity<Map<String, Object>> validateCard(
            @RequestBody Map<String, String> cardInfo) {
        try {
            String cardNumber = cardInfo.get("cardNumber");
            String expMonth = cardInfo.get("expMonth");
            String expYear = cardInfo.get("expYear");
            String cvc = cardInfo.get("cvc");
            
            boolean isValidCard = stripeService.isValidCardNumber(cardNumber);
            boolean isValidCvc = stripeService.isValidCvc(cvc);
            boolean isValidExpiry = stripeService.isValidExpiryDate(expMonth, expYear);
            
            Map<String, Object> result = Map.of(
                "isValidCard", isValidCard,
                "isValidCvc", isValidCvc,
                "isValidExpiry", isValidExpiry,
                "isValid", isValidCard && isValidCvc && isValidExpiry
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get payment statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPaymentStatistics() {
        try {
            Map<String, Object> stats = paymentService.getPaymentStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get high risk payments
     */
    @GetMapping("/high-risk")
    public ResponseEntity<List<Map<String, Object>>> getHighRiskPayments() {
        try {
            List<Payment> payments = paymentService.getHighRiskPayments();
            List<Map<String, Object>> responses = payments.stream()
                .map(this::createPaymentResponse)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get failed payments
     */
    @GetMapping("/failed")
    public ResponseEntity<List<Map<String, Object>>> getFailedPayments() {
        try {
            List<Payment> payments = paymentService.getFailedPayments();
            List<Map<String, Object>> responses = payments.stream()
                .map(this::createPaymentResponse)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Analyze fraud risk
     */
    @PostMapping("/fraud-analysis")
    public ResponseEntity<Map<String, Object>> analyzeFraudRisk(@RequestBody Payment payment) {
        try {
            Map<String, Object> analysis = paymentService.analyzeFraudRisk(payment);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get supported banks
     */
    @GetMapping("/banks")
    public ResponseEntity<Map<String, Object>> getSupportedBanks() {
        try {
            Map<String, Object> result = bankTransferService.getSupportedBanks();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check bank transfer status
     */
    @GetMapping("/bank-transfer/status/{orderNumber}")
    public ResponseEntity<Map<String, Object>> checkBankTransferStatus(
            @PathVariable String orderNumber) {
        try {
            Map<String, Object> result = bankTransferService.checkTransferStatus(orderNumber);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Confirm bank transfer (admin)
     */
    @PostMapping("/bank-transfer/confirm")
    public ResponseEntity<Map<String, Object>> confirmBankTransfer(
            @RequestBody Map<String, Object> request) {
        try {
            String orderNumber = (String) request.get("orderNumber");
            String transactionId = (String) request.get("transactionId");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            
            Map<String, Object> result = bankTransferService.confirmTransfer(orderNumber, transactionId, amount);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create payment response DTO
     */
    private Map<String, Object> createPaymentResponse(Payment payment) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", payment.getId());
        response.put("orderId", payment.getOrderId());
        response.put("orderNumber", payment.getOrderNumber());
        response.put("userId", payment.getUserId());
        response.put("paymentMethod", payment.getPaymentMethod().name());
        response.put("status", payment.getStatus().name());
        response.put("amount", payment.getAmount());
        response.put("currency", payment.getCurrency());
        response.put("transactionId", payment.getTransactionId());
        response.put("paymentReference", payment.getPaymentReference());
        response.put("failureReason", payment.getFailureReason());
        response.put("riskScore", payment.getRiskScore());
        response.put("riskLevel", payment.getRiskLevel() != null ? payment.getRiskLevel().name() : null);
        response.put("paymentDate", payment.getPaymentDate());
        response.put("refundAmount", payment.getRefundAmount());
        response.put("refundReason", payment.getRefundReason());
        response.put("refundDate", payment.getRefundDate());
        response.put("refundReference", payment.getRefundReference());
        response.put("createdAt", payment.getCreatedAt());
        response.put("updatedAt", payment.getUpdatedAt());
        
        return response;
    }

    /**
     * Get client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}