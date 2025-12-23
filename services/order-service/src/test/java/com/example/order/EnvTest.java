package com.example.order;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvTest {

    public static void main(String[] args) {
        System.out.println("=== Testing .env file loading ===");
        
        // Test direct dotenv loading
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .systemProperties()
                .load();

        // Check if some environment variables are loaded
        String dbUsername = dotenv.get("DB_USERNAME");
        String dbPassword = dotenv.get("DB_PASSWORD");
        String geminiApiKey = dotenv.get("GEMINI_API_KEY");
        
        System.out.println("DB_USERNAME: " + dbUsername);
        System.out.println("DB_PASSWORD: " + dbPassword);
        System.out.println("GEMINI_API_KEY: " + geminiApiKey);
        
        // Test system properties (should be set by DotenvEnvironmentPostProcessor)
        String systemDbUsername = System.getProperty("DB_USERNAME");
        String systemDbPassword = System.getProperty("DB_PASSWORD");
        
        System.out.println("\n=== System Properties ===");
        System.out.println("System DB_USERNAME: " + systemDbUsername);
        System.out.println("System DB_PASSWORD: " + systemDbPassword);
        
        // Test if .env file is found
        if (dbUsername != null && dbPassword != null) {
            System.out.println("\n✅ SUCCESS: .env file is loaded correctly!");
            System.out.println("✅ DB_USERNAME = " + dbUsername);
            System.out.println("✅ DB_PASSWORD = " + dbPassword);
        } else {
            System.out.println("\n❌ FAILED: .env file not loaded or variables not found");
        }
    }
}
