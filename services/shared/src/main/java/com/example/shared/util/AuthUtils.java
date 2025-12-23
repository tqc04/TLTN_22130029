package com.example.shared.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Utility class for authentication and authorization
 */
public class AuthUtils {
    
    /**
     * Extract user ID from authentication token
     * Supports both JWT and other authentication types
     * 
     * @param authentication the authentication object
     * @return user ID as String, or null if not found
     */
    public static String extractUserIdFromAuth(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        
        // Try to extract from JWT token using reflection (to avoid dependency on OAuth2 Resource Server)
        try {
            // Check if it's a JWT authentication token
            String className = authentication.getClass().getName();
            if (className.contains("JwtAuthenticationToken") || className.contains("Jwt")) {
                // Use reflection to get JWT token
                Object jwt = null;
                try {
                    // Try getToken() method
                    java.lang.reflect.Method getTokenMethod = authentication.getClass().getMethod("getToken");
                    jwt = getTokenMethod.invoke(authentication);
                } catch (Exception e) {
                    // Try getPrincipal() as fallback
                    Object principal = authentication.getPrincipal();
                    if (principal != null && principal.getClass().getName().contains("Jwt")) {
                        jwt = principal;
                    }
                }
                
                if (jwt != null) {
                    // Extract claims from JWT
                    try {
                        java.lang.reflect.Method hasClaimMethod = jwt.getClass().getMethod("hasClaim", String.class);
                        java.lang.reflect.Method getClaimMethod = jwt.getClass().getMethod("getClaim", String.class);
                        java.lang.reflect.Method getSubjectMethod = jwt.getClass().getMethod("getSubject");
                        
                        // Try userId claim
                        if ((Boolean) hasClaimMethod.invoke(jwt, "userId")) {
                            Object userIdClaim = getClaimMethod.invoke(jwt, "userId");
                            if (userIdClaim != null) {
                                String userId = userIdClaim.toString();
                                if (SecurityUtils.isSupportedUserIdentifier(userId)) {
                                    return userId;
                                }
                            }
                        }
                        
                        // Try id claim
                        if ((Boolean) hasClaimMethod.invoke(jwt, "id")) {
                            Object idClaim = getClaimMethod.invoke(jwt, "id");
                            if (idClaim != null) {
                                String userId = idClaim.toString();
                                if (SecurityUtils.isSupportedUserIdentifier(userId)) {
                                    return userId;
                                }
                            }
                        }
                        
                        // Fallback to subject
                        String subject = (String) getSubjectMethod.invoke(jwt);
                        if (subject != null && !subject.isEmpty() && SecurityUtils.isSupportedUserIdentifier(subject)) {
                            return subject;
                        }
                    } catch (Exception e) {
                        // Reflection failed, continue to other methods
                    }
                }
            }
        } catch (Exception e) {
            // Reflection failed, continue to other methods
        }
        
        // Fallback: try to get from principal name or details
        String name = authentication.getName();
        if (name != null && !name.isEmpty() && SecurityUtils.isValidUUID(name)) {
            return name;
        }
        
        return null;
    }
    
    /**
     * Check if user has admin role
     * 
     * @param authentication the authentication object
     * @return true if user has ADMIN role, false otherwise
     */
    public static boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> 
                authority.equals("ROLE_ADMIN") || 
                authority.equals("ADMIN") ||
                authority.equals("ROLE_SUPER_ADMIN") ||
                authority.equals("SUPER_ADMIN")
            );
    }
    
    /**
     * Verify that the userId in path variable matches the authenticated user
     * Admin users can access any user's data
     * 
     * @param pathUserId the userId from path variable
     * @param authentication the authentication object
     * @return true if access is allowed, false otherwise
     */
    public static boolean canAccessUserData(String pathUserId, Authentication authentication) {
        if (pathUserId == null || authentication == null) {
            return false;
        }
        
        // Admin can access any user's data
        if (isAdmin(authentication)) {
            return true;
        }
        
        // Extract userId from JWT
        String authenticatedUserId = extractUserIdFromAuth(authentication);
        
        // Verify match
        return authenticatedUserId != null && authenticatedUserId.equals(pathUserId);
    }
    
    /**
     * Verify user access and throw exception if denied
     * 
     * @param pathUserId the userId from path variable
     * @param authentication the authentication object
     * @throws SecurityException if access is denied
     */
    public static void verifyUserAccess(String pathUserId, Authentication authentication) {
        if (!canAccessUserData(pathUserId, authentication)) {
            throw new SecurityException("Access denied: You can only access your own data");
        }
    }
}

