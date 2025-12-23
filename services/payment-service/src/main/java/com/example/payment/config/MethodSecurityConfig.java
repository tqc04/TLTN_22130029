package com.example.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Allow health check endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Allow VNPay endpoints (callback and return) - these are called by payment gateway
                .requestMatchers("/api/payments/vnpay/callback", "/api/payments/vnpay/return", "/api/payments/vnpay/validate").permitAll()
                // Allow card validation endpoint (public utility)
                .requestMatchers("/api/payments/validate-card").permitAll()
                // Allow inter-service payment processing (with basic auth or JWT)
                .requestMatchers("/api/payments/process").permitAll()
                // Require authentication for payment creation endpoints
                .requestMatchers("/api/payments/create", "/api/payments/momo/create", "/api/payments/banks").authenticated()
                // Require authentication for admin endpoints
                .requestMatchers("/api/payments/failed", "/api/payments/high-risk", "/api/payments/stats", "/api/payments/*/refund").hasRole("ADMIN")
                .requestMatchers("/api/payments/user/**", "/api/payments/order/**", "/api/payments/status/**").hasRole("ADMIN")
                // Allow all other endpoints for inter-service communication
                .anyRequest().permitAll()
            )
            .httpBasic(httpBasic -> httpBasic
                .realmName("payment-service")
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails serviceUser = User.builder()
            .username("service")
            .password("{noop}service123")
            .roles("USER")
            .build();

        UserDetails adminUser = User.builder()
            .username("admin")
            .password("{noop}admin123")
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(serviceUser, adminUser);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}


