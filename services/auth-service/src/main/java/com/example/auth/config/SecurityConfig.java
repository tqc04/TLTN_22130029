package com.example.auth.config;

import com.example.auth.filter.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            // OAuth2 login cần session để giữ Authentication qua redirect,
            // nên không dùng STATELESS tại auth-service
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/health",
                    "/api/auth/verify-email",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/auth/phone/**",
                    "/api/auth/refresh",
                    "/api/auth/oauth2-signup",
                    "/api/auth/oauth2-success",
                    "/api/auth/oauth2-failure",
                    "/api/auth/introspect",
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/api/auth/oauth2-success", true)
                .failureUrl("/api/auth/oauth2-failure")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oauth2UserService())
                )
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));
        return http.build();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        return new DefaultOAuth2UserService();
    }

    /**
     * Register rate limiting filter to run before Spring Security
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(
            @Autowired RateLimitingFilter rateLimitingFilter) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(rateLimitingFilter);
        registration.addUrlPatterns("/api/auth/login");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // Run before Spring Security
        registration.setName("rateLimitingFilter");
        return registration;
    }

}


