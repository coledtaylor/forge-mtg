# 🔗 Starter → Forge Integration Mapping

This document shows how concepts you learn in the **Spring Boot Starter** directly map to the **Forge Integration**.

---

## 📊 Side-by-Side Comparison

| Concept               | Starter Project                | Future Forge Integration                    |
|-----------------------|--------------------------------|---------------------------------------------|
| **Game State**        | `GameSession` (simple)         | `Game` (full MTG rules)                     |
| **Player Controller** | Simple action queue            | `PlayerControllerWeb`                       |
| **Game View**         | `GameStateDTO`                 | `GameView` (from Forge)                     |
| **Actions**           | `GameAction` (DRAW, PLAY_CARD) | Full MTG actions (cast spell, attack, etc.) |
| **Session Manager**   | `GameSessionManager`           | Same concept, more robust                   |
| **REST API**          | Card CRUD, Game management     | Deck loading, Game creation                 |
| **WebSocket**         | `GameWebSocketHandler`         | Same, but with richer game state            |

---

## 🎯 Concept Mapping

### 1. Game State Management

**Starter (What you're learning):**
```java
public class GameSession {
    private String gameId;
    private int player1Life = 20;
    private List<CardDTO> player1Hand;
    
    public void drawCard(String player) {
        // Simple: Just add a card
        player1Hand.add(newCard);
    }
    
    public GameStateDTO toDTO() {
        // Convert to JSON-friendly format
        return new GameStateDTO(...);
    }
}
```

**Forge Integration (What you'll build):**
```java
public class ForgeGameSession {
    private String gameId;
    private Game forgeGame;  // Actual Forge game engine!
    private WebGuiGame guiGame;
    
    public void drawCard(PlayerView player) {
        // Let Forge handle the logic
        forgeGame.getAction().invoke(...);
    }
    
    public GameStateDTO toDTO() {
        // Serialize Forge's GameView
        GameView view = guiGame.getGameView();
        return GameViewSerializer.toDTO(view);
    }
}
```

**What Carries Over:**
- ✅ The DTO pattern (converting game state to JSON)
- ✅ Session management concept
- ✅ toDTO() method pattern
- ✅ Game ID tracking

**What Changes:**
- Instead of simple fields, you use Forge's `Game` object
- Instead of simple logic, Forge handles all rules
- State is richer (zones, stack, priority, etc.)

---

### 2. Player Actions

**Starter (What you're learning):**
```java
public class GameAction {
    private String actionType;  // "DRAW", "PLAY_CARD", "PASS_TURN"
    private Long cardId;
}

// In controller:
switch (action.getActionType()) {
    case "DRAW":
        session.drawCard(playerId);
        break;
    case "PLAY_CARD":
        session.playCard(playerId, action.getCardId());
        break;
}
```

**Forge Integration (What you'll build):**
```java
public class ForgeGameAction {
    private String actionType;  // "SELECT_CARD", "SELECT_TARGET", "CONFIRM", etc.
    private String cardId;      // CardView ID
    private List<String> targets;
    private Map<String, String> choices;
}

// In PlayerControllerWeb:
switch (action.getActionType()) {
    case "SELECT_CARD":
        playerController.selectCard(getCardView(action.getCardId()));
        break;
    case "CONFIRM_ABILITY":
        playerController.confirmAction();
        break;
}
```

**What Carries Over:**
- ✅ Action DTO pattern
- ✅ Switch statement for routing
- ✅ ID-based entity references

**What Changes:**
- More action types (20+  instead of 3)
- Richer payloads (multiple targets, choices)
- Two-way communication (server prompts, client responds)

---

### 3. WebSocket Communication

**Starter (What you're learning):**
```java
@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    GameAction action = objectMapper.readValue(payload, GameAction.class);
    
    GameSession gameSession = sessionManager.getGame(action.getGameId());
    processAction(gameSession, action);
    
    // Broadcast updated state
    broadcastGameState(action.getGameId(), gameSession);
}
```

**Forge Integration (What you'll build):**
```java
@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    ForgeGameAction action = objectMapper.readValue(payload, ForgeGameAction.class);
    
    ForgeGameSession gameSession = sessionManager.getGame(action.getGameId());
    
    // Queue action for PlayerControllerWeb
    gameSession.getPlayerController().queueAction(action);
    
    // Wait for game engine to process
    // GameView update comes from GuiGame callbacks
    // Broadcast happens automatically when game state changes
}
```

**What Carries Over:**
- ✅ WebSocket message handling pattern
- ✅ JSON deserialization
- ✅ Session lookup by game ID
- ✅ Broadcast pattern

**What Changes:**
- Actions are queued, not processed immediately
- Game engine drives state updates
- Broadcasting triggered by Forge callbacks

---

### 4. Player Controller Pattern

**Starter (What you're learning):**
```java
// You learn the CONCEPT with simple code
public class GameController {
    @PostMapping("/{gameId}/action")
    public GameStateDTO performAction(@RequestBody GameAction action) {
        GameSession session = sessionManager.getGame(gameId);
        
        // Direct action processing
        session.drawCard(action.getPlayerId());
        
        return session.toDTO();
    }
}
```

**Forge Integration (What you'll build):**
```java
// Apply the PATTERN to Forge
public class PlayerControllerWeb extends PlayerController {
    private BlockingQueue<ForgeGameAction> actionQueue;
    private WebSocketSession webSocketSession;
    
    @Override
    public SpellAbility getAbilityToPlay(Card card, List<SpellAbility> abilities) {
        // Send prompt to frontend
        sendPrompt(new AbilityPrompt(abilities));
        
        // Wait for response via WebSocket
        ForgeGameAction response = actionQueue.poll(30, TimeUnit.SECONDS);
        
        // Return selected ability
        return abilities.get(response.getSelectedIndex());
    }
}
```

**What Carries Over:**
- ✅ Request/Response pattern
- ✅ Session-based state
- ✅ Async communication concept

**What Changes:**
- Extends Forge's `PlayerController` abstract class
- Implements 30+ methods instead of 3
- Uses queues for async request/response

---

## 🛠️ Code Evolution Examples

### Example 1: Simple Card → Forge CardView

**Starter:**
```java
public class CardDTO {
    private Long id;
    private String name;
    private String type;
}
```

**Forge Integration:**
```java
public class CardViewDTO {
    private String id;  // CardView.getId()
    private String name;  // CardView.getName()
    private String type;  // CardView.getType()
    private List<String> colors;  // CardView.getColors()
    private String power;  // CardView.getPower()
    private String toughness;  // CardView.getToughness()
    private boolean tapped;  // CardView.isTapped()
    private List<KeywordDTO> keywords;  // CardView.getKeywords()
    private String zone;  // CardView.getZone()
    // ... 20+ more fields from Forge's CardView
}
```

### Example 2: Simple State → Full GameView

**Starter:**
```java
public class GameStateDTO {
    private List<CardDTO> player1Hand;
    private List<CardDTO> battlefield;
    private int player1Life;
}
```

**Forge Integration:**
```java
public class ForgeGameStateDTO {
    private PlayerViewDTO[] players;
    private List<CardViewDTO> stack;
    private String currentPhase;
    private String currentPlayer;
    private boolean hasPriority;
    private List<ZoneDTO> zones;  // All zones for all players
    private List<String> gameLog;
    private CombatDTO combat;
    private ManaCostDTO manaCost;
    // ... 30+ more fields from GameView
}
```

---

## 📈 Complexity Scaling

| Feature                | Starter              | Forge                                       |
|------------------------|----------------------|---------------------------------------------|
| **Lines of Code**      | ~1,000               | ~5,000-10,000                               |
| **Action Types**       | 3 (DRAW, PLAY, PASS) | 20+ (SELECT, TARGET, ORDER, MULLIGAN, etc.) |
| **DTO Fields**         | 8-10 per DTO         | 50+ per DTO                                 |
| **WebSocket Messages** | 2-3 types            | 10+ types (prompts, updates, errors)        |
| **Game Rules**         | None (simple)        | Full MTG comprehensive rules                |

**But the PATTERNS are the same!** 🎯

---

## 🎓 Learning Progression

### Phase 1: Master the Starter (Now - 2 weeks)
- ✅ Understand Spring Boot basics
- ✅ Build REST endpoints
- ✅ Implement WebSocket
- ✅ Manage sessions
- ✅ Serialize to JSON

### Phase 2: Study Forge Interfaces (Week 3-4)
- Read `IGameController` interface
- Read `IGuiGame` interface  
- Understand `PlayerController` abstract class
- Study `GameView` structure

### Phase 3: Build Bridge (Week 5-8)
- Create `PlayerControllerWeb` extending `PlayerController`
- Create `WebGuiGame` implementing `IGuiGame`
- Serialize `GameView` to JSON
- Test with simple games

### Phase 4: Full Integration (Week 9-16)
- Handle all prompt types
- Implement all player actions
- Build full game UI
- Test complex interactions

---

## 🔄 Pattern Reuse Examples

### Pattern 1: DTO Conversion
**You learned:**
```java
public GameStateDTO toDTO() {
    GameStateDTO dto = new GameStateDTO();
    dto.setGameId(gameId);
    dto.setPlayer1Life(player1Life);
    // ... set all fields
    return dto;
}
```

**You'll apply:**
```java
public GameStateDTO toDTO(GameView view) {
    GameStateDTO dto = new GameStateDTO();
    dto.setPhase(view.getPhase().toString());
    dto.setPlayers(convertPlayers(view.getPlayers()));
    dto.setStack(convertStack(view.getStack()));
    // ... convert all Forge objects
    return dto;
}
```

### Pattern 2: WebSocket Broadcasting
**You learned:**
```java
private void broadcastGameState(String gameId, GameSession session) {
    String json = objectMapper.writeValueAsString(session.toDTO());
    for (WebSocketSession ws : connectedClients) {
        ws.sendMessage(new TextMessage(json));
    }
}
```

**You'll apply:**
```java
private void broadcastGameState(String gameId, GameView view) {
    String json = objectMapper.writeValueAsString(toDTO(view));
    for (WebSocketSession ws : connectedClients) {
        ws.sendMessage(new TextMessage(json));
    }
}
```

**Same code, different objects!**

---

## 💡 Key Insights

1. **The Architecture Stays the Same**
   - REST for CRUD operations
   - WebSocket for real-time game state
   - Session management
   - DTO serialization

2. **The Complexity Increases Gradually**
   - More action types
   - Richer state
   - More edge cases
   - But same patterns!

3. **Forge Does the Heavy Lifting**
   - You don't implement MTG rules
   - You just pass actions to Forge
   - Forge tells you what to display
   - You serialize and send to frontend

4. **Your Job is "Translation"**
   - Frontend action → Forge action
   - Forge state → JSON DTO
   - User input → Game command
   - Game event → UI update

---

## 🎯 Success Criteria

**You'll know you're ready for Forge integration when you can:**

- [ ] Create a REST endpoint without looking at examples
- [ ] Implement WebSocket message handling
- [ ] Explain dependency injection
- [ ] Convert Java objects to JSON DTOs
- [ ] Understand async request/response patterns
- [ ] Debug WebSocket communication
- [ ] Manage session state
- [ ] Handle errors gracefully

**All of these you'll practice in the starter project!**

---

## 🚀 Next Action Items

1. **This Week:** Master the starter project
   - Run it, understand it, modify it
   - Complete exercises 7, 8, 9
   - Build something custom

2. **Next Week:** Study Forge interfaces
   - Read `PlayerController.java` in forge-game
   - Read `IGuiGame.java` in forge-gui
   - Understand `GameView` structure

3. **Week 3:** Plan integration
   - Design your `PlayerControllerWeb`
   - Plan GameView → DTO serialization
   - Sketch API endpoints

4. **Week 4+:** Build it!
   - Start with the simplest game mode
   - Test incrementally
   - Iterate and improve

---

You're not learning toy code - you're learning **production patterns** that scale from this simple starter to a full MTG rules engine integration!

🎓 Master the patterns, not just the code. The patterns stay the same as you scale up!

