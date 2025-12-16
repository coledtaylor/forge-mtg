# 🚀 Quick Start Guide

## Running the Application

### Option 1: Using Maven (Recommended)

1. Open PowerShell and navigate to the project:
   ```powershell
   cd C:\Users\ColeT\Code\java-2D\forge-mtg\forge-web-starter
   ```

2. Run the application:
   ```powershell
   mvn spring-boot:run
   ```

3. Wait for the message: `✅ Forge Web Starter is running!`

4. Open your browser to: http://localhost:8080

### Option 2: Using Your IDE (IntelliJ IDEA)

1. Open the `forge-web-starter` folder in IntelliJ
2. Wait for Maven to sync dependencies
3. Right-click on `ForgeWebStarterApplication.java`
4. Select "Run 'ForgeWebStarterApplication.main()'"
5. Open browser to: http://localhost:8080

## First Steps

### 1. Test the REST API (5 minutes)

Open http://localhost:8080/test.html

Click these buttons in order:
- **Get All Cards** - See the sample card database
- **Create New Game** - Create a game session
- **Get Game State** - View the game state

### 2. Test WebSocket (5 minutes)

After creating a game:
- Click **Connect WebSocket**
- Click **Player1 Draw** - Watch the hand update in real-time!
- Click **Player2 Draw**
- Click cards to **Play** them
- Click **Pass Turn** to switch players

### 3. Read the Code (15 minutes)

Open these files and read the comments:
1. `ForgeWebStarterApplication.java` - Main entry point
2. `CardController.java` - REST API example
3. `GameController.java` - Game management
4. `GameWebSocketHandler.java` - Real-time updates

## Understanding the Flow

### REST API Flow
```
Browser → GET /api/cards → CardController.getAllCards() → Returns JSON
```

### WebSocket Flow
```
Browser → WebSocket message → GameWebSocketHandler → 
  Process action → Update GameSession → 
  Broadcast to all connected clients
```

## Common Issues

### Port 8080 is in use
Change the port in `src/main/resources/application.properties`:
```properties
server.port=8081
```

### Maven dependencies won't download
Try:
```powershell
mvn clean install -U
```

### Application won't start
Check that Java 17 is installed:
```powershell
java -version
```

## What to Learn First

1. **Day 1**: REST APIs
   - Open `CardController.java`
   - Try Exercise 7 in the README
   - Understand `@GetMapping`, `@PostMapping`, `@PathVariable`

2. **Day 2**: WebSocket
   - Open `GameWebSocketHandler.java`
   - Connect via WebSocket in the test client
   - Send actions and watch state updates

3. **Day 3**: Dependency Injection
   - See how `GameController` gets `GameSessionManager`
   - Understand `@Service`, `@Autowired`
   - Try creating your own service

4. **Day 4**: DTOs and Serialization
   - Open `GameStateDTO.java`
   - Add a field and see it appear in JSON
   - Understand how Java objects become JSON

5. **Day 5**: Build Something
   - Complete Challenge 1 (Deck System) from README
   - Or create your own feature!

## Next Steps

Once you're comfortable (1-2 weeks):
- Build a React frontend
- Integrate with Forge game engine
- Deploy to the cloud

## Quick Reference

### Key URLs
- Home: http://localhost:8080
- Test Client: http://localhost:8080/test.html
- Cards API: http://localhost:8080/api/cards
- Game API: http://localhost:8080/api/game/new

### Key Annotations
- `@RestController` - Makes a class handle web requests
- `@GetMapping` - Handle GET requests
- `@PostMapping` - Handle POST requests
- `@Service` - Mark as a service component
- `@Autowired` - Inject dependencies

### Stop the Application
Press `Ctrl+C` in the terminal

---

Have fun learning! 🎮

