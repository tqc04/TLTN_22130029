package com.example.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SmsService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${twilio.account.sid:}")
    private String accountSid;
    
    @Value("${twilio.auth.token:}")
    private String authToken;
    
    @Value("${twilio.phone.number:}")
    private String fromPhoneNumber;
    
    @Value("${services.user.base-url:http://localhost:8082}")
    private String userServiceBaseUrl;

    /**
     * Send SMS message
     */
    public Map<String, Object> sendSms(String to, String message) {
        try {
            if (accountSid.isEmpty() || authToken.isEmpty() || fromPhoneNumber.isEmpty()) {
                return Map.of("success", false, "error", "Twilio not configured");
            }
            
            // In a real implementation, this would use Twilio SDK
            // For now, return a mock response
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("messageId", "SMS" + System.currentTimeMillis());
            result.put("to", to);
            result.put("message", message);
            result.put("status", "sent");
            
            return result;
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send OTP SMS
     */
    public Map<String, Object> sendOtpSms(String phoneNumber, String otp) {
        try {
            String message = "Your OTP code is: " + otp + ". This code will expire in 5 minutes.";
            return sendSms(phoneNumber, message);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send order notification SMS
     */
    public Map<String, Object> sendOrderNotificationSms(Long userId, String orderNumber, String status) {
        try {
            String phoneNumber = getUserPhoneNumber(userId);
            if (phoneNumber == null) {
                return Map.of("success", false, "error", "User phone number not found");
            }
            
            String message = String.format("Order %s status: %s", orderNumber, status);
            return sendSms(phoneNumber, message);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send payment notification SMS
     */
    public Map<String, Object> sendPaymentNotificationSms(Long userId, String amount, String status) {
        try {
            String phoneNumber = getUserPhoneNumber(userId);
            if (phoneNumber == null) {
                return Map.of("success", false, "error", "User phone number not found");
            }
            
            String message = String.format("Payment %s: %s", status, amount);
            return sendSms(phoneNumber, message);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send promotional SMS
     */
    public Map<String, Object> sendPromotionalSms(String phoneNumber, String message) {
        try {
            return sendSms(phoneNumber, message);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send bulk SMS
     */
    public Map<String, Object> sendBulkSms(String[] phoneNumbers, String message) {
        try {
            Map<String, Object> results = new HashMap<>();
            int successCount = 0;
            int failureCount = 0;
            
            for (String phoneNumber : phoneNumbers) {
                Map<String, Object> result = sendSms(phoneNumber, message);
                if ((Boolean) result.get("success")) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }
            
            results.put("success", true);
            results.put("totalSent", phoneNumbers.length);
            results.put("successCount", successCount);
            results.put("failureCount", failureCount);
            
            return results;
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Get user phone number from user service
     */
    private String getUserPhoneNumber(Long userId) {
        try {
            String url = userServiceBaseUrl + "/api/users/" + userId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) restTemplate.getForObject(url, Map.class);
            if (response == null) {
                return null;
            }
            return (String) response.get("phoneNumber");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validate phone number format
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Remove all non-digit characters
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        
        // Check if it's a valid length (7-15 digits)
        return cleanNumber.length() >= 7 && cleanNumber.length() <= 15;
    }

    /**
     * Format phone number for international use
     */
    public String formatPhoneNumber(String phoneNumber, String countryCode) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }
        
        // Remove all non-digit characters
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        
        // Add country code if not present
        if (!cleanNumber.startsWith(countryCode)) {
            cleanNumber = countryCode + cleanNumber;
        }
        
        return "+" + cleanNumber;
    }
}
