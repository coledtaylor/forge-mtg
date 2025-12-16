package forge.web.starter.controller;

import forge.web.starter.entity.Card;
import forge.web.starter.service.CardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for card operations using a DATABASE instead of in-memory storage.
 * <p>
 * Key improvements over the in-memory version:
 * ✅ Pagination - Returns 50 cards at a time instead of all 25,000+
 * ✅ Database storage - Cards persist across restarts
 * ✅ Fast queries - Database indexes make searches lightning-fast
 * ✅ Low memory - No need to load all cards into RAM
 * ✅ Cloud-ready - Multiple app instances can share the same database
 * <p>
 * Example API calls:
 * - GET /api/v2/cards?page=0&size=50&sort=name,asc
 * - GET /api/v2/cards/search?name=bolt&type=Instant&page=0
 * - GET /api/v2/cards/123
 * - POST /api/v2/cards
 * - DELETE /api/v2/cards/123
 */
@RestController
@RequestMapping("/api/v2/cards")
public class CardControllerV2 {

    private final CardService cardService;

    public CardControllerV2(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * GET /api/v2/cards?page=0&size=50&sort=name,asc
     * <p>
     * Returns a PAGE of cards instead of all cards.
     * <p>
     * Response format:
     * {
     *   "content": [ {...card objects...} ],
     *   "pageable": { "pageNumber": 0, "pageSize": 50 },
     *   "totalPages": 500,
     *   "totalElements": 25000,
     *   "last": false
     * }
     */
    @GetMapping
    public ResponseEntity<Page<Card>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        // Limit page size to prevent abuse
        if (size > 100) size = 100;

        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc")
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        Page<Card> cards = cardService.getAllCards(
            PageRequest.of(page, size, Sort.by(direction, sortBy))
        );

        System.out.println("📋 GET /api/v2/cards - Page " + page + ", " +
                          cards.getNumberOfElements() + " cards returned");

        return ResponseEntity.ok(cards);
    }

    /**
     * GET /api/v2/cards/{id}
     * <p>
     * Get a specific card by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Card> getCard(@PathVariable Long id) {
        System.out.println("🔍 GET /api/v2/cards/" + id);

        return cardService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v2/cards/search?name=bolt&type=Instant&colors=R&page=0&size=20
     * <p>
     * Advanced search with multiple filters.
     * All parameters are optional.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<Card>> searchCards(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String colors,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > 100) size = 100;

        Page<Card> results = cardService.searchCards(
            name, type, colors,
            PageRequest.of(page, size)
        );

        System.out.println("🔎 GET /api/v2/cards/search - Found " +
                          results.getTotalElements() + " cards");

        return ResponseEntity.ok(results);
    }

    /**
     * POST /api/v2/cards
     * <p>
     * Create a new card.
     * <p>
     * Request body example:
     * {
     *   "name": "Lightning Bolt",
     *   "type": "Instant",
     *   "manaCost": "{R}",
     *   "colors": "R",
     *   "text": "Lightning Bolt deals 3 damage to any target.",
     *   "rarity": "Common",
     *   "edition": "M21"
     * }
     */
    @PostMapping
    public ResponseEntity<Card> createCard(@RequestBody Card card) {
        Card saved = cardService.save(card);
        System.out.println("➕ POST /api/v2/cards - Created: " + saved.getName() + " (ID: " + saved.getId() + ")");
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * PUT /api/v2/cards/{id}
     * <p>
     * Update an existing card.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Card> updateCard(@PathVariable Long id, @RequestBody Card card) {
        return cardService.findById(id)
            .map(existing -> {
                card.setId(id);
                Card updated = cardService.save(card);
                System.out.println("✏️ PUT /api/v2/cards/" + id + " - Updated: " + updated.getName());
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/v2/cards/{id}
     * <p>
     * Delete a card by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        System.out.println("🗑️ DELETE /api/v2/cards/" + id);

        return cardService.deleteById(id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    /**
     * GET /api/v2/cards/stats
     * <p>
     * Get statistics about the card database.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCards", cardService.getTotalCount());
        stats.put("creatures", cardService.getCountByType("Creature"));
        stats.put("instants", cardService.getCountByType("Instant"));
        stats.put("sorceries", cardService.getCountByType("Sorcery"));
        stats.put("enchantments", cardService.getCountByType("Enchantment"));
        stats.put("artifacts", cardService.getCountByType("Artifact"));
        stats.put("planeswalkers", cardService.getCountByType("Planeswalker"));
        stats.put("lands", cardService.getCountByType("Land"));

        System.out.println("📊 GET /api/v2/cards/stats");
        return ResponseEntity.ok(stats);
    }
}

