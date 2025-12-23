package com.example.review.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Configure JWT authentication converter
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("role");
        gac.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(gac);
        
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - Không cần login
                .requestMatchers("/api/reviews/health").permitAll()
                .requestMatchers("/api/reviews/product/**").permitAll()  // Xem reviews của sản phẩm
                .requestMatchers("/api/reviews/*/helpful").permitAll()   // Vote helpful (optional auth)
                .requestMatchers("/actuator/**").permitAll()
                
                // Protected endpoints - Cần login
                .requestMatchers("/api/reviews").authenticated()          // Tạo review
                .requestMatchers("/api/reviews/*").authenticated()        // Update/Delete review
                .requestMatchers("/api/reviews/user/**").authenticated()  // Xem reviews của user
                
                // Cho phép tất cả các request khác để tránh block
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()).jwtAuthenticationConverter(jwtAuthConverter))
            );
        
        return http.build();
    }
}

