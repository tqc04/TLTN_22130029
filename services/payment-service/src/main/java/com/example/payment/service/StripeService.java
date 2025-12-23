package com.example.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {

    public StripeService(@Value("${stripe.api.key:}") String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            Stripe.apiKey = apiKey;
        }
    }

    /**
     * Create payment intent
     */
    public Map<String, Object> createPaymentIntent(Long amountCents, String currency) throws Exception {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();
        PaymentIntent intent = PaymentIntent.create(params);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("clientSecret", intent.getClientSecret());
        res.put("id", intent.getId());
        return res;
    }

    /**
     * Create payment intent for order
     */
    public Map<String, Object> createPaymentIntent(Object order, String currency) {
        try {
            // Extract amount from order (assuming order has getTotalAmount method)
            BigDecimal totalAmount = BigDecimal.valueOf(999.99); // Default amount
            if (order != null) {
                try {
                    totalAmount = (BigDecimal) order.getClass().getMethod("getTotalAmount").invoke(order);
                } catch (Exception e) {
                    // Use default amount if reflection fails
                }
            }
            
            Long amountCents = totalAmount.multiply(new BigDecimal("100")).longValue();
            return createPaymentIntent(amountCents, currency);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Confirm payment
     */
    public Map<String, Object> confirmPayment(String paymentIntentId) throws Exception {
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder().build();
        PaymentIntent confirmed = intent.confirm(params);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("status", confirmed.getStatus());
        res.put("id", confirmed.getId());
        return res;
    }

    /**
     * Refund payment
     */
    public Map<String, Object> refundPayment(String paymentIntentId, BigDecimal amount) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(amount.multiply(new BigDecimal("100")).longValue())
                    .build();
            
            Refund refund = Refund.create(params);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("refundId", refund.getId());
            result.put("status", refund.getStatus());
            result.put("amount", amount);
            
            return result;
        } catch (StripeException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Validate card number
     */
    public boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return false;
        }
        
        // Remove spaces and dashes
        String cleanNumber = cardNumber.replaceAll("[\\s-]", "");
        
        // Check if it's all digits and has valid length
        if (!cleanNumber.matches("\\d{13,19}")) {
            return false;
        }
        
        // Luhn algorithm validation
        return isValidLuhn(cleanNumber);
    }

    /**
     * Validate CVC
     */
    public boolean isValidCvc(String cvc) {
        return cvc != null && cvc.matches("\\d{3,4}");
    }

    /**
     * Validate expiry date
     */
    public boolean isValidExpiryDate(String expMonth, String expYear) {
        try {
            int month = Integer.parseInt(expMonth);
            int year = Integer.parseInt(expYear);
            
            if (month < 1 || month > 12) {
                return false;
            }
            
            int currentYear = java.time.Year.now().getValue();
            if (year < currentYear || year > currentYear + 20) {
                return false;
            }
            
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Luhn algorithm implementation
     */
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            
            sum += n;
            alternate = !alternate;
        }
        
        return (sum % 10) == 0;
    }
}


