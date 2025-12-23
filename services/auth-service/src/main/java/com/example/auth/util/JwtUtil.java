package com.example.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.Date;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.ttlMillis}")
    private long jwtExpiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        try {
            // Try to decode as Base64 first
            byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
            secretKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            // If Base64 decoding fails, use the string directly as bytes
            byte[] keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            secretKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    public String generateToken(String username, String userId) {
        String token = Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
        return token;
    }

    public String generateToken(String username) {
        return generateToken(username, null);
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

    public Claims parseToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
