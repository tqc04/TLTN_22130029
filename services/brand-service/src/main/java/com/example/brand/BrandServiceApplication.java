package com.example.brand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BrandServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BrandServiceApplication.class, args);
    }
}
