# Database Solution for Cloud-Ready Card Storage

## Problem Summary

Currently, Forge stores all card data in local files and loads them into memory, causing:
- **Slow startup times** - Loading thousands of cards from disk on initialization
- **High memory usage** - Keeping all card data in memory
- **Not cloud-ready** - Each instance needs its own copy of card files
- **Difficult to scale** - Can't easily add more instances or share data

## Recommended Solution: PostgreSQL + Spring Data JPA + Caching

### Architecture Overview

```
┌─────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Client    │────▶│  Spring Boot │────▶│ Redis Cache  │────▶│  PostgreSQL  │
│             │◀────│  Controller  │◀────│   (Optional) │◀────│   Database   │
└─────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
```

### Why This Solution?

1. **PostgreSQL** - Industry-standard, ACID-compliant, excellent for structured card data
2. **Spring Data JPA** - Minimal code, automatic CRUD operations, type-safe queries
3. **Redis Cache** - Optional layer to cache frequently accessed cards (Lightning Bolt, etc.)
4. **Pagination** - Return cards in pages (e.g., 50 at a time) instead of all at once
5. **Indexing** - Fast lookups by name, type, color, mana cost, etc.

---

## Implementation Plan

### Phase 1: Add Database Dependencies

Add to `forge-web-starter/pom.xml`:

```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- PostgreSQL Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Optional: H2 for local development/testing -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Optional: Redis for caching -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Phase 2: Create Database Entity

Transform `CardDTO` into a JPA entity:

```java
package forge.web.starter.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards", indexes = {
    @Index(name = "idx_card_name", columnList = "name"),
    @Index(name = "idx_card_type", columnList = "type"),
    @Index(name = "idx_card_mana", columnList = "manaCost"),
    @Index(name = "idx_card_colors", columnList = "colors")
})
public class Card {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(nullable = false, length = 100)
    private String type;
    
    @Column(length = 50)
    private String manaCost;  // e.g., "{2}{U}{U}"
    
    @Column(length = 10)
    private String colors;    // e.g., "U,B" for Blue/Black
    
    @Column(length = 10)
    private String rarity;    // Common, Uncommon, Rare, Mythic
    
    private Integer power;
    private Integer toughness;
    
    @Column(columnDefinition = "TEXT")
    private String text;
    
    @Column(length = 10)
    private String edition;   // e.g., "M21", "DOM"
    
    @Column(length = 20)
    private String collectorNumber;
    
    @Column(length = 500)
    private String imageUrl;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters...
}
```

### Phase 3: Create Repository

Spring Data JPA handles all SQL automatically:

```java
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

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    
    // Automatic query generation by Spring
    Optional<Card> findByName(String name);
    List<Card> findByType(String type);
    List<Card> findByRarity(String rarity);
    
    // Paginated search
    Page<Card> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    // Custom query for advanced search
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
    
    // Find by multiple colors
    @Query("SELECT c FROM Card c WHERE c.colors IN :colors")
    List<Card> findByColorsIn(@Param("colors") List<String> colors);
}
```

### Phase 4: Create Service Layer

```java
package forge.web.starter.service;

import forge.web.starter.entity.Card;
import forge.web.starter.repository.CardRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CardService {
    
    private final CardRepository cardRepository;
    
    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }
    
    @Cacheable("cards")
    public Card findById(Long id) {
        return cardRepository.findById(id).orElse(null);
    }
    
    @Cacheable(value = "cardsByName", key = "#name")
    public Card findByName(String name) {
        return cardRepository.findByName(name).orElse(null);
    }
    
    public Page<Card> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }
    
    public Page<Card> searchCards(String name, String type, String colors, Pageable pageable) {
        return cardRepository.searchCards(name, type, colors, pageable);
    }
    
    public Card createCard(Card card) {
        return cardRepository.save(card);
    }
    
    public boolean deleteCard(Long id) {
        if (cardRepository.existsById(id)) {
            cardRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
```

### Phase 5: Update Controller for Pagination

```java
package forge.web.starter.controller;

import forge.web.starter.entity.Card;
import forge.web.starter.service.CardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    
    private final CardService cardService;
    
    public CardController(CardService cardService) {
        this.cardService = cardService;
    }
    
    /**
     * GET /api/cards?page=0&size=50&sort=name,asc
     */
    @GetMapping
    public Page<Card> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "name") String sortBy) {
        
        return cardService.getAllCards(
            PageRequest.of(page, size, Sort.by(sortBy))
        );
    }
    
    /**
     * GET /api/cards/search?name=bolt&type=Instant&colors=R&page=0&size=20
     */
    @GetMapping("/search")
    public Page<Card> searchCards(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String colors,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return cardService.searchCards(
            name, type, colors, 
            PageRequest.of(page, size)
        );
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Card> getCard(@PathVariable Long id) {
        Card card = cardService.findById(id);
        return card != null ? ResponseEntity.ok(card) : ResponseEntity.notFound().build();
    }
    
    @PostMapping
    public Card createCard(@RequestBody Card card) {
        return cardService.createCard(card);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        return cardService.deleteCard(id) 
            ? ResponseEntity.noContent().build() 
            : ResponseEntity.notFound().build();
    }
}
```

### Phase 6: Configuration

Create `application.yml`:

```yaml
spring:
  application:
    name: forge-web-starter
  
  # Database Configuration
  datasource:
    # Production (PostgreSQL)
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/forge}
    username: ${DATABASE_USER:forge}
    password: ${DATABASE_PASSWORD:forge}
    driver-class-name: org.postgresql.Driver
    
    # Alternative: H2 for local development
    # url: jdbc:h2:mem:forgedb
    # driver-class-name: org.h2.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate  # Use 'update' for dev, 'validate' for production
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        show_sql: false
    open-in-view: false
  
  # Redis Cache (Optional)
  cache:
    type: redis
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    
server:
  port: 8080
```

### Phase 7: Database Migration (Flyway or Liquibase)

Add Flyway for version-controlled schema changes:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

Create `src/main/resources/db/migration/V1__create_cards_table.sql`:

```sql
CREATE TABLE cards (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    mana_cost VARCHAR(50),
    colors VARCHAR(10),
    rarity VARCHAR(10),
    power INTEGER,
    toughness INTEGER,
    text TEXT,
    edition VARCHAR(10),
    collector_number VARCHAR(20),
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for fast lookups
CREATE INDEX idx_card_name ON cards(name);
CREATE INDEX idx_card_type ON cards(type);
CREATE INDEX idx_card_mana ON cards(mana_cost);
CREATE INDEX idx_card_colors ON cards(colors);
CREATE INDEX idx_card_edition ON cards(edition);

-- Full-text search index (PostgreSQL specific)
CREATE INDEX idx_card_text_search ON cards USING GIN(to_tsvector('english', name || ' ' || COALESCE(text, '')));
```

---

## Data Migration Strategy

### Option 1: One-Time Bulk Import

Create a data loader that reads existing Forge card files and imports to database:

```java
@Component
public class CardDataImporter implements CommandLineRunner {
    
    private final CardRepository cardRepository;
    private final CardStorageReader cardReader;
    
    @Override
    public void run(String... args) {
        if (cardRepository.count() > 0) {
            return; // Already loaded
        }
        
        System.out.println("Loading cards from Forge data files...");
        
        List<Card> cardsToImport = new ArrayList<>();
        for (CardRules rules : cardReader.loadCards()) {
            Card card = convertToEntity(rules);
            cardsToImport.add(card);
            
            if (cardsToImport.size() >= 1000) {
                cardRepository.saveAll(cardsToImport);
                cardsToImport.clear();
            }
        }
        
        if (!cardsToImport.isEmpty()) {
            cardRepository.saveAll(cardsToImport);
        }
        
        System.out.println("Card import complete: " + cardRepository.count() + " cards loaded");
    }
}
```

### Option 2: Lazy Loading with Cache

Keep a hybrid approach:
- Store card metadata in database (name, type, colors, rarity)
- Load full card rules on-demand
- Cache frequently accessed cards in Redis

---

## Cloud Deployment Options

### Option 1: AWS
- **Database**: RDS PostgreSQL (or Aurora PostgreSQL for auto-scaling)
- **Cache**: ElastiCache Redis
- **App**: Elastic Beanstalk or ECS
- **Cost**: ~$50-200/month depending on size

### Option 2: Azure
- **Database**: Azure Database for PostgreSQL
- **Cache**: Azure Cache for Redis
- **App**: Azure App Service
- **Cost**: Similar to AWS

### Option 3: Google Cloud
- **Database**: Cloud SQL (PostgreSQL)
- **Cache**: Memorystore for Redis
- **App**: Cloud Run or App Engine

### Option 4: Heroku (Easiest for learning)
- **Database**: Heroku Postgres (free tier available)
- **Cache**: Heroku Redis
- **App**: Heroku Dyno
- **Cost**: Free tier available, ~$25/month for production

### Option 5: Railway/Render (Modern alternatives)
- Simple deployment
- Auto-scaling
- Free tier for development

---

## Performance Optimizations

### 1. Pagination Response

```json
{
  "content": [
    { "id": 1, "name": "Lightning Bolt", ... },
    { "id": 2, "name": "Counterspell", ... }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 50
  },
  "totalPages": 500,
  "totalElements": 25000,
  "last": false
}
```

### 2. Database Indexes

```sql
-- Compound index for common searches
CREATE INDEX idx_cards_search ON cards(type, colors, rarity);

-- Full-text search for card text
CREATE INDEX idx_cards_fulltext ON cards USING GIN(to_tsvector('english', text));
```

### 3. Redis Caching Strategy

```java
@Cacheable(value = "popularCards", unless = "#result == null")
public Card findById(Long id) {
    return cardRepository.findById(id).orElse(null);
}

// Cache eviction on update
@CacheEvict(value = "popularCards", key = "#id")
public Card updateCard(Long id, Card card) {
    // ...
}
```

### 4. Query Optimization

```java
// Use projections to fetch only needed fields
public interface CardSummary {
    Long getId();
    String getName();
    String getType();
}

List<CardSummary> findAllProjectedBy();
```

---

## Estimated Performance Gains

| Metric | Current (File-based) | With Database | Improvement |
|--------|---------------------|---------------|-------------|
| Startup time | 30-60 seconds | 2-5 seconds | **90% faster** |
| Memory usage | 2-4 GB | 200-500 MB | **85% less** |
| Card search | 500-1000ms | 10-50ms | **95% faster** |
| Concurrent users | 1-5 | 100+ | **20x scalability** |

---

## Migration Checklist

- [ ] Add Spring Data JPA and PostgreSQL dependencies
- [ ] Create Card entity with proper indexes
- [ ] Create CardRepository interface
- [ ] Create CardService with caching
- [ ] Update CardController for pagination
- [ ] Set up Flyway for database migrations
- [ ] Create data import script from Forge files
- [ ] Set up local PostgreSQL for testing
- [ ] Add Redis for caching (optional)
- [ ] Test with production-like data volume
- [ ] Deploy to cloud provider
- [ ] Monitor performance and optimize indexes

---

## Alternative: NoSQL (MongoDB)

If you prefer document storage:

**Pros:**
- Flexible schema (good for Magic's varied card types)
- Easy to store complex nested data
- Horizontal scaling out of the box

**Cons:**
- Less mature query capabilities
- No ACID transactions across documents
- Harder to do complex relational queries

**When to use:** If you need extreme flexibility or expect massive scale (100M+ cards).

---

## Conclusion

**Recommended approach:**
1. Start with PostgreSQL + Spring Data JPA + Pagination
2. Add Redis caching for popular cards
3. Use Flyway for database versioning
4. Deploy to Heroku or Railway for easy start
5. Migrate to AWS/Azure when scaling needs grow

This gives you:
- ✅ Fast startup (no loading all cards into memory)
- ✅ Low memory footprint
- ✅ Cloud-ready (stateless app servers)
- ✅ Scalable (add more app instances)
- ✅ Maintainable (industry-standard stack)

Let me know if you want me to implement any part of this solution!

