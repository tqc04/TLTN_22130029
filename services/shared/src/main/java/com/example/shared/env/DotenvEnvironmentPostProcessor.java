package com.example.shared.env;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads variables from a .env file before Spring context is fully initialized.
 * This allows using ${ENV_VAR} placeholders in application.yml/properties reliably.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, PriorityOrdered {
    private static final String PROPERTY_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String startDir = System.getProperty("user.dir");
        String envDir = findEnvDirectory(startDir);

        Dotenv dotenv = (envDir != null
                ? Dotenv.configure().directory(envDir)
                : Dotenv.configure())
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .systemProperties()
                .load();

        Map<String, Object> map = new HashMap<>();
        dotenv.entries().forEach(e -> map.put(e.getKey(), e.getValue()));

        if (!map.isEmpty()) {
            MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
            environment.getPropertySources().addFirst(propertySource);
        }
    }

    private String findEnvDirectory(String startDir) {
        if (startDir == null || startDir.isBlank()) return null;
        java.io.File dir = new java.io.File(startDir);
        for (int i = 0; i < 6 && dir != null; i++) {
            java.io.File envFile = new java.io.File(dir, ".env");
            if (envFile.exists()) {
                return dir.getAbsolutePath();
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}


