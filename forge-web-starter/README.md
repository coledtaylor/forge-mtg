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

## 🏗️ Project Structure

```
forge-web-starter/
├── src/main/java/forge/web/starter/
│   ├── ForgeWebStarterApplication.java    # Main entry point
│   ├── config/
│   │   └── WebSocketConfig.java           # WebSocket configuration
│   ├── controller/
│   │   ├── CardController.java            # REST API for cards
│   │   └── GameController.java            # REST API for games
│   ├── model/
│   │   ├── CardDTO.java                   # Card data transfer object
│   │   ├── GameStateDTO.java              # Game state data
│   │   └── GameAction.java                # Player actions
│   ├── service/
│   │   ├── GameSession.java               # Single game session
│   │   └── GameSessionManager.java        # Manages all games
│   └── websocket/
│       └── GameWebSocketHandler.java      # WebSocket message handler
├── src/main/resources/
│   ├── application.properties             # Configuration
│   └── static/
│       ├── index.html                     # Landing page
│       └── test.html                      # Interactive test client
└── pom.xml                                # Maven dependencies
```

## 🚀 Getting Started

### Prerequisites

- **Java 17** (already installed for Forge)
- **Maven** (already available in your Forge project)
- A web browser

### Running the Application

1. **Navigate to the project directory:**
   ```powershell
   cd C:\Users\ColeT\Code\java-2D\forge-mtg\forge-web-starter
   ```

2. **Run with Maven:**
   ```powershell
   mvn spring-boot:run
   ```

3. **Open your browser:**
   - Main page: http://localhost:8080
   - Test client: http://localhost:8080/test.html

### First Time Setup

Maven will download all dependencies on first run. This may take a few minutes.

## 🎯 Interactive Tutorial

### Part 1: Understanding REST APIs

REST APIs use HTTP methods to perform actions:
- **GET** - Retrieve data
- **POST** - Create new data
- **PUT/PATCH** - Update data
- **DELETE** - Delete data

#### Exercise 1: Test the Card API

1. Open the test client: http://localhost:8080/test.html
2. Click **"Get All Cards"** - This calls `GET /api/cards`
3. Click **"Get Card #1"** - This calls `GET /api/cards/1`
4. Click **"Search 'Bolt'"** - This calls `GET /api/cards/search?name=bolt`
5. Click **"Create New Card"** - This calls `POST /api/cards`

**What's happening?**
- Your browser sends HTTP requests to the Spring Boot server
- Spring routes the request to the correct controller method
- The controller returns data (as Java objects)
- Spring automatically converts it to JSON
- The browser receives and displays the JSON

#### Exercise 2: Explore the Code

Open `CardController.java` and find the `getAllCards()` method:

```java
@GetMapping
public List<CardDTO> getAllCards() {
    return cardDatabase;
}
```

**Key concepts:**
- `@GetMapping` - Maps HTTP GET requests to this method
- `@RestController` - Tells Spring this class handles web requests
- Return value automatically becomes JSON

**Try this:** Add a new endpoint that returns only creature cards!

### Part 2: Understanding WebSocket

WebSocket provides **real-time, bidirectional** communication. Unlike REST (request/response), WebSocket keeps a persistent connection open so the server can push updates to clients.

#### Exercise 3: Test WebSocket Communication

1. In the test client, click **"Create New Game"**
2. Note the Game ID that appears
3. Click **"Connect WebSocket"**
4. Watch the message log - you'll see the connection established
5. Click **"Player1 Draw"** - Watch the game state update in real-time!
6. Click **"Player2 Draw"** and **"Pass Turn"**

**What's happening?**
- Browser opens a WebSocket connection to the server
- When you click "Draw", JavaScript sends a JSON message through the WebSocket
- Server receives it, processes the action, and broadcasts the updated game state
- Browser receives the update and refreshes the UI
- All without page refresh!

#### Exercise 4: Explore WebSocket Code

Open `GameWebSocketHandler.java` and find the `handleTextMessage()` method:

```java
@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    GameAction action = objectMapper.readValue(payload, GameAction.class);
    processAction(gameSession, action);
    broadcastGameState(action.getGameId(), gameSession);
}
```

**Key concepts:**
- `WebSocketSession` - Represents one connected client
- `TextMessage` - JSON message from the client
- `broadcastGameState()` - Sends updates to all connected clients

### Part 3: Understanding Dependency Injection

Spring uses **dependency injection** to manage objects. Instead of creating objects with `new`, you let Spring create and inject them.

#### Exercise 5: Trace a Request

1. Look at `GameController.java`:
   ```java
   public GameController(GameSessionManager sessionManager) {
       this.sessionManager = sessionManager;
   }
   ```

2. Spring automatically:
   - Creates a `GameSessionManager` instance (because of `@Service`)
   - Injects it into `GameController` (because of `@Autowired`)
   - You never write `new GameSessionManager()`!

**Benefits:**
- Easy to test (inject mock objects)
- Loose coupling (classes don't create their dependencies)
- Spring manages lifecycle

### Part 4: Understanding DTOs

**Data Transfer Objects** (DTOs) are simple classes that hold data for transfer between layers.

#### Exercise 6: Modify a DTO

Open `CardDTO.java` and add a new field:

```java
private String imageUrl;
```

Save the file, and Spring DevTools will auto-reload the application. Now when you create cards, you can include image URLs!

**Why DTOs?**
- Separate internal models from API contracts
- Control what data is exposed to clients
- Easy to serialize to JSON

## 🛠️ Hands-On Exercises

### Exercise 7: Add a New Endpoint

Add this method to `CardController.java`:

```java
@GetMapping("/types/{type}")
public List<CardDTO> getCardsByType(@PathVariable String type) {
    return cardDatabase.stream()
        .filter(card -> card.getType().equalsIgnoreCase(type))
        .toList();
}
```

Test it: http://localhost:8080/api/cards/types/Instant

### Exercise 8: Add Game Statistics

Add this to `GameSession.java`:

```java
private int turnCount = 0;

public void passTurn() {
    turnCount++;
    currentPlayer = "Player1".equals(currentPlayer) ? "Player2" : "Player1";
    currentPhase = "Draw";
    lastAction = currentPlayer + "'s turn (Turn " + turnCount + ")";
}
```

Add `turnCount` to `GameStateDTO.java` and watch turns increment!

### Exercise 9: Add a Deal Damage Action

1. Add to `GameSession.java`:
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
       }
   }
   ```

2. Add a new action type to `GameWebSocketHandler.java`:
   ```java
   case "DEAL_DAMAGE":
       gameSession.dealDamage(action.getTarget(), 3);
       break;
   ```

3. Test it through the WebSocket!

## 📖 Spring Boot Concepts Reference

### Annotations You'll Use Often

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@SpringBootApplication` | Main entry point | On main class |
| `@RestController` | REST API controller | On controller classes |
| `@Service` | Service layer component | On service classes |
| `@Configuration` | Configuration class | On config classes |
| `@GetMapping` | Handle GET requests | On methods |
| `@PostMapping` | Handle POST requests | On methods |
| `@PathVariable` | Extract URL parameters | `@PathVariable String id` |
| `@RequestParam` | Extract query parameters | `@RequestParam String name` |
| `@RequestBody` | Parse JSON body | `@RequestBody CardDTO card` |
| `@Autowired` | Inject dependencies | On constructors |

### Common Patterns

#### Controller Pattern
```java
@RestController
@RequestMapping("/api/resource")
public class ResourceController {
    private final ResourceService service;
    
    @Autowired
    public ResourceController(ResourceService service) {
        this.service = service;
    }
    
    @GetMapping
    public List<Resource> getAll() {
        return service.findAll();
    }
}
```

#### Service Pattern
```java
@Service
public class ResourceService {
    public List<Resource> findAll() {
        // Business logic here
    }
}
```

## 🔍 Troubleshooting

### Port 8080 Already in Use
```powershell
# Find and kill the process using port 8080
netstat -ano | findstr :8080
taskkill /PID <process_id> /F
```

Or change the port in `application.properties`:
```properties
server.port=8081
```

### Application Won't Start
1. Make sure Java 17 is installed: `java -version`
2. Clean and rebuild: `mvn clean install`
3. Check console for error messages

### WebSocket Won't Connect
1. Make sure you created a game first
2. Check browser console for errors (F12)
3. Ensure the server is running

## 🎓 Next Steps

Once you're comfortable with these concepts, you're ready to:

1. **Integrate with Forge Game Engine**
   - Create a `forge-web-api` module
   - Implement `PlayerControllerWeb` using these patterns
   - Serialize `GameView` objects to JSON

2. **Build a React Frontend**
   - Use `fetch()` or `axios` for REST calls
   - Use WebSocket API for real-time updates
   - Build game UI components

3. **Add Advanced Features**
   - User authentication (Spring Security)
   - Database persistence (Spring Data JPA)
   - Caching (Spring Cache)
   - Testing (Spring Boot Test)

## 📝 Learning Challenges

### Challenge 1: Deck System
Create a `DeckController` that allows creating and managing decks:
- `POST /api/decks` - Create a deck
- `GET /api/decks` - List all decks
- `POST /api/decks/{id}/cards` - Add a card to a deck
- `DELETE /api/decks/{id}/cards/{cardId}` - Remove a card

### Challenge 2: Player Profiles
Add player profile support:
- Create `PlayerDTO` with name, wins, losses
- Store in `GameSession`
- Update stats when games finish
- Display in the UI

### Challenge 3: Game History
Track game history:
- Store completed games
- Add endpoint to retrieve past games
- Display win/loss records

## 🔗 Useful Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Boot Guides](https://spring.io/guides)
- [Baeldung Spring Tutorials](https://www.baeldung.com/spring-boot)
- [WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)

## 💡 Tips for Learning

1. **Read the code comments** - Every file has detailed explanations
2. **Experiment** - Break things and fix them
3. **Use the test client** - See how everything connects
4. **Check the console** - Lots of helpful log messages
5. **Compare patterns** - Notice similarities between controllers

## 🎯 When You're Ready for Forge Integration

You'll know you're ready when you can:
- ✅ Create REST endpoints without looking at examples
- ✅ Understand how dependency injection works
- ✅ Send and receive WebSocket messages
- ✅ Serialize Java objects to JSON
- ✅ Manage session state

The patterns you're learning here will directly apply to integrating the Forge game engine!

---

**Questions?** The code is heavily commented. Read through each file to understand how it works, then start experimenting!

Good luck, and have fun learning Spring Boot! 🚀

