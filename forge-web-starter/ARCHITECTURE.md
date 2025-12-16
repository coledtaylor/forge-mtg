# 🏗️ Architecture Diagram

## How the Spring Boot Starter Works

```
┌─────────────────────────────────────────────────────────────────┐
│                        YOUR BROWSER                             │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                   test.html                               │  │
│  │  - JavaScript code                                        │  │
│  │  - Buttons to trigger actions                            │  │
│  │  - Display game state                                    │  │
│  └────────┬──────────────────────────────────┬──────────────┘  │
│           │                                  │                 │
│           │ HTTP REST                        │ WebSocket      │
│           │ (Request/Response)               │ (Real-time)    │
└───────────┼──────────────────────────────────┼─────────────────┘
            │                                  │
            ▼                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│              SPRING BOOT APPLICATION                            │
│              (Running on http://localhost:8080)                 │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              CONTROLLERS (Handle HTTP)                   │  │
│  │                                                           │  │
│  │  CardController.java                                     │  │
│  │  ├─ GET /api/cards         → getAllCards()              │  │
│  │  ├─ GET /api/cards/{id}    → getCard(id)                │  │
│  │  ├─ POST /api/cards        → createCard(card)           │  │
│  │  └─ DELETE /api/cards/{id} → deleteCard(id)             │  │
│  │                                                           │  │
│  │  GameController.java                                     │  │
│  │  ├─ POST /api/game/new       → createNewGame()          │  │
│  │  ├─ GET /api/game/{id}       → getGameState(id)         │  │
│  │  ├─ POST /api/game/{id}/action → performAction(...)     │  │
│  │  └─ DELETE /api/game/{id}    → deleteGame(id)           │  │
│  │                                                           │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                        │
│                       │ Uses                                   │
│                       ▼                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │        SERVICES (Business Logic)                         │  │
│  │                                                           │  │
│  │  GameSessionManager.java                                 │  │
│  │  ├─ createGame()         → Creates new GameSession      │  │
│  │  ├─ getGame(id)          → Retrieves GameSession        │  │
│  │  ├─ deleteGame(id)       → Removes GameSession          │  │
│  │  └─ activeSessions       → Map of all active games      │  │
│  │                                                           │  │
│  │  GameSession.java                                        │  │
│  │  ├─ gameId, status, currentPlayer                       │  │
│  │  ├─ player1Hand, player2Hand                            │  │
│  │  ├─ battlefield                                          │  │
│  │  ├─ drawCard(player)                                    │  │
│  │  ├─ playCard(player, cardId)                            │  │
│  │  ├─ passTurn()                                          │  │
│  │  └─ toDTO()              → Convert to JSON format       │  │
│  │                                                           │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                        │
│                       │ Returns                                │
│                       ▼                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              MODELS (Data Transfer Objects)              │  │
│  │                                                           │  │
│  │  CardDTO.java                                            │  │
│  │  ├─ id, name, type                                       │  │
│  │  ├─ manaCost, power, toughness                          │  │
│  │  └─ text                                                 │  │
│  │                                                           │  │
│  │  GameStateDTO.java                                       │  │
│  │  ├─ gameId, status, currentPlayer                       │  │
│  │  ├─ player1Life, player2Life                            │  │
│  │  ├─ player1Hand, player2Hand                            │  │
│  │  ├─ battlefield                                          │  │
│  │  └─ lastAction                                           │  │
│  │                                                           │  │
│  │  GameAction.java                                         │  │
│  │  ├─ gameId, playerId                                    │  │
│  │  ├─ actionType (DRAW, PLAY_CARD, PASS_TURN)            │  │
│  │  └─ cardId, target                                       │  │
│  │                                                           │  │
│  └─────────────────────────┬────────────────────────────────┘  │
│                            │                                   │
│                            │ Serialized to JSON                │
│                            ▼                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │           WEBSOCKET HANDLER (Real-time)                  │  │
│  │                                                           │  │
│  │  GameWebSocketHandler.java                               │  │
│  │  ├─ afterConnectionEstablished()                        │  │
│  │  ├─ handleTextMessage(message)                          │  │
│  │  │   ├─ Parse GameAction from JSON                      │  │
│  │  │   ├─ Process action on GameSession                   │  │
│  │  │   └─ Broadcast updated GameStateDTO                  │  │
│  │  └─ afterConnectionClosed()                             │  │
│  │                                                           │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 🔄 Request Flow Examples

### Example 1: Getting All Cards (REST)
```
Browser                Spring Boot
   │                        │
   │  GET /api/cards        │
   ├───────────────────────>│
   │                        │
   │                        │ CardController.getAllCards()
   │                        │      ↓
   │                        │ Return List<CardDTO>
   │                        │      ↓
   │                        │ Spring converts to JSON
   │                        │
   │  JSON Response         │
   │<───────────────────────┤
   │  [{id:1, name:"Lightning Bolt",...}]
   │                        │
```

### Example 2: Creating a Game (REST)
```
Browser                Spring Boot
   │                        │
   │  POST /api/game/new    │
   ├───────────────────────>│
   │                        │
   │                        │ GameController.createNewGame()
   │                        │      ↓
   │                        │ sessionManager.createGame()
   │                        │      ↓
   │                        │ new GameSession(id)
   │                        │      ↓
   │                        │ session.toDTO()
   │                        │      ↓
   │                        │ Return GameStateDTO
   │                        │      ↓
   │                        │ Spring converts to JSON
   │                        │
   │  JSON Response         │
   │<───────────────────────┤
   │  {gameId:"abc123", status:"IN_PROGRESS",...}
   │                        │
```

### Example 3: Drawing a Card (WebSocket)
```
Browser                Spring Boot
   │                        │
   │  WebSocket OPEN        │
   ├═══════════════════════>│
   │                        │ GameWebSocketHandler
   │                        │   .afterConnectionEstablished()
   │                        │
   │  Send JSON:            │
   │  {gameId:"abc123",     │
   │   actionType:"DRAW",   │
   │   playerId:"Player1"}  │
   ├───────────────────────>│
   │                        │
   │                        │ handleTextMessage()
   │                        │      ↓
   │                        │ Parse GameAction
   │                        │      ↓
   │                        │ session.drawCard("Player1")
   │                        │      ↓
   │                        │ session.toDTO()
   │                        │      ↓
   │                        │ broadcastGameState()
   │                        │      ↓
   │                        │ Send to ALL connected clients
   │                        │
   │  Receive JSON:         │
   │<───────────────────────┤
   │  {gameId:"abc123",     │
   │   player1Hand:[...5 cards],
   │   lastAction:"Player1 drew a card"}
   │                        │
```

## 🎯 Key Concepts

### Dependency Injection
```
GameController
    ↓ (Spring injects)
GameSessionManager
    ↓ (manages)
GameSession (multiple instances)
```

Spring automatically creates ONE instance of `GameSessionManager` and injects it into `GameController`. You never write `new GameSessionManager()`.

### REST vs WebSocket

**REST (HTTP)**
- Request/Response pattern
- Client asks, server responds
- Good for: Get data, submit forms, CRUD operations
- Example: "Give me all cards" → Server sends cards

**WebSocket**
- Persistent connection
- Bidirectional communication
- Server can push updates
- Good for: Real-time games, chat, live updates
- Example: Player 1 draws → Server pushes update to Player 2

### JSON Serialization

Java Object → JSON (automatically by Spring)
```java
CardDTO card = new CardDTO(1L, "Lightning Bolt", "Instant", 1, 0, 0, "Deal 3 damage");
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
  "text": "Deal 3 damage"
}
```

## 🔍 Where to Find Each Concept

| Concept | File | Lines to Read |
|---------|------|---------------|
| **REST endpoints** | `CardController.java` | All methods |
| **WebSocket setup** | `WebSocketConfig.java` | Entire file |
| **WebSocket handling** | `GameWebSocketHandler.java` | `handleTextMessage()` |
| **State management** | `GameSession.java` | `drawCard()`, `playCard()` |
| **Dependency injection** | `GameController.java` | Constructor |
| **JSON models** | `GameStateDTO.java` | All fields |

---

This architecture is **exactly** what you'll use for the Forge integration, just with:
- `GameSession` → Real Forge `Game` object
- `GameStateDTO` → Serialized `GameView`
- Simple actions → Full MTG rules engine

🎓 Master this, and you'll be ready for the real thing!

