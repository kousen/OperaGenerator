package com.kousenit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for the Embabel-based Opera Generator.
 * <p>
 * Shell mode is enabled via the embabel-agent-starter-shell dependency and
 * Spring profiles. Run with:
 * <pre>
 *   ./gradlew bootRun --args='--spring.profiles.active=shell'
 * </pre>
 * Or simply:
 * <pre>
 *   ./gradlew bootRun
 * </pre>
 * The shell starter auto-configures the shell profile.
 */
@SpringBootApplication
public class OperaApplication {

    public static void main(String[] args) {
        SpringApplication.run(OperaApplication.class, args);
    }
}
