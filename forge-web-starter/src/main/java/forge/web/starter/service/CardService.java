package forge.web.starter.service;

import forge.web.starter.entity.Card;
import forge.web.starter.repository.CardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for card business logic.
 * <p>
 * Benefits of a service layer:
 * - Encapsulates business logic separate from HTTP layer
 * - Provides transaction management
 * - Can be cached easily
 * - Easier to test
 */
@Service
@Transactional
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    /**
     * Find card by ID.
     * Could be cached with @Cacheable("cards") for frequently accessed cards.
     */
    public Optional<Card> findById(Long id) {
        return cardRepository.findById(id);
    }

    /**
     * Find card by exact name.
     * Example: findByName("Lightning Bolt")
     */
    public Optional<Card> findByName(String name) {
        return cardRepository.findByName(name);
    }

    /**
     * Get all cards with pagination.
     * Returns a Page object with:
     * - Current page content
     * - Total pages
     * - Total elements
     * - Has next/previous page
     */
    public Page<Card> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }

    /**
     * Search cards with multiple filters.
     * All parameters are optional (can be null).
     */
    public Page<Card> searchCards(String name, String type, String colors, Pageable pageable) {
        return cardRepository.searchCards(name, type, colors, pageable);
    }

    /**
     * Find cards by type with pagination.
     */
    public Page<Card> findByType(String type, Pageable pageable) {
        return cardRepository.findByType(type, pageable);
    }

    /**
     * Create or update a card.
     */
    public Card save(Card card) {
        return cardRepository.save(card);
    }

    /**
     * Delete a card by ID.
     * Returns true if deleted, false if not found.
     */
    public boolean deleteById(Long id) {
        if (cardRepository.existsById(id)) {
            cardRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Get total count of all cards.
     */
    public long getTotalCount() {
        return cardRepository.count();
    }

    /**
     * Get count by type (useful for statistics).
     */
    public long getCountByType(String type) {
        return cardRepository.countByType(type);
    }

    /**
     * Bulk import cards (useful for initial data load).
     * Saves in batches for better performance.
     */
    public void bulkImport(List<Card> cards) {
        cardRepository.saveAll(cards);
    }
}

