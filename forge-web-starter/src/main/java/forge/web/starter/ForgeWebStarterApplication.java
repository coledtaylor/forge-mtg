package forge.web.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application entry point.
 *
 * The @SpringBootApplication annotation enables:
 * - Auto-configuration: Automatically configures Spring based on dependencies
 * - Component scanning: Finds and registers Spring components
 * - Configuration properties: Loads application.properties
 */
@SpringBootApplication
public class ForgeWebStarterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForgeWebStarterApplication.class, args);
        System.out.println("\n✅ Forge Web Starter is running!");
        System.out.println("📝 Open http://localhost:8080 in your browser");
        System.out.println("🧪 Test WebSocket at http://localhost:8080/test.html\n");
    }
}

