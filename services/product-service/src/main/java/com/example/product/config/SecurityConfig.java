package com.example.product.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    @Value("${security.jwt.secret:}")
    private String jwtSecret;
    
    @Value("${JWT_SECRET:}")
    private String envJwtSecret;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("role");
        gac.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(gac);
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public product endpoints
                .requestMatchers(HttpMethod.GET,
                    "/api/products",
                    "/api/products/",
                    "/api/products/*",
                    "/api/products/**",
                    "/api/brands",
                    "/api/brands/**",
                    "/api/categories",
                    "/api/categories/**",
                    "/api/product-variants",
                    "/api/product-variants/**",
                    "/api/reviews",
                    "/api/reviews/**",
                    "/api/vouchers",
                    "/api/vouchers/**",
                    "/api/products/featured",
                    "/api/products/on-sale",
                    "/api/products/flash-sale/**",
                    "/actuator/health"
                ).permitAll()
                // Allow internal service calls for image management
                // These endpoints are called by admin-service after file upload
                .requestMatchers(HttpMethod.POST,
                    "/api/products/*/images",
                    "/api/products/*/images/**"
                ).permitAll()
                .requestMatchers(HttpMethod.DELETE,
                    "/api/products/*/images/*"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()).jwtAuthenticationConverter(jwtAuthConverter))
            );
        return http.build();
    }
}


