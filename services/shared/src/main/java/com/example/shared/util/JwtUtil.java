package com.example.shared.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    @Value("${security.jwt.secret:}")
    private String jwtSecret;

    @Value("${JWT_SECRET:}")
    private String envJwtSecret;

    @Value("${security.jwt.ttlMillis:86400000}")
    private long jwtExpiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        String effective = (jwtSecret != null && !jwtSecret.isBlank()) ? jwtSecret : envJwtSecret;
        if (effective == null || effective.isBlank()) {
            effective = "CHANGE_THIS_TO_A_VERY_LONG_RANDOM_SECRET_KEY_AT_LEAST_256_BITS";
        }
        
        try {
            // Try to decode as Base64 first
            byte[] keyBytes = Base64.getDecoder().decode(effective);
            secretKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            // If Base64 decoding fails, use the string directly as bytes
            byte[] keyBytes = effective.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            secretKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    public String generateToken(String username, String userId) {
        String token = Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
        return token;
    }

    public String generateToken(String username) {
        return generateToken(username, null);
    }

    public String generateTokenWithClaims(String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            Object userIdObj = claims.get("userId");
            if (userIdObj != null) {
                return userIdObj.toString();
            }
            return null;
        } catch (Exception e) {
            logger.warn("Failed to extract userId from token: {}", e.getMessage());
            return null;
        }
    }

    public String getSubjectFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            logger.warn("Failed to extract subject from token: {}", e.getMessage());
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            Object role = claims.get("role");
            return role != null ? role.toString() : "USER";
        } catch (Exception e) {
            logger.warn("Failed to extract role from token: {}", e.getMessage());
            return "USER";
        }
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }
}
