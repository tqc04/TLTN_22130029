package com.example.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
public class ReviewServiceApplication {

    @Value("${interservice.username:service}")
    private String interserviceUsername;
    
    @Value("${interservice.password:service123}")
    private String interservicePassword;

    public static void main(String[] args) {
        SpringApplication.run(ReviewServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Add interceptor to add service authentication headers
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add((request, body, execution) -> {
            // Add basic auth or custom headers for inter-service communication
            request.getHeaders().add("X-Service-Auth", "review-service");
            request.getHeaders().add("X-Internal-Request", "true");
            
            // Optional: Add basic auth if needed
            // String auth = interserviceUsername + ":" + interservicePassword;
            // byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            // String authHeader = "Basic " + new String(encodedAuth);
            // request.getHeaders().add("Authorization", authHeader);
            
            return execution.execute(request, body);
        });
        
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
