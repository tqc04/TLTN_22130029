package com.example.voucher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.jwt.secret:}")
    private String jwtSecretProp;

    @Value("${JWT_SECRET:}")
    private String jwtSecretEnv;

    @Bean
    public JwtDecoder jwtDecoder() {
        String effective = (jwtSecretProp != null && !jwtSecretProp.isBlank()) ? jwtSecretProp : jwtSecretEnv;
        if (effective == null || effective.isBlank()) {
            effective = "CHANGE_THIS_TO_A_VERY_LONG_RANDOM_SECRET_KEY_AT_LEAST_256_BITS";
        }
        SecretKey key = new SecretKeySpec(effective.getBytes(StandardCharsets.UTF_8), "HS256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("role");
        gac.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(gac);

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/vouchers/health", "/api/vouchers/public", "/api/vouchers/validate", "/api/vouchers/{code}", "/api/vouchers/can-use").permitAll()
                // Allow user voucher endpoints without authentication (frontend will handle auth)
                // Use regex pattern to match any userId
                .requestMatchers("/api/vouchers/user/*/available").permitAll()
                .requestMatchers("/api/vouchers/user/*/all").permitAll()
                .requestMatchers("/api/vouchers/user/*/claim/*").permitAll()
                .requestMatchers("/api/vouchers", "/api/vouchers/{id}").hasRole("ADMIN")
                .requestMatchers("/api/vouchers/usage", "/api/vouchers/usage/order/{orderId}").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder()).jwtAuthenticationConverter(jwtAuthConverter)));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
