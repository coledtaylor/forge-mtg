# 🎮 Forge Web Starter

A Spring Boot learning project for understanding REST APIs, WebSocket communication, and game state management before integrating with the Forge MTG game engine.

## 📚 What You'll Learn

This starter project teaches you the essential Spring Boot concepts you'll need for building the full Forge web integration:

1. **REST APIs** - Creating endpoints that clients can call
2. **WebSocket** - Real-time bidirectional communication
3. **Dependency Injection** - How Spring manages components
4. **DTOs** - Data Transfer Objects for API communication
5. **Service Layer** - Business logic separation
6. **Session Management** - Handling multiple concurrent games

## 📖 Documentation

| Guide                                                       | Purpose                           | Time      |
|-------------------------------------------------------------|-----------------------------------|-----------|
| **[Getting Started](documentation/GETTING_STARTED.md)**     | Run the app in 5 minutes          | 5-10 min  |
| **[Learning Guide](documentation/LEARNING_GUIDE.md)**       | Interactive tutorials & exercises | 2-3 hours |
| **[Database Guide](documentation/DATABASE_GUIDE.md)**       | PostgreSQL + cloud deployment     | 30-60 min |
| **[Docker Guide](documentation/DOCKER_GUIDE.md)**           | Docker setup & commands           | 15-30 min |
| **[Architecture](documentation/ARCHITECTURE.md)**           | How everything works              | 20-30 min |
| **[Forge Integration](documentation/FORGE_INTEGRATION.md)** | Connecting to Forge engine        | 45-60 min |

**Recommended path:** Getting Started → Learning Guide → Database Guide


---

## 🎯 What's Included

### Core Application
- **REST API** - Card and game management endpoints
- **WebSocket** - Real-time game state updates
- **Session Manager** - Handle multiple concurrent games
- **Database Support** - PostgreSQL with Spring Data JPA
- **Docker Setup** - PostgreSQL, Redis, pgAdmin, Redis Commander

### Interactive Demo
Open http://localhost:8080/test.html after starting the app:
- Create and manage cards via REST API
- Create game sessions
- Connect via WebSocket
- Send game actions in real-time
- Watch state updates instantly

### Example Code Snippets

**Simple REST Endpoint:**
```java
@RestController
@RequestMapping("/api/cards")
public class CardController {
    @GetMapping
    public List<CardDTO> getAllCards() {
        return cardDatabase;
    }
    
    @GetMapping("/{id}")
    public CardDTO getCard(@PathVariable Long id) {
        return cardDatabase.stream()
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
}
```

**WebSocket Handler:**
```java
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        GameAction action = parseAction(message);
        GameSession game = findGame(action.getGameId());
        game.processAction(action);
        broadcastGameState(game);
    }
}
```

**Database Query with Spring Data JPA:**
```java
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    // Spring automatically implements these!
    Optional<Card> findByName(String name);
    Page<Card> findByType(String type, Pageable pageable);
    List<Card> findByColorsContaining(String color);
}
```

---

## 🏗️ Project Structure

```
forge-web-starter/
├── src/main/java/forge/web/starter/
│   ├── controller/          # REST API endpoints
│   ├── service/             # Business logic
│   ├── entity/              # Database entities (JPA)
│   ├── repository/          # Data access (Spring Data)
│   ├── model/               # DTOs for API
│   └── websocket/           # WebSocket handlers
├── src/main/resources/
│   ├── application.yml      # Configuration
│   └── static/              # HTML test clients
├── documentation/           # All guides
├── docker-compose.yml       # Docker services
└── pom.xml                  # Dependencies
```

---

## 💡 Key Concepts

### REST vs WebSocket

**REST** - Request/Response pattern
```
Client: GET /api/cards
Server: [{"id":1,"name":"Lightning Bolt",...}]
```
- Good for: CRUD operations, one-time queries
- One request = one response

**WebSocket** - Persistent connection
```
Client connects → Server can push updates anytime
Client: {"action":"DRAW_CARD"}
Server: {"gameState":{...}} (to all connected clients)
```
- Good for: Real-time updates, game state changes
- Bidirectional communication

### Spring Data JPA Magic

Write this:
```java
interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByNameContainingIgnoreCase(String name);
}
```

Get this SQL automatically:
```sql
SELECT * FROM cards WHERE LOWER(name) LIKE LOWER('%?%')
```

No SQL writing needed! Spring generates queries from method names.

---

## 🔧 Common Tasks

### Add a New REST Endpoint
```java
// In CardController.java
@GetMapping("/rarity/{rarity}")
public List<CardDTO> getCardsByRarity(@PathVariable String rarity) {
    return cardDatabase.stream()
        .filter(c -> c.getRarity().equals(rarity))
        .toList();
}
```

### Add a New Game Action
```java
// In GameSession.java
public void dealDamage(String player, int amount) {
    if ("Player1".equals(player)) {
        player1Life -= amount;
    }
    lastAction = player + " took " + amount + " damage";
}
```

### Query Database with Pagination
```java
// Returns 50 cards at a time
Page<Card> cards = cardRepository.findAll(
    PageRequest.of(0, 50, Sort.by("name"))
);
```

---

## 🐳 Docker Quick Reference

```powershell
# Start databases
docker-compose up -d

# Stop databases
docker-compose down

# View logs
docker-compose logs -f

# Access PostgreSQL
docker exec -it forge-postgres psql -U forge -d forge

# Access Redis
docker exec -it forge-redis redis-cli
```

See **[Docker Guide](documentation/DOCKER_GUIDE.md)** for full details.

---

## 🐛 Troubleshooting

| Problem                 | Solution                                         |
|-------------------------|--------------------------------------------------|
| Port 8080 in use        | Change in `application.yml`: `server.port: 8081` |
| Maven won't download    | Run `mvn clean install -U`                       |
| Database won't connect  | Check Docker: `docker-compose ps`                |
| WebSocket won't connect | Create a game first, then connect                |

See **[Getting Started](documentation/GETTING_STARTED.md)** for more troubleshooting.

---

## 🎓 Learning Path

1. **Week 1:** Run the app, explore the test client, understand REST APIs
2. **Week 2:** Study WebSocket, modify game actions, add new features
3. **Week 3:** Set up database, understand Spring Data JPA, add pagination
4. **Week 4:** Deploy to cloud, optimize performance, plan Forge integration

**Ready to start?** → **[Getting Started Guide](documentation/GETTING_STARTED.md)**

---

## 🔗 Next Steps

- **Learn the basics** - Follow the [Learning Guide](documentation/LEARNING_GUIDE.md)
- **Add a database** - See the [Database Guide](documentation/DATABASE_GUIDE.md)  
- **Plan integration** - Read [Forge Integration](documentation/FORGE_INTEGRATION.md)
- **Deploy to cloud** - Check the [Database Guide](documentation/DATABASE_GUIDE.md#cloud-deployment)

---

## 📄 License

This is a learning project. Use it however you want!

