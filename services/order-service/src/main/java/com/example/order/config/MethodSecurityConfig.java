package com.example.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {

    @Value("${security.jwt.secret:}")
    private String jwtSecret;
    
    @Value("${JWT_SECRET:}")
    private String envJwtSecret;

    @Value("${interservice.username:service}")
    private String interserviceUsername;

    @Value("${interservice.password:service123}")
    private String interservicePassword;

    @Bean
    public JwtDecoder jwtDecoder() {
        String effective = (jwtSecret != null && !jwtSecret.isBlank()) ? jwtSecret : envJwtSecret;
        if (effective == null || effective.isBlank()) {
            effective = "CHANGE_THIS_TO_A_VERY_LONG_RANDOM_SECRET_KEY_AT_LEAST_256_BITS";
        }
        SecretKey key = new SecretKeySpec(effective.getBytes(StandardCharsets.UTF_8), "HS256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public AuthenticationProvider serviceAuthenticationProvider() {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) {
                if (authentication instanceof UsernamePasswordAuthenticationToken) {
                    UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
                    String username = (String) token.getPrincipal();
                    String password = (String) token.getCredentials();

                    // Check service credentials
                    if (interserviceUsername.equals(username) && interservicePassword.equals(password)) {
                        return new UsernamePasswordAuthenticationToken(
                            username,
                            password,
                            Arrays.asList(new SimpleGrantedAuthority("ROLE_SERVICE"))
                        );
                    }
                }
                throw new BadCredentialsException("Invalid credentials");
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
            }
        };
    }

    @Bean
    public SecurityFilterChain orderSecurityFilterChain(HttpSecurity http) throws Exception {
        // Configure JWT authentication converter
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("role");
        gac.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(gac);

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(serviceAuthenticationProvider())
            .authorizeHttpRequests(authz -> authz
                // Allow health check endpoints
                .requestMatchers("/actuator/health", "/actuator/info", "/api/orders/health").permitAll()
                // Allow shipping endpoints
                .requestMatchers("/api/shipping/**").permitAll()
                // Require authentication for admin endpoints - allow ORDER_MANAGER and SUPPORT for order management
                .requestMatchers("/api/admin/orders/**").hasAnyRole("ADMIN", "ORDER_MANAGER", "SUPPORT")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Require authentication for order operations
                .requestMatchers(HttpMethod.POST, "/api/orders").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders/my-orders").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/orders/number/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/orders/by-number/*/cancel").authenticated()
                // Require authentication for other order endpoints
                .requestMatchers("/api/orders/**").authenticated()
                // Require authentication for other endpoints
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Basic authentication failed\"}");
                })
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthConverter)
                )
                .authenticationEntryPoint((request, response, ex) -> {
                    // Check if this is a basic auth request by looking for Authorization header
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Basic ")) {
                        // This is a basic auth request, don't handle with JWT entry point
                        // Let basic auth handle it
                        return;
                    }
                    // This is a JWT request, handle JWT validation errors
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + ex.getMessage() + "\"}");
                })
            );

        return http.build();
    }
}


