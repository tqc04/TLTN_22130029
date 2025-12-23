package com.example.shared.util;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Security utility class for validation and security checks
 */
public class SecurityUtils {
    
    // UUID pattern: 8-4-4-4-12 hexadecimal characters
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    
    /**
     * Validate if a string is a valid UUID format
     * 
     * @param uuid the string to validate
     * @return true if valid UUID format, false otherwise
     */
    public static boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }
        
        // Quick check: UUID must be exactly 36 characters (with hyphens)
        if (uuid.length() != 36) {
            return false;
        }
        
        // Pattern match for UUID format
        if (!UUID_PATTERN.matcher(uuid).matches()) {
            return false;
        }
        
        // Try to parse as UUID to ensure it's valid
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Validate UUID and throw exception if invalid
     * 
     * @param uuid the string to validate
     * @param fieldName the name of the field (for error message)
     * @throws IllegalArgumentException if UUID is invalid
     */
    public static void validateUUID(String uuid, String fieldName) {
        if (!isValidUUID(uuid)) {
            throw new IllegalArgumentException(
                String.format("Invalid %s format. Expected UUID format.", fieldName)
            );
        }
    }
    
    /**
     * Check if a string is a numeric identifier (legacy IDs)
     */
    public static boolean isNumericId(String value) {
        return value != null && !value.isEmpty() && NUMERIC_PATTERN.matcher(value).matches();
    }
    
    /**
     * Check if a string is an alphanumeric identifier (letters, digits, underscore, hyphen)
     */
    public static boolean isAlphanumericId(String value) {
        return value != null && !value.isEmpty() && ALPHANUMERIC_PATTERN.matcher(value).matches();
    }
    
    /**
     * Determine if a user identifier is acceptable (UUID, numeric, or alphanumeric)
     */
    public static boolean isSupportedUserIdentifier(String value) {
        return isValidUUID(value) || isNumericId(value) || isAlphanumericId(value);
    }
    
    /**
     * Sanitize string input to prevent injection attacks
     * Removes potentially dangerous characters
     * 
     * @param input the input string
     * @return sanitized string
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        // Remove null bytes and control characters
        return input.replaceAll("[\\x00-\\x1F\\x7F]", "");
    }
}

