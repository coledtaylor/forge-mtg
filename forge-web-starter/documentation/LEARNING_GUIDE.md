# 🎓 Learning Guide

Interactive tutorials and exercises for mastering Spring Boot concepts.

## Overview

This guide teaches you Spring Boot through hands-on exercises. Work through them in order for the best experience.

**Time required:** 2-3 hours  
**Prerequisites:** [Getting Started](GETTING_STARTED.md) completed

---

## Part 1: Understanding REST APIs (45 minutes)

### What is REST?

REST APIs use HTTP methods to perform actions on resources:

| Method        | Purpose         | Example             |
|---------------|-----------------|---------------------|
| **GET**       | Retrieve data   | Get a list of cards |
| **POST**      | Create new data | Create a new card   |
| **PUT/PATCH** | Update data     | Update card details |
| **DELETE**    | Delete data     | Delete a card       |

### Exercise 1: Explore the Card API (15 min)

**Goal:** Understand how REST endpoints work

1. Open **http://localhost:8080/test.html**

2. **Try each button:**
   - **"Get All Cards"** → Calls `GET /api/cards`
   - **"Get Card #1"** → Calls `GET /api/cards/1`
   - **"Search 'Bolt'"** → Calls `GET /api/cards/search?name=bolt`
   - **"Create New Card"** → Calls `POST /api/cards`

3. **Watch the network tab:**
   - Press F12 in your browser
   - Go to "Network" tab
   - Click buttons again
   - See the HTTP requests and responses!

**What's happening:**
```
Browser                          Spring Boot Server
   │                                     │
   ├──── GET /api/cards ────────────────▶│
   │                                     │
   │◀──── JSON: [{card1}, {card2}] ──────┤
```

### Exercise 2: Read Controller Code (15 min)

Open `src/main/java/forge/web/starter/controller/CardController.java`

**Find this method:**
```java
@GetMapping
public List<CardDTO> getAllCards() {
    System.out.println("📋 GET /api/cards - Returning " + cardDatabase.size() + " cards");
    return cardDatabase;
}
```

**Key annotations:**
- `@GetMapping` - Maps HTTP GET requests to this method
- `@RestController` - Tells Spring this class handles web requests
- Return value automatically becomes JSON

**Try this experiment:**

1. Add this new method to `CardController.java`:
```java
@GetMapping("/types/{type}")
public List<CardDTO> getCardsByType(@PathVariable String type) {
    System.out.println("🔍 Searching for type: " + type);
    return cardDatabase.stream()
        .filter(card -> card.getType().equalsIgnoreCase(type))
        .toList();
}
```

2. Save the file (Spring DevTools auto-reloads)

3. Test it in browser:
   - http://localhost:8080/api/cards/types/Instant
   - http://localhost:8080/api/cards/types/Creature

**You just created your first endpoint!** 🎉

### Exercise 3: Test with curl (15 min)

Try these commands in PowerShell:

```powershell
# Get all cards
curl http://localhost:8080/api/cards

# Get specific card
curl http://localhost:8080/api/cards/1

# Search by name
curl "http://localhost:8080/api/cards/search?name=bolt"

# Create a new card (POST with JSON)
curl -X POST http://localhost:8080/api/cards `
  -H "Content-Type: application/json" `
  -d '{\"name\":\"Fireball\",\"type\":\"Sorcery\",\"manaCost\":1,\"power\":0,\"toughness\":0,\"text\":\"Deal damage\"}'
```

**Understanding the response:**

Spring automatically converts Java objects to JSON:
```java
CardDTO card = new CardDTO(1L, "Lightning Bolt", "Instant", ...);
return card;  // Spring converts this to JSON!
```

Becomes:
```json
{
  "id": 1,
  "name": "Lightning Bolt",
  "type": "Instant",
  "manaCost": 1,
  "power": 0,
  "toughness": 0,
  "text": "Deal 3 damage to any target."
}
```

---

## Part 2: Understanding WebSocket (45 minutes)

### What is WebSocket?

Unlike REST (request → response), WebSocket maintains a persistent connection:

**REST:**
```
Client: GET /api/game/123
Server: {"life": 20, ...}
(connection closes)
```

**WebSocket:**
```
Client: (opens connection)
Server: Connected!
Client: {"action": "DRAW_CARD"}
Server: {"life": 20, "hand": [...]} (pushed to ALL connected clients)
(connection stays open)
```

### Exercise 4: Test WebSocket Communication (20 min)

1. **Create a game:**
   - Click **"Create New Game"**
   - Note the Game ID (e.g., `game-abc123`)

2. **Connect WebSocket:**
   - Click **"Connect WebSocket"**
   - See message: "WebSocket connected!"

3. **Send actions:**
   - Click **"Player1 Draw"** - Watch hand update!
   - Click **"Player2 Draw"**
   - Click **"Play"** on a card
   - Click **"Pass Turn"**

4. **Open multiple browser tabs:**
   - Open http://localhost:8080/test.html in 2 tabs
   - Connect both to the same game
   - Draw in one tab
   - **Watch the other tab update automatically!**

**This is the power of WebSocket!**

### Exercise 5: Explore WebSocket Code (15 min)

Open `src/main/java/forge/web/starter/websocket/GameWebSocketHandler.java`

**Find the message handler:**
```java
@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    GameAction action = objectMapper.readValue(payload, GameAction.class);
    processAction(gameSession, action);
    broadcastGameState(action.getGameId(), gameSession);
}
```

**What this does:**
1. Receives JSON message from client
2. Converts JSON to `GameAction` object
3. Processes the action (draw card, play card, etc.)
4. Sends updated game state to ALL connected clients

**Try adding a new action:**

1. Open `GameSession.java` and add:
```java
public void dealDamage(String player, int amount) {
    if ("Player1".equals(player)) {
        player1Life -= amount;
    } else {
        player2Life -= amount;
    }
    lastAction = player + " took " + amount + " damage";
    
    if (player1Life <= 0 || player2Life <= 0) {
        status = "FINISHED";
        lastAction = (player1Life <= 0 ? "Player2" : "Player1") + " wins!";
    }
}
```

2. Add handler in `GameWebSocketHandler.java`:
```java
case "DEAL_DAMAGE":
    gameSession.dealDamage(action.getTarget(), 3);
    break;
```

3. Test by sending WebSocket message:
```javascript
// In browser console (F12)
ws.send(JSON.stringify({
    gameId: "game-abc123",
    action: "DEAL_DAMAGE",
    player: "Player1",
    target: "Player1"
}));
```

### Exercise 6: Browser Console Testing (10 min)

Open browser dev tools (F12) → Console tab

```javascript
// Create WebSocket connection
const ws = new WebSocket('ws://localhost:8080/game');

ws.onopen = () => console.log('Connected!');
ws.onmessage = (event) => console.log('Received:', JSON.parse(event.data));

// Send a draw action
ws.send(JSON.stringify({
    gameId: 'game-abc123',  // Use your actual game ID
    action: 'DRAW_CARD',
    player: 'Player1'
}));
```

Watch the console - you'll see the game state update!

---

## Part 3: Understanding Dependency Injection (30 minutes)

### What is Dependency Injection?

Instead of creating objects with `new`, you let Spring create and inject them.

**Without DI (old way):**
```java
public class GameController {
    private GameSessionManager manager = new GameSessionManager(); // ❌ Tight coupling
}
```

**With DI (Spring way):**
```java
@RestController
public class GameController {
    private final GameSessionManager manager;
    
    public GameController(GameSessionManager manager) {  // ✅ Spring injects this!
        this.manager = manager;
    }
}
```

### Exercise 7: Trace a Request (15 min)

Let's follow a request through the application:

1. **Client sends:** `POST /api/game/new`

2. **Spring routes to:** `GameController.createNewGame()`
   ```java
   @PostMapping("/new")
   public GameStateDTO createNewGame() {
       GameSession session = sessionManager.createGame();  // Uses injected manager
       return session.toDTO();
   }
   ```

3. **GameSessionManager creates game:**
   ```java
   @Service  // Spring creates ONE instance
   public class GameSessionManager {
       public GameSession createGame() {
           GameSession session = new GameSession(generateId());
           activeSessions.put(session.getGameId(), session);
           return session;
       }
   }
   ```

**Key insight:** Spring creates ONE `GameSessionManager` and shares it with all controllers that need it!

### Exercise 8: Add Your Own Service (15 min)

Create a new service to track statistics:

1. Create `src/main/java/forge/web/starter/service/GameStatsService.java`:
```java
package forge.web.starter.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GameStatsService {
    private AtomicInteger gamesCreated = new AtomicInteger(0);
    private AtomicInteger cardsDrawn = new AtomicInteger(0);
    
    public void recordGameCreated() {
        gamesCreated.incrementAndGet();
        System.out.println("📊 Total games created: " + gamesCreated.get());
    }
    
    public void recordCardDrawn() {
        cardsDrawn.incrementAndGet();
    }
    
    public int getGamesCreated() {
        return gamesCreated.get();
    }
    
    public int getCardsDrawn() {
        return cardsDrawn.get();
    }
}
```

2. Inject it into `GameController`:
```java
private final GameStatsService stats;

public GameController(GameSessionManager sessionManager, GameStatsService stats) {
    this.sessionManager = sessionManager;
    this.stats = stats;  // Spring injects this!
}
```

3. Use it:
```java
@PostMapping("/new")
public GameStateDTO createNewGame() {
    stats.recordGameCreated();  // Track creation
    GameSession session = sessionManager.createGame();
    return session.toDTO();
}
```

4. Add endpoint to view stats:
```java
@GetMapping("/stats")
public Map<String, Integer> getStats() {
    return Map.of(
        "gamesCreated", stats.getGamesCreated(),
        "cardsDrawn", stats.getCardsDrawn()
    );
}
```

5. Test: http://localhost:8080/api/game/stats

---

## Part 4: Understanding DTOs (20 minutes)

### What are DTOs?

**Data Transfer Objects** separate internal models from API contracts.

**Example:**
```java
// Internal model (complex)
public class GameSession {
    private Map<String, WebSocketSession> connectedClients;
    private ConcurrentHashMap<String, Object> internalState;
    // ... lots of internal details
}

// DTO (simple, for API)
public class GameStateDTO {
    private String gameId;
    private int player1Life;
    private List<String> player1Hand;
    // ... only what clients need
}
```

### Exercise 9: Modify a DTO (20 min)

Let's add card images to the API:

1. **Update `CardDTO.java`:**
```java
public class CardDTO {
    // ...existing fields...
    private String imageUrl;
    private String rarity;  // Common, Uncommon, Rare, Mythic
    
    // Add getters/setters
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }
}
```

2. **Update sample data in `CardController`:**
```java
CardDTO bolt = new CardDTO(1L, "Lightning Bolt", "Instant", 1, 0, 0, "Deal 3 damage");
bolt.setImageUrl("https://cards.scryfall.io/normal/front/...");
bolt.setRarity("Common");
```

3. **Test:** http://localhost:8080/api/cards/1

You'll now see `imageUrl` and `rarity` in the JSON!

**Benefits:**
- ✅ Control exactly what data is exposed
- ✅ API changes don't affect internal models
- ✅ Easy to version APIs (CardDTOv2)

---

## Challenges

### Challenge 1: Deck System (1-2 hours)

Build a deck management system:

**Requirements:**
1. Create `DeckDTO` class with name and list of cards
2. Create `DeckController` with endpoints:
   - `POST /api/decks` - Create a deck
   - `GET /api/decks` - List all decks
   - `GET /api/decks/{id}` - Get specific deck
   - `POST /api/decks/{id}/cards` - Add card to deck
   - `DELETE /api/decks/{id}/cards/{cardId}` - Remove card
3. Store decks in a `DeckManager` service
4. Validate deck rules (max 4 copies, etc.)

**Hints:**
- Follow the pattern of `CardController` and `GameController`
- Use `@Service` for `DeckManager`
- Use dependency injection
- Return DTOs from endpoints

### Challenge 2: Player Profiles (1-2 hours)

Add player tracking:

**Requirements:**
1. Create `PlayerDTO` with name, wins, losses, games played
2. Track player stats in games
3. Update stats when games finish
4. Add endpoints to get player stats

### Challenge 3: Game History (2-3 hours)

Store and retrieve completed games:

**Requirements:**
1. When game status becomes "FINISHED", archive it
2. Create `GameHistoryService`
3. Add endpoints to browse history
4. Add filtering by player, date, etc.

---

## Key Concepts Summary

### Annotations Reference

| Annotation                | Purpose                  | Used On            |
|---------------------------|--------------------------|--------------------|
| `@SpringBootApplication`  | Main entry point         | Main class         |
| `@RestController`         | REST API controller      | Controller classes |
| `@Service`                | Business logic component | Service classes    |
| `@Component`              | Generic Spring bean      | Any class          |
| `@Configuration`          | Configuration class      | Config classes     |
| `@GetMapping("/path")`    | Handle GET requests      | Methods            |
| `@PostMapping("/path")`   | Handle POST requests     | Methods            |
| `@PutMapping("/path")`    | Handle PUT requests      | Methods            |
| `@DeleteMapping("/path")` | Handle DELETE requests   | Methods            |
| `@PathVariable`           | Extract URL parameter    | Method parameters  |
| `@RequestParam`           | Extract query parameter  | Method parameters  |
| `@RequestBody`            | Parse JSON body          | Method parameters  |
| `@Autowired`              | Inject dependency        | Constructors       |

### Common Patterns

**Controller Pattern:**
```java
@RestController
@RequestMapping("/api/resource")
public class ResourceController {
    private final ResourceService service;
    
    public ResourceController(ResourceService service) {
        this.service = service;
    }
    
    @GetMapping
    public List<ResourceDTO> getAll() {
        return service.findAll();
    }
    
    @GetMapping("/{id}")
    public ResourceDTO getOne(@PathVariable Long id) {
        return service.findById(id);
    }
    
    @PostMapping
    public ResourceDTO create(@RequestBody ResourceDTO resource) {
        return service.create(resource);
    }
}
```

**Service Pattern:**
```java
@Service
public class ResourceService {
    private Map<Long, Resource> store = new ConcurrentHashMap<>();
    
    public List<ResourceDTO> findAll() {
        return store.values().stream()
            .map(Resource::toDTO)
            .toList();
    }
}
```

---

## What's Next?

You've learned the fundamentals! Now you can:

1. **Add a database** → [Database Guide](DATABASE_GUIDE.md)
2. **Deploy with Docker** → [Docker Guide](DOCKER_GUIDE.md)
3. **Plan Forge integration** → [Forge Integration](FORGE_INTEGRATION.md)
4. **Understand the architecture** → [Architecture](ARCHITECTURE.md)

---

## Tips for Success

1. **Type the code yourself** - Don't copy/paste, you'll learn more
2. **Read error messages** - They tell you what's wrong
3. **Use the debugger** - Set breakpoints and step through code
4. **Check logs** - Console output shows what's happening
5. **Experiment** - Break things intentionally to see what happens
6. **Ask "why?"** - Understand the reasoning behind patterns

**Keep going - you're building real skills!** 🚀

