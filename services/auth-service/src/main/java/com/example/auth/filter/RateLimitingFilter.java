package com.example.auth.filter;

import com.example.auth.service.RateLimitingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to apply rate limiting to login endpoint
 * Checks rate limit before processing login requests
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Autowired
    private RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Only apply rate limiting to login endpoint
        if (isLoginEndpoint(request)) {
            String clientIp = getClientIpAddress(request);
            
            if (!rateLimitingService.isAllowed(clientIp)) {
                long remainingTokens = rateLimitingService.getAvailableTokens(clientIp);
                
                logger.warn("Rate limit exceeded for IP: {} on login endpoint. Remaining tokens: {}", 
                    clientIp, remainingTokens);
                
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                
                String errorMessage = String.format(
                    "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many login attempts. Please try again later.\",\"retryAfter\":%d}",
                    rateLimitingService.getAvailableTokens(clientIp) > 0 ? 0 : 60
                );
                
                response.getWriter().write(errorMessage);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Check if request is to login endpoint
     */
    private boolean isLoginEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method) && 
               (path.equals("/api/auth/login") || path.endsWith("/api/auth/login"));
    }

    /**
     * Extract client IP address from request
     * Handles proxies and load balancers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // Handle multiple IPs (X-Forwarded-For can contain multiple IPs)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        // Fallback to localhost if IP is still unknown
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = "127.0.0.1";
        }
        
        return ip;
    }
}

