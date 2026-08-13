package com.chickenexpress.foodorder;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ChickenExpress — main application entry point.
 *
 * Loads environment variables from .env (if present) before the Spring
 * context starts, so application.properties can reference them via ${VAR}.
 *
 * Access the application at http://localhost:8080 after startup.
 */
@SpringBootApplication
public class FoodOrderApplication {

    public static void main(String[] args) {
        // Load .env file — ignores missing file so production env vars still work
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();

        // Push each .env entry into system properties so Spring can read ${VAR}
        dotenv.entries().forEach(entry ->
            System.setProperty(entry.getKey(), entry.getValue())
        );

        SpringApplication.run(FoodOrderApplication.class, args);
    }
}
