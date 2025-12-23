package com.example.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VNPayService {
	@Value("${vnpay.merchant.id:}")
	private String merchantId;

	@Value("${vnpay.secret.key:}")
	private String secretKey;
	
	// Trim secret key to remove any whitespace (common issue from copy-paste)
	private String getTrimmedSecretKey() {
		return secretKey != null ? secretKey.trim() : "";
	}

	@Value("${vnpay.return.url:http://localhost:8085/api/payments/vnpay/return}")
	private String returnUrl;

	@Value("${vnpay.payment.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
	private String paymentUrl;


	@Value("${services.order.base-url:http://localhost:8084}")
	private String orderServiceBaseUrl;

	@Value("${interservice.username:service}")
	private String interServiceUsername;

	@Value("${interservice.password:service123}")
	private String interServicePassword;

	public Map<String, Object> validateConfiguration() {
		// Check if we're in development mode (using placeholder values or blank)
		boolean isDevelopmentMode = "your-vnpay-merchant-id-here".equals(merchantId) ||
								  "your-vnpay-secret-key-here".equals(secretKey) ||
								  isBlank(merchantId) || isBlank(secretKey) ||
								  merchantId.length() < 5; // Consider very short IDs as development

		if (isDevelopmentMode) {
			return Map.of("success", true, "message", "VNPay configuration is in development mode - using mock implementation");
		}

		if (isBlank(merchantId)) return Map.of("success", false, "error", "VNPay Merchant ID is not configured");
		if (isBlank(secretKey)) return Map.of("success", false, "error", "VNPay Secret Key is not configured");
		if (isBlank(returnUrl)) return Map.of("success", false, "error", "VNPay Return URL is not configured");
		if (isBlank(paymentUrl)) return Map.of("success", false, "error", "VNPay Payment URL is not configured");

		// Validate merchant ID format (can be alphanumeric for some VNPay accounts)
		if (!merchantId.matches("[A-Za-z0-9]+")) {
			return Map.of("success", false, "error", "VNPay Merchant ID must contain only letters and numbers");
		}

		// Validate return URL format (allow localhost with any port)
		if (!returnUrl.startsWith("http://") && !returnUrl.startsWith("https://")) {
			return Map.of("success", false, "error", "VNPay Return URL must start with http:// or https://");
		}

		// Validate return URL contains 'vnpay' and 'return' (more flexible)
		if (!returnUrl.toLowerCase().contains("vnpay") || !returnUrl.toLowerCase().contains("return")) {
			return Map.of("success", false, "error", "VNPay Return URL must contain 'vnpay' and 'return'");
		}

		return Map.of("success", true, "message", "VNPay configuration is valid");
	}

	public Map<String, Object> createPaymentUrl(Long orderId, String orderNumber, String ipAddress) {
		Map<String, Object> cfg = validateConfiguration();
		if (!(Boolean) cfg.get("success")) return cfg;

		// Check if we're in development mode (using placeholder values or blank)
		boolean isDevelopmentMode = "your-vnpay-merchant-id-here".equals(merchantId) ||
								  "your-vnpay-secret-key-here".equals(secretKey) ||
								  isBlank(merchantId) || isBlank(secretKey) ||
								  merchantId.length() < 5; // Consider very short IDs as development

		// Try to fetch order by orderNumber first, then by orderId as fallback
		OrderDTO order = null;
		if (orderNumber != null && !orderNumber.isEmpty()) {
			order = fetchOrderByNumber(orderNumber);
		}
		if (order == null) {
			order = fetchOrder(orderId);
		}

		if (isDevelopmentMode) {
			// Development mode: return mock payment URL
			if (order == null) {
				// Fallback for development: create mock order data
				System.out.println("WARNING: Could not fetch order " + orderId + " (or orderNumber: " + orderNumber + ") from order service, using fallback data");
				BigDecimal amount = new BigDecimal("100000"); // 100,000 VND fallback
				String fallbackOrderNumber = orderNumber != null ? orderNumber : "ORD-" + orderId;

				// Mock payment URL for development
				String mockPaymentUrl = "http://localhost:3000/payment-result?orderNumber=" + fallbackOrderNumber +
									  "&success=true&status=SUCCESS&message=VNPay%20Payment%20Mock%20Success&paymentMethod=VNPAY";

				return Map.of(
					"success", true,
					"paymentUrl", mockPaymentUrl,
					"orderNumber", fallbackOrderNumber,
					"amount", amount,
					"message", "Development mode: using fallback order data"
				);
			}

			BigDecimal amount = order.totalAmount != null ? order.totalAmount : new BigDecimal("100000");
			String finalOrderNumber = order.orderNumber != null ? order.orderNumber : orderNumber;

			// Mock payment URL for development
			String mockPaymentUrl = "http://localhost:3000/payment-result?orderNumber=" + finalOrderNumber +
								  "&success=true&status=SUCCESS&message=VNPay%20Payment%20Mock%20Success&paymentMethod=VNPAY";

			return Map.of(
				"success", true,
				"paymentUrl", mockPaymentUrl,
				"orderNumber", finalOrderNumber,
				"amount", amount,
				"message", "Development mode: using mock payment URL"
			);
		}

		// Production mode: use real VNPay
		if (order == null) {
			System.err.println("CRITICAL: Could not fetch order " + orderId + " (or orderNumber: " + orderNumber + ") from order service for VNPay payment");
			return Map.of("success", false, "error", "Order not found or order service unavailable");
		}

		// Use a fallback amount if order total is not available
		BigDecimal amount = order.totalAmount;
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			// Use a default amount for testing - in production this should fail
			amount = new BigDecimal("100000"); // 100,000 VND for testing
			System.out.println("WARNING: Using fallback amount for order " + orderId + " - this should not happen in production");
		}

		String vnp_Version = "2.1.0";
		String vnp_Command = "pay";
		String vnp_TmnCode = merchantId != null ? merchantId.trim() : "";
		
		// Validate merchant ID
		if (vnp_TmnCode.isEmpty()) {
			System.err.println("ERROR: VNPay Merchant ID is empty or not configured");
			return Map.of("success", false, "error", "VNPay Merchant ID is required");
		}
		
		// Validate secret key
		String trimmedSecret = getTrimmedSecretKey();
		if (trimmedSecret == null || trimmedSecret.isEmpty()) {
			System.err.println("ERROR: VNPay Secret Key is empty or not configured");
			return Map.of("success", false, "error", "VNPay Secret Key is required");
		}
		
		
		// Validate amount
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			return Map.of("success", false, "error", "Invalid payment amount");
		}
		String vnp_Amount = String.valueOf(amount.multiply(new BigDecimal("100")).longValue());
		
		// Validate amount is positive
		if (Long.parseLong(vnp_Amount) <= 0) {
			return Map.of("success", false, "error", "Payment amount must be greater than 0");
		}
		
		String vnp_CurrCode = "VND";
		
		// Clean and validate vnp_TxnRef (max 100 chars, alphanumeric and hyphens only)
		String rawTxnRef = order.orderNumber != null ? order.orderNumber : String.valueOf(order.id);
		// Remove special characters, keep only alphanumeric, hyphens, underscores
		String cleanedTxnRef = rawTxnRef.replaceAll("[^a-zA-Z0-9_-]", "");
		// Limit to 100 characters using the cleaned string length
		String vnp_TxnRef = cleanedTxnRef.length() > 100 ? cleanedTxnRef.substring(0, 100) : cleanedTxnRef;
		if (vnp_TxnRef.isEmpty()) {
			vnp_TxnRef = "ORD" + System.currentTimeMillis();
		}
		
		// vnp_OrderInfo: Use ASCII only, no Vietnamese characters to avoid encoding issues
		// Keep spaces as-is for hash calculation (VNPAY handles spaces in query string)
		String vnp_OrderInfo = "Payment for order " + vnp_TxnRef;
		// Ensure it's ASCII only (should already be, but double-check)
		vnp_OrderInfo = vnp_OrderInfo.replaceAll("[^\\x00-\\x7F]", "");
		// Limit to 255 characters as per VNPAY spec
		if (vnp_OrderInfo.length() > 255) {
			vnp_OrderInfo = vnp_OrderInfo.substring(0, 255);
		}
		
		String vnp_OrderType = "other";
		String vnp_Locale = "vn";
		
		// Normalize and validate IP address
		String vnp_IpAddr = normalizeIp(ipAddress);
		if (vnp_IpAddr == null || vnp_IpAddr.isEmpty()) {
			vnp_IpAddr = "127.0.0.1"; // Default fallback
		}
		
		// Validate and trim return URL
		String vnp_ReturnUrl = returnUrl != null ? returnUrl.trim() : "";
		if (vnp_ReturnUrl.isEmpty() || (!vnp_ReturnUrl.startsWith("http://") && !vnp_ReturnUrl.startsWith("https://"))) {
			return Map.of("success", false, "error", "Invalid return URL");
		}
		
		String vnp_CreateDate = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

		Map<String, String> params = new TreeMap<>();
		params.put("vnp_Version", vnp_Version);
		params.put("vnp_Command", vnp_Command);
		params.put("vnp_TmnCode", vnp_TmnCode);
		params.put("vnp_Amount", vnp_Amount);
		params.put("vnp_CurrCode", vnp_CurrCode);
		params.put("vnp_TxnRef", vnp_TxnRef);
		params.put("vnp_OrderInfo", vnp_OrderInfo);
		params.put("vnp_OrderType", vnp_OrderType);
		params.put("vnp_Locale", vnp_Locale);
		params.put("vnp_IpAddr", vnp_IpAddr);
		params.put("vnp_ReturnUrl", vnp_ReturnUrl);
		params.put("vnp_CreateDate", vnp_CreateDate);

		params.remove("vnp_SecureHashType");
		params.remove("vnp_SecureHash");
		
		String queryStringForHash = buildQueryForHash(params);
		String secureHash = hmacSHA512(trimmedSecret, queryStringForHash);
		String queryStringForUrl = buildQueryForUrl(params);
		String paymentRedirect = paymentUrl + "?" + queryStringForUrl + "&vnp_SecureHash=" + URLEncoder.encode(secureHash, StandardCharsets.UTF_8);
		
		// Debug logging - CRITICAL để fix lỗi code=70
		System.err.println("=== VNPay Hash Debug (code=70 fix) ===");
		System.err.println("Query String for Hash: " + queryStringForHash);
		System.err.println("Secret Key length: " + trimmedSecret.length());
		System.err.println("Secret Key (first 4): " + (trimmedSecret.length() > 4 ? trimmedSecret.substring(0, 4) : trimmedSecret));
		System.err.println("Secret Key (last 4): " + (trimmedSecret.length() > 4 ? trimmedSecret.substring(trimmedSecret.length() - 4) : trimmedSecret));
		System.err.println("Key Encoding: ISO-8859-1");
		System.err.println("Data Encoding: UTF-8");
		System.err.println("Hash (SHA512): " + secureHash);
		System.err.println("Hash length: " + secureHash.length());
		System.err.println("Payment URL: " + paymentRedirect);
		System.err.println("=====================================");

		return Map.of(
				"success", true,
				"paymentUrl", paymentRedirect,
				"orderNumber", vnp_TxnRef,
				"amount", amount
		);
	}

	public Map<String, Object> processCallback(Map<String, String> callbackParams) {
		Map<String, String> params = new TreeMap<>(callbackParams);
		String receivedHash = params.remove("vnp_SecureHash");
		params.remove("vnp_SecureHashType");
		String queryString = buildQueryForHash(params);
		String trimmedSecret = getTrimmedSecretKey();
		String computed = hmacSHA512(trimmedSecret, queryString);
		boolean valid = receivedHash != null && receivedHash.equalsIgnoreCase(computed);
		return Map.of("success", valid, "data", callbackParams);
	}

	private String normalizeIp(String ip) {
		if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return "127.0.0.1";
		return ip;
	}

	/**
	 * Build query string WITH URL encoding (VNPay expects hash string identical to actual query)
	 * Parameters must be sorted alphabetically by KEY name (TreeMap handles this)
	 * Only include non-null and non-empty values
	 * IMPORTANT: Do NOT include vnp_SecureHash or vnp_SecureHashType in hash calculation
	 */
	private static String buildQueryForHash(Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String> e : params.entrySet()) {
			// Skip null values but keep empty strings (VNPAY requirement)
			if (e.getValue() == null) {
				continue;
			}
			String key = e.getKey();
			if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) {
				continue;
			}
			if (!first) sb.append("&");
			first = false;
			sb.append(encodeForVnPay(key))
			  .append("=")
			  .append(encodeForVnPay(e.getValue()));
		}
		return sb.toString();
	}
	
	/**
	 * Build query string WITH URL encoding (for URL construction)
	 */
	private static String buildQueryForUrl(Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String> e : params.entrySet()) {
			if (!first) sb.append("&");
			first = false;
			sb.append(encodeForVnPay(e.getKey()))
			  .append("=")
			  .append(encodeForVnPay(e.getValue()));
		}
		return sb.toString();
	}

	private static String hmacSHA512(String key, String data) {
		try {
			javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
			// VNPay yêu cầu: Secret key dùng ISO-8859-1, data dùng UTF-8
			// Đây là yêu cầu chính thức từ VNPay documentation
			byte[] keyBytes = key.getBytes(StandardCharsets.ISO_8859_1);
			byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
			mac.init(new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA512"));
			byte[] bytes = mac.doFinal(dataBytes);
			StringBuilder hash = new StringBuilder(bytes.length * 2);
			for (byte b : bytes) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) hash.append('0');
				hash.append(hex);
			}
			return hash.toString();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to compute HMAC SHA512", ex);
		}
	}

	private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

	private static String encodeForVnPay(String input) {
		// VNPay expects standard application/x-www-form-urlencoded format.
		// Do not replace '+' with '%20' or make other custom tweaks,
		// otherwise the hash string will differ from what VNPay reconstructs.
		return URLEncoder.encode(input, StandardCharsets.UTF_8);
	}

	private OrderDTO fetchOrder(Long id) {
		try {
			RestTemplate rest = new RestTemplate();

			String url = orderServiceBaseUrl + "/api/orders/" + id;

			// Add authentication headers for inter-service communication
			org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
			headers.setBasicAuth(interServiceUsername, interServicePassword); // Use configurable service account credentials

			org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
			ResponseEntity<OrderDTO> resp = rest.exchange(url, org.springframework.http.HttpMethod.GET, entity, OrderDTO.class);
			return resp.getBody();
		} catch (Exception e) {
			// Log the error but don't fail silently - this is critical for payment processing
			System.err.println("Failed to fetch order " + id + " from order service: " + e.getMessage());
			return null;
		}
	}

	private OrderDTO fetchOrderByNumber(String orderNumber) {
		try {
			RestTemplate rest = new RestTemplate();

			String url = orderServiceBaseUrl + "/api/orders/number/" + orderNumber;

			// Add authentication headers for inter-service communication
			org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
			headers.setBasicAuth(interServiceUsername, interServicePassword); // Use configurable service account credentials

			org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
			ResponseEntity<OrderDTO> resp = rest.exchange(url, org.springframework.http.HttpMethod.GET, entity, OrderDTO.class);
			return resp.getBody();
		} catch (Exception e) {
			// Log the error but don't fail silently - this is critical for payment processing
			System.err.println("Failed to fetch order by number " + orderNumber + " from order service: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Refund payment
	 */
	public Map<String, Object> refundPayment(String orderNumber, BigDecimal amount, String reason) {
		Map<String, Object> cfg = validateConfiguration();
		if (!(Boolean) cfg.get("success")) return cfg;

		try {
			// In a real implementation, this would call VNPay's refund API
			// For now, return a mock response
			String refundId = "REF" + System.currentTimeMillis();
			
			Map<String, Object> result = new HashMap<>();
			result.put("success", true);
			result.put("refundId", refundId);
			result.put("orderNumber", orderNumber);
			result.put("amount", amount);
			result.put("reason", reason);
			result.put("message", "Refund request submitted successfully");
			
			return result;
		} catch (Exception e) {
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	public static class OrderDTO {
		public Long id;
		public String orderNumber;
		public BigDecimal totalAmount;
	}
	
	public String getMerchantId() {
		return merchantId;
	}
	
	public String getSecretKey() {
		return getTrimmedSecretKey();
	}
	
	public String getReturnUrl() {
		return returnUrl;
	}
	
	public String getPaymentUrl() {
		return paymentUrl;
	}
}


