package com.example.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class JwtService {

    private final SecretKey secretKey;
    private final long ttlMillis;
    private final String keyId;

    public JwtService(String secret, long ttlMillis) {
        this(secret, ttlMillis, null);
    }

    public JwtService(String secret, long ttlMillis, String keyId) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.ttlMillis = ttlMillis;
        this.keyId = keyId;
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(ttlMillis);
        return Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String parseSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }
}


