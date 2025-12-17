package forge.web.starter.controller;

import forge.web.starter.service.ForgeCardImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for manually triggering card imports.
 * <p>
 * This allows you to re-import cards without restarting the application.
 */
@RestController
@RequestMapping("/api/admin/cards")
public class CardImportController {

    private static final Logger log = LoggerFactory.getLogger(CardImportController.class);

    private final ForgeCardImporter cardImporter;

    public CardImportController(ForgeCardImporter cardImporter) {
        this.cardImporter = cardImporter;
    }

    /**
     * Manually trigger a card import.
     * <p>
     * POST /api/admin/cards/import
     * <p>
     * Example: {@code curl -X POST http://localhost:8080/api/admin/cards/import}
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importCards() {
        log.info("Manual card import triggered via API");
        long startTime = System.currentTimeMillis();

        Map<String, Object> response = new HashMap<>();

        try {
            cardImporter.importCards();
            long duration = System.currentTimeMillis() - startTime;

            response.put("success", true);
            response.put("message", "Card import completed successfully");
            response.put("durationSeconds", duration / 1000.0);

            log.info("✅ Manual card import completed in {} seconds", duration / 1000.0);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            response.put("success", false);
            response.put("message", "Card import failed: " + e.getMessage());
            response.put("durationSeconds", duration / 1000.0);

            log.error("❌ Manual card import failed", e);
            return ResponseEntity.status(500).body(response);
        }
    }
}

