# Quick Start Guide: Database-Backed Card Storage

This guide shows you how to migrate from in-memory card storage to a database.

## Prerequisites

- Java 17+
- Maven
- PostgreSQL (or use H2 for quick testing)

## Option 1: Quick Start with H2 (In-Memory Database)

**Perfect for testing the concept without installing PostgreSQL!**

### Step 1: Update pom.xml

Add these dependencies to `forge-web-starter/pom.xml`:

```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- H2 Database (for quick testing) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Step 2: Update application.yml

In `src/main/resources/application.yml`, use these settings:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:forgedb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        show_sql: true
  
  h2:
    console:
      enabled: true
      path: /h2-console
```

### Step 3: Build and Run

```bash
cd forge-web-starter
mvn clean install
mvn spring-boot:run
```

### Step 4: Test the API

```bash
# Create a card
curl -X POST http://localhost:8080/api/v2/cards \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Lightning Bolt",
    "type": "Instant",
    "manaCost": "{R}",
    "colors": "R",
    "text": "Lightning Bolt deals 3 damage to any target.",
    "rarity": "Common"
  }'

# Get all cards (paginated)
curl http://localhost:8080/api/v2/cards?page=0&size=10

# Search for cards
curl "http://localhost:8080/api/v2/cards/search?name=bolt"

# View H2 Console
# Open browser: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:forgedb
# Username: sa
# Password: (leave empty)
```

---

## Option 2: Production Setup with PostgreSQL

### Step 1: Install PostgreSQL

**Windows:**
```bash
# Download from: https://www.postgresql.org/download/windows/
# Or use Chocolatey:
choco install postgresql
```

**Mac:**
```bash
brew install postgresql
brew services start postgresql
```

**Linux:**
```bash
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### Step 2: Create Database

```bash
# Login to PostgreSQL
psql -U postgres

# Create database and user
CREATE DATABASE forge;
CREATE USER forge WITH PASSWORD 'forge';
GRANT ALL PRIVILEGES ON DATABASE forge TO forge;
\q
```

### Step 3: Update pom.xml

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
```

### Step 4: Update application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/forge
    username: forge
    password: forge
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### Step 5: Build and Run

```bash
cd forge-web-starter
mvn clean install
mvn spring-boot:run
```

---

## Option 3: Cloud Deployment (Heroku Example)

### Step 1: Create Heroku App

```bash
# Install Heroku CLI
# Windows: https://devcenter.heroku.com/articles/heroku-cli

# Login and create app
heroku login
heroku create forge-mtg-api

# Add PostgreSQL addon (free tier)
heroku addons:create heroku-postgresql:mini
```

### Step 2: Update application.yml for Heroku

Heroku automatically provides DATABASE_URL environment variable:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
```

### Step 3: Deploy

```bash
git add .
git commit -m "Add database support"
git push heroku main
```

---

## Migrating Existing Card Data

If you have existing Forge card files, create a data loader:

```java
@Component
public class CardDataLoader implements CommandLineRunner {
    
    private final CardService cardService;
    
    @Override
    public void run(String... args) {
        // Only load if database is empty
        if (cardService.getTotalCount() > 0) {
            System.out.println("Database already has cards, skipping import");
            return;
        }
        
        System.out.println("Loading cards from Forge data files...");
        
        // Read your existing card files
        List<Card> cards = loadCardsFromForgeFiles();
        
        // Bulk import
        cardService.bulkImport(cards);
        
        System.out.println("Imported " + cards.size() + " cards");
    }
    
    private List<Card> loadCardsFromForgeFiles() {
        // TODO: Implement based on your Forge file format
        // This would read from your existing StaticData/CardDb
        return new ArrayList<>();
    }
}
```

---

## Performance Tips

### 1. Use Pagination

Always use pagination for large result sets:

```java
// BAD: Returns all 25,000+ cards
GET /api/v2/cards

// GOOD: Returns 50 cards at a time
GET /api/v2/cards?page=0&size=50
```

### 2. Add Indexes

For frequently searched fields, add database indexes:

```sql
CREATE INDEX idx_card_name ON cards(name);
CREATE INDEX idx_card_type ON cards(type);
CREATE INDEX idx_card_colors ON cards(colors);
```

### 3. Use Caching

For popular cards, add Redis caching:

```java
@Cacheable("cards")
public Optional<Card> findById(Long id) {
    return cardRepository.findById(id);
}
```

### 4. Optimize Queries

Use projections to fetch only needed fields:

```java
public interface CardSummary {
    Long getId();
    String getName();
    String getType();
}

List<CardSummary> findAllProjectedBy();
```

---

## Troubleshooting

### "Table 'cards' doesn't exist"

Set `spring.jpa.hibernate.ddl-auto=create` in application.yml (dev only!)

### "Connection refused to PostgreSQL"

Check PostgreSQL is running:
```bash
# Windows
sc query postgresql-x64-14

# Mac/Linux
brew services list
# or
sudo systemctl status postgresql
```

### "Out of memory"

You're likely loading all cards at once. Use pagination!

---

## Next Steps

1. ✅ Start with H2 to test the concept
2. ✅ Create a few sample cards via API
3. ✅ Test pagination and search
4. ✅ Switch to PostgreSQL for persistence
5. ✅ Add data migration from existing Forge files
6. ✅ Deploy to cloud (Heroku/Railway/AWS)
7. ✅ Add Redis caching for performance
8. ✅ Monitor and optimize queries

---

## Comparison: Before vs After

| Aspect | Before (In-Memory) | After (Database) |
|--------|-------------------|------------------|
| Storage | ArrayList in RAM | PostgreSQL database |
| Startup time | 30-60 seconds | 2-5 seconds |
| Memory usage | 2-4 GB | 200-500 MB |
| Data persistence | Lost on restart | Persisted |
| Search speed | O(n) linear scan | O(log n) with indexes |
| Scalability | 1 instance only | Infinite instances |
| Cloud ready | ❌ No | ✅ Yes |
| Cost | N/A | $0-50/month |

---

Need help? Check:
- DATABASE_SOLUTION.md - Detailed architecture guide
- Spring Data JPA docs: https://spring.io/projects/spring-data-jpa
- PostgreSQL docs: https://www.postgresql.org/docs/

