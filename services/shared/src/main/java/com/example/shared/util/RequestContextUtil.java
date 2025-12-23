package com.example.shared.util;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Component
public class RequestContextUtil {
    public Long getCurrentUserId() {
        return null;
    }

    public String getCurrentSessionId() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                return request.getSession().getId();
            }
        } catch (Exception ignored) {}
        return UUID.randomUUID().toString();
    }

    public String getCurrentIpAddress() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    public String getCurrentUserAgent() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                return request.getHeader("User-Agent");
            }
        } catch (Exception ignored) {}
        return "Unknown";
    }

    public String getCurrentDeviceType() {
        String userAgent = getCurrentUserAgent();
        if (userAgent != null) {
            String ua = userAgent.toLowerCase();
            if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "mobile";
            if (ua.contains("tablet") || ua.contains("ipad")) return "tablet";
        }
        return "desktop";
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) return attributes.getRequest();
        } catch (Exception ignored) {}
        return null;
    }

    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }
}


