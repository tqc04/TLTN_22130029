package com.example.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    @Value("${services.user.base-url:http://localhost:8082}")
    private String userServiceBaseUrl;

    /**
     * Send simple email
     */
    public Map<String, Object> sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            
            return Map.of("success", true, "message", "Email sent successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send HTML email
     */
    public Map<String, Object> sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            return Map.of("success", true, "message", "HTML email sent successfully");
        } catch (MessagingException e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send order confirmation email
     */
    public Map<String, Object> sendOrderConfirmationEmail(Long userId, Object order) {
        try {
            // Get user email
            String userEmail = getUserEmail(userId);
            if (userEmail == null) {
                return Map.of("success", false, "error", "User email not found");
            }
            
            String subject = "Order Confirmation - " + getOrderNumber(order);
            String htmlContent = generateOrderConfirmationHtml(order);
            
            return sendHtmlEmail(userEmail, subject, htmlContent);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send payment confirmation email
     */
    public Map<String, Object> sendPaymentConfirmationEmail(Long userId, Object payment) {
        try {
            String userEmail = getUserEmail(userId);
            if (userEmail == null) {
                return Map.of("success", false, "error", "User email not found");
            }
            
            String subject = "Payment Confirmation";
            String htmlContent = generatePaymentConfirmationHtml(payment);
            
            return sendHtmlEmail(userEmail, subject, htmlContent);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send password reset email
     */
    public Map<String, Object> sendPasswordResetEmail(String email, String resetToken, String baseUrl) {
        try {
            String subject = "Password Reset Request";
            String resetUrl = baseUrl + "/reset-password?token=" + resetToken;
            String htmlContent = generatePasswordResetHtml(resetUrl);
            
            return sendHtmlEmail(email, subject, htmlContent);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send email verification email
     */
    public Map<String, Object> sendEmailVerificationEmail(String email, String verificationToken, String baseUrl) {
        try {
            String subject = "Verify Your Email Address";
            String verificationUrl = baseUrl + "/verify-email?token=" + verificationToken;
            String htmlContent = generateEmailVerificationHtml(verificationUrl);
            
            return sendHtmlEmail(email, subject, htmlContent);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send promotional email
     */
    public Map<String, Object> sendPromotionalEmail(String email, String subject, String htmlContent) {
        try {
            return sendHtmlEmail(email, subject, htmlContent);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send welcome email
     */
    public Map<String, Object> sendWelcomeEmail(String email, String firstName, String lastName) {
        try {
            String subject = "Welcome to Our Platform!";
            String name = (firstName != null && !firstName.isEmpty() ? firstName : "") +
                         (lastName != null && !lastName.isEmpty() ? " " + lastName : "");
            if (name.trim().isEmpty()) {
                name = "there";
            }

            String htmlContent = generateWelcomeEmailHtml(name.trim());

            return sendHtmlEmail(email, subject, htmlContent);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Get user email from user service
     */
    private String getUserEmail(Long userId) {
        try {
            String url = userServiceBaseUrl + "/api/users/" + userId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) restTemplate.getForObject(url, Map.class);
            if (response == null) {
                return null;
            }
            return (String) response.get("email");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get order number from order object
     */
    private String getOrderNumber(Object order) {
        try {
            return (String) order.getClass().getMethod("getOrderNumber").invoke(order);
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Generate order confirmation HTML
     */
    private String generateOrderConfirmationHtml(Object order) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Order Confirmation</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f8f9fa; padding: 20px; text-align: center; }
                    .content { padding: 20px; }
                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Order Confirmation</h1>
                    </div>
                    <div class="content">
                        <p>Thank you for your order!</p>
                        <p>Your order has been confirmed and is being processed.</p>
                        <p>Order Number: <strong>" + getOrderNumber(order) + "</strong></p>
                        <p>We will send you another email when your order ships.</p>
                    </div>
                    <div class="footer">
                        <p>Thank you for shopping with us!</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    /**
     * Generate payment confirmation HTML
     */
    private String generatePaymentConfirmationHtml(Object payment) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Payment Confirmation</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f8f9fa; padding: 20px; text-align: center; }
                    .content { padding: 20px; }
                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Payment Confirmation</h1>
                    </div>
                    <div class="content">
                        <p>Your payment has been processed successfully!</p>
                        <p>Thank you for your payment.</p>
                    </div>
                    <div class="footer">
                        <p>Thank you for shopping with us!</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    /**
     * Generate password reset HTML
     */
    private String generatePasswordResetHtml(String resetUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset=\"UTF-8\">
                <title>Password Reset</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f8f9fa; padding: 20px; text-align: center; }
                    .content { padding: 20px; }
                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class=\"container\"> 
                    <div class=\"header\">
                        <h1>Password Reset Request</h1>
                    </div>
                    <div class=\"content\">
                        <p>You requested to reset your password.</p>
                        <p>Click the button below to reset your password:</p>
                        <p>
                          <a href=\"%s\" target=\"_blank\" rel=\"noopener noreferrer\"
                             style=\"display:inline-block;padding:12px 20px;background-color:#1a73e8;color:#ffffff;text-decoration:none;border-radius:6px;font-weight:600\">Reset Password</a>
                        </p>
                        <p style=\"font-size:12px;color:#555;\">If the button doesn't work, copy and paste this link into your browser:</p>
                        <p style=\"word-break:break-all;font-size:12px;\"><a href=\"%s\" target=\"_blank\" rel=\"noopener noreferrer\">%s</a></p>
                        <p>If you didn't request this, please ignore this email.</p>
                    </div>
                    <div class=\"footer\">
                        <p>This link will expire in 24 hours.</p>
                    </div>
                </div>
            </body>
            </html>
            """, resetUrl, resetUrl, resetUrl);
    }

    /**
     * Generate email verification HTML
     */
    private String generateEmailVerificationHtml(String verificationUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset=\"UTF-8\">
                <title>Email Verification</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f8f9fa; padding: 20px; text-align: center; }
                    .content { padding: 20px; }
                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class=\"container\">
                    <div class=\"header\">
                        <h1>Verify Your Email Address</h1>
                    </div>
                    <div class=\"content\">
                        <p>Welcome! Please verify your email address to complete your registration.</p>
                        <p>Click the button below to verify your email:</p>
                        <p>
                            <a href=\"%s\" target=\"_blank\" rel=\"noopener noreferrer\"
                               style=\"display:inline-block;padding:12px 20px;background-color:#1a73e8;color:#ffffff;text-decoration:none;border-radius:6px;font-weight:600\">
                                Verify Email
                            </a>
                        </p>
                        <p style=\"font-size:12px;color:#555;\">If the button doesn't work, copy and paste this link into your browser:</p>
                        <p style=\"word-break:break-all;font-size:12px;\"><a href=\"%s\" target=\"_blank\" rel=\"noopener noreferrer\">%s</a></p>
                        <p>If you didn't create an account, please ignore this email.</p>
                    </div>
                    <div class=\"footer\">
                        <p>This link will expire in 24 hours.</p>
                    </div>
                </div>
            </body>
            </html>
            """, verificationUrl, verificationUrl, verificationUrl);
    }

    /**
     * Generate welcome email HTML
     */
    private String generateWelcomeEmailHtml(String name) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Welcome to Our Platform</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f8f9fa; padding: 20px; text-align: center; }
                    .content { padding: 20px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; }
                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to Our Platform!</h1>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>Welcome to our platform! We're excited to have you join our community.</p>
                        <p>Your account has been successfully verified and you're all set to start exploring our features.</p>
                        <p>Here's what you can do next:</p>
                        <ul>
                            <li>Complete your profile</li>
                            <li>Explore our services</li>
                            <li>Connect with other users</li>
                            <li>Start using our amazing features</li>
                        </ul>
                        <p>If you have any questions, feel free to reach out to our support team.</p>
                        <p>Happy exploring!</p>
                        <p>Best regards,<br>The Team</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """, name);
    }
}
