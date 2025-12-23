package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            @Value("${security.jwt.secret:}") String secret,
            @Value("${JWT_SECRET:}") String envSecret) {
        String effective = (secret != null && !secret.isBlank()) ? secret : envSecret;
        if (effective == null || effective.isBlank()) {
            // Use the same default secret as auth service for consistency
            effective = "CHANGE_THIS_TO_A_VERY_LONG_RANDOM_SECRET_KEY_AT_LEAST_256_BITS";
        }
        SecretKey key = new SecretKeySpec(effective.getBytes(StandardCharsets.UTF_8), "HS256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:5173", "https://accounts.google.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Auth endpoints chain: bypass JWT entirely for auth endpoints (HIGHEST PRIORITY)
    @Bean
    @Order(1)
    public SecurityWebFilterChain authPublicChain(ServerHttpSecurity http, CorsConfigurationSource corsConfigurationSource) {
        http
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                "/api/auth/**",
                "/oauth2/**",
                "/login/oauth2/**"
            ))
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(ex -> ex.anyExchange().permitAll());
        return http.build();
    }

    // Public chain: bypass JWT entirely for public GET endpoints even if Authorization header is present
    @Bean
    @Order(2)
    public SecurityWebFilterChain publicSecurityFilterChain(ServerHttpSecurity http, CorsConfigurationSource corsConfigurationSource) {
        http
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                HttpMethod.GET, 
                "/actuator/**",
                "/api/products/**",
                "/api/brands/**",
                "/api/categories/**",
                "/api/product-variants/**",
                "/api/reviews/**",
                "/api/vouchers/**",
                "/api/shipping/**"
            ))
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(ex -> ex.anyExchange().permitAll());
        return http.build();
    }

    // Chatbot & AI public chain: allow all chatbot/AI/recommendations without JWT
    @Bean
    @Order(3)
    public SecurityWebFilterChain chatbotPublicChain(ServerHttpSecurity http, CorsConfigurationSource corsConfigurationSource) {
        http
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                "/api/chatbot/**",
                "/api/ai/**",
                "/api/recommendations/**",
                "/api/knowledge/**"
            ))
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(ex -> ex.anyExchange().permitAll());
        return http.build();
    }

    // Main security filter chain with proper CORS and JWT authentication
    @Bean
    @Order(4)
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, CorsConfigurationSource corsConfigurationSource) {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("role");
        gac.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(gac);

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(ex -> ex
                // Public endpoints - no authentication required
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers("/ws/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/api/*/health").permitAll()
                .pathMatchers("/api/auth/**").permitAll()
                .pathMatchers("/oauth2/**").permitAll()
                .pathMatchers("/login/oauth2/**").permitAll()
                .pathMatchers("/api/shipping/**").permitAll() // All shipping endpoints public like monolithic
                .pathMatchers("/api/payments/vnpay/callback", "/api/payments/vnpay/return", "/api/payments/vnpay/validate").permitAll() // Payment gateway callbacks only
                .pathMatchers("/api/payments/validate-card").permitAll() // Card validation public utility

                // All other endpoints require authentication - individual services handle their own security
                // Order creation, payment creation, cart operations, inventory management all require valid JWT tokens

                // Admin endpoints - require ADMIN role (accept both ROLE_ADMIN and plain ADMIN)
                // Fine-grained admin areas by role
                .pathMatchers("/api/admin/users/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_USER_MANAGER", "USER_MANAGER")
                .pathMatchers("/api/admin/products/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_PRODUCT_MANAGER", "PRODUCT_MANAGER")
                .pathMatchers("/api/admin/orders/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_ORDER_MANAGER", "ORDER_MANAGER", "ROLE_SUPPORT", "SUPPORT")
                .pathMatchers("/api/admin/support/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_SUPPORT", "SUPPORT")
                .pathMatchers("/api/admin/moderation/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_MODERATOR", "MODERATOR")
                // Catch-all admin fallback
                .pathMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/inventory/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.PUT, "/api/inventory/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.DELETE, "/api/inventory/**").hasRole("ADMIN")
                
                // Protected endpoints - require authentication (specific rules first)
                .pathMatchers(HttpMethod.PUT, "/api/orders/**").authenticated()
                .pathMatchers(HttpMethod.DELETE, "/api/orders/**").authenticated()
                .pathMatchers("/api/orders/{orderId:[0-9]+}/**").authenticated() // Only numeric order IDs need auth
                .pathMatchers("/api/payments/process", "/api/payments/confirm", "/api/payments/failed", "/api/payments/high-risk", "/api/payments/stats").authenticated() // Only internal payment endpoints need auth
                .pathMatchers("/api/users/**").authenticated()
                .pathMatchers(HttpMethod.POST, "/api/reviews/**").authenticated()
                .pathMatchers(HttpMethod.PUT, "/api/reviews/**").authenticated()
                .pathMatchers(HttpMethod.DELETE, "/api/reviews/**").authenticated()
                
                // Default: permit all for other routes (like monolithic)
                .anyExchange().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(jwtAuthConverter)))
            );
        return http.build();
    }
}


