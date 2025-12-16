package forge.web.starter.controller;

import forge.web.starter.model.CardDTO;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST Controller for card-related endpoints.
 *
 * @RestController - Combines @Controller and @ResponseBody
 * @RequestMapping - Base path for all endpoints in this controller
 *
 * This demonstrates basic CRUD operations (Create, Read, Update, Delete).
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final List<CardDTO> cardDatabase = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public CardController() {
        // Initialize with some sample cards
        cardDatabase.add(new CardDTO(idCounter.getAndIncrement(), "Lightning Bolt", "Instant", 1, 0, 0, "Deal 3 damage to any target."));
        cardDatabase.add(new CardDTO(idCounter.getAndIncrement(), "Grizzly Bears", "Creature", 2, 2, 2, "A simple 2/2 creature."));
        cardDatabase.add(new CardDTO(idCounter.getAndIncrement(), "Counterspell", "Instant", 2, 0, 0, "Counter target spell."));
        cardDatabase.add(new CardDTO(idCounter.getAndIncrement(), "Black Lotus", "Artifact", 0, 0, 0, "Add three mana of any color."));
    }

    /**
     * GET /api/cards
     * <p>
     * Returns all cards in the database.
     * Try it: <a href="http://localhost:8080/api/cards">...</a>
     */
    @GetMapping
    public List<CardDTO> getAllCards() {
        System.out.println("📋 GET /api/cards - Returning " + cardDatabase.size() + " cards");
        return cardDatabase;
    }

    /**
     * GET /api/cards/{id}
     * <p>
     * Returns a specific card by ID.
     * Try it: <a href="http://localhost:8080/api/cards/1">...</a>
     *
     * @PathVariable - Extracts the {id} from the URL path
     */
    @GetMapping("/{id}")
    public CardDTO getCard(@PathVariable Long id) {
        System.out.println("🔍 GET /api/cards/" + id);
        return cardDatabase.stream()
            .filter(card -> card.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    /**
     * GET /api/cards/search?name=Lightning
     * <p>
     * Search for cards by name (case-insensitive partial match).
     * Try it: <a href="http://localhost:8080/api/cards/search?name=bolt">...</a>
     *
     * @RequestParam - Extracts query parameter from URL
     */
    @GetMapping("/search")
    public List<CardDTO> searchCards(@RequestParam String name) {
        System.out.println("🔎 GET /api/cards/search?name=" + name);
        return cardDatabase.stream()
            .filter(card -> card.getName().toLowerCase().contains(name.toLowerCase()))
            .toList();
    }

    /**
     * POST /api/cards
     * <p>
     * Create a new card.
     * Send JSON in request body like:
     * {
     *   "name": "New Card",
     *   "type": "Sorcery",
     *   "manaCost": 3,
     *   "text": "Does something cool"
     * }
     *
     * @RequestBody - Automatically deserializes JSON to CardDTO
     */
    @PostMapping
    public CardDTO createCard(@RequestBody CardDTO card) {
        card.setId(idCounter.getAndIncrement());
        cardDatabase.add(card);
        System.out.println("➕ POST /api/cards - Created: " + card.getName());
        return card;
    }

    /**
     * DELETE /api/cards/{id}
     * <p>
     * Delete a card by ID.
     * Try it with curl or Postman: DELETE <a href="http://localhost:8080/api/cards/1">...</a>
     */
    @DeleteMapping("/{id}")
    public boolean deleteCard(@PathVariable Long id) {
        System.out.println("🗑️ DELETE /api/cards/" + id);
        return cardDatabase.removeIf(card -> card.getId().equals(id));
    }
}

