package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@org.springframework.scheduling.annotation.EnableScheduling
public class OrderServiceApplication {
    public static void main(String[] args) {
        // Load .env as early as possible (fallback to parent dirs)
        Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .systemProperties()
                .load();

        SpringApplication.run(OrderServiceApplication.class, args);
    }
}


