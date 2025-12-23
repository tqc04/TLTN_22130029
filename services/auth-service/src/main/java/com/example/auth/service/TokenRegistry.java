package com.example.auth.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TokenRegistry {
    private static final Map<String, Set<String>> subjectToTokens = new ConcurrentHashMap<>();
    private static TokenBlacklist blacklist;

    public TokenRegistry(TokenBlacklist bl) {
        blacklist = bl;
    }

    public static void register(String subject, String token, long expMs) {
        subjectToTokens.computeIfAbsent(subject, k -> java.util.Collections.newSetFromMap(new ConcurrentHashMap<>())).add(token);
    }

    public static void revokeAllForSubject(String subject) {
        Set<String> tokens = subjectToTokens.get(subject);
        if (tokens == null || blacklist == null) return;
        long nowPlus = System.currentTimeMillis() + 3600000;
        for (String t : tokens) blacklist.revoke(t, nowPlus);
        subjectToTokens.remove(subject);
    }
}


