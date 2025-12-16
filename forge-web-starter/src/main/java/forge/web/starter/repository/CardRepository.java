package forge.web.starter.repository;

import forge.web.starter.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Card entities.
 * <p>
 * No need to write SQL! Spring generates queries automatically from method names.
 * <p>
 * Examples:
 * - findByName("Lightning Bolt") → SELECT * FROM cards WHERE name = 'Lightning Bolt'
 * - findByType("Instant") → SELECT * FROM cards WHERE type = 'Instant'
 * - findByNameContaining("bolt") → SELECT * FROM cards WHERE name LIKE '%bolt%'
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    // Automatic query generation by Spring
    Optional<Card> findByName(String name);

    // Find card by name and edition (for duplicate detection during import)
    Optional<Card> findByNameAndEdition(String name, String edition);

    List<Card> findByType(String type);

    List<Card> findByRarity(String rarity);

    List<Card> findByEdition(String edition);

    // Paginated search by name (case-insensitive)
    Page<Card> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Paginated search by type
    Page<Card> findByType(String type, Pageable pageable);

    /**
     * Advanced search with multiple optional filters.
     * Returns paginated results.
     * <p>
     * Example: searchCards("bolt", "Instant", "R", pageable)
     */
    @Query("SELECT c FROM Card c WHERE " +
           "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:type IS NULL OR c.type = :type) AND " +
           "(:colors IS NULL OR c.colors LIKE CONCAT('%', :colors, '%'))")
    Page<Card> searchCards(
        @Param("name") String name,
        @Param("type") String type,
        @Param("colors") String colors,
        Pageable pageable
    );

    /**
     * Find cards by multiple colors.
     * Example: findByColorsIn(Arrays.asList("U", "U,B"))
     */
    @Query("SELECT c FROM Card c WHERE c.colors IN :colors")
    List<Card> findByColorsIn(@Param("colors") List<String> colors);

    /**
     * Count cards by type (useful for statistics).
     */
    long countByType(String type);

    /**
     * Find cards with power greater than or equal to specified value.
     */
    List<Card> findByPowerGreaterThanEqual(Integer power);
}

