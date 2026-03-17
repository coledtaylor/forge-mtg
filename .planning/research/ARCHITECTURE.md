# Architecture Research

**Domain:** Game client wrapping Java engine via WebSocket (MTG card game)
**Researched:** 2026-03-16
**Confidence:** HIGH

## System Overview

```
Browser (React/TypeScript)
===========================
  +-----------+  +------------+  +----------+  +-----------+
  | Game      |  | Deck       |  | Lobby    |  | Card      |
  | Board     |  | Builder    |  | Screen   |  | Browser   |
  +-----+-----+  +------+-----+  +----+-----+  +-----+-----+
        |               |             |              |
  +-----+---------------+-------------+--------------+------+
  |                 WebSocket Layer (useGameSocket)          |
  |              + REST Client (api.ts)                      |
  +--------------------------+------------------------------+
                             |
                      HTTP / WebSocket
                             |
  +--------------------------+------------------------------+
  |              forge-gui-web (Java)                        |
  |  +-------------+  +--------------+  +----------------+  |
  |  | REST        |  | WebSocket    |  | WebGuiGame     |  |
  |  | Endpoints   |  | Handler      |  | (IGuiGame)     |  |
  |  | (cards,     |  | (session     |  |                |  |
  |  |  decks)     |  |  mgmt)       |  | WebGuiBase     |  |
  |  +------+------+  +------+-------+  | (IGuiBase)     |  |
  |         |                |           +-------+--------+  |
  +---------+----------------+-----------+-------+-----------+
                             |                   |
  +--------------------------+-------------------+-----------+
  |              forge-gui (Shared)                          |
  |  +--------------+  +------------------+  +------------+  |
  |  | HostedMatch  |  | PlayerController |  | AbstractGui|  |
  |  |              |  | Human            |  | Game       |  |
  |  +--------------+  +------------------+  +------------+  |
  +--------------------------+-------------------------------+
                             |
  +--------------------------+-------------------------------+
  |              forge-game / forge-core / forge-ai           |
  |  +---------+  +----------+  +--------+  +-------------+  |
  |  | Game    |  | Card     |  | AI     |  | StaticData  |  |
  |  | Engine  |  | Database |  | Engine |  | (cards/sets)|  |
  |  +---------+  +----------+  +--------+  +-------------+  |
  +----------------------------------------------------------+
```

### Component Responsibilities

| Component | Responsibility | Boundary |
|-----------|----------------|----------|
| **Game Board (React)** | Renders battlefield, hand, stack, graveyard, exile zones; handles player input (cast, attack, block) | Receives `GameView` state, sends player actions |
| **Deck Builder (React)** | Card search, deck list editing, format validation display | REST-only, no WebSocket needed |
| **Lobby Screen (React)** | Format selection, deck choice, AI difficulty, match start | REST for deck list, POST to start match |
| **Card Browser (React)** | Visual card search with filters, Scryfall image display | REST-only |
| **WebSocket Layer (React)** | Single persistent connection per game session; message routing, reconnection | Owns connection lifecycle; distributes messages to subscribers |
| **REST Client (React)** | Stateless HTTP calls for cards, decks, formats | Thin wrapper around fetch |
| **REST Endpoints (Java)** | Card search, deck CRUD, format listing, match creation | Stateless; reads from StaticData and filesystem |
| **WebSocket Handler (Java)** | Session management, message routing, JSON serialization | One session per active game; maps WebSocket frames to engine calls |
| **WebGuiGame (Java)** | Implements `IGuiGame` -- serializes engine callbacks to JSON WebSocket messages | Translates 60+ GUI methods into JSON message types |
| **WebGuiBase (Java)** | Implements `IGuiBase` -- mostly no-ops for web context | Minimal; provides `getNewGuiGame()`, `hostMatch()` |
| **HostedMatch (Java, existing)** | Orchestrates match lifecycle | Unchanged; receives `WebGuiGame` as its `IGuiGame` instance |
| **PlayerControllerHuman (Java, existing)** | Bridges player decisions to engine | Unchanged; blocks on `CompletableFuture` that WebSocket handler completes |

## Recommended Project Structure

### Backend (forge-gui-web)

```
forge-gui-web/
├── pom.xml                         # Maven module config
├── src/main/java/forge/web/
│   ├── WebServer.java              # Entry point: starts embedded server
│   ├── WebGuiGame.java             # IGuiGame implementation (engine -> JSON)
│   ├── WebGuiBase.java             # IGuiBase implementation
│   ├── WebPlayerInput.java         # Blocking queue for player decisions
│   ├── api/
│   │   ├── CardEndpoint.java       # GET /api/cards, /api/cards/:id
│   │   ├── DeckEndpoint.java       # CRUD /api/decks
│   │   ├── FormatEndpoint.java     # GET /api/formats
│   │   └── MatchEndpoint.java      # POST /api/match/start
│   ├── websocket/
│   │   ├── GameWebSocket.java      # WebSocket endpoint handler
│   │   ├── MessageRouter.java      # Routes incoming messages to handlers
│   │   └── SessionManager.java     # Maps WebSocket sessions to games
│   └── protocol/
│       ├── MessageType.java        # Enum of all message types
│       ├── InboundMessage.java     # Player action messages (client -> server)
│       └── OutboundMessage.java    # Game state messages (server -> client)
└── frontend/                       # React app (Vite project)
    ├── package.json
    ├── vite.config.ts
    ├── tsconfig.json
    └── src/
        ├── main.tsx                # App entry point
        ├── App.tsx                 # Router setup
        ├── api/
        │   ├── rest.ts             # REST client (cards, decks, formats)
        │   └── types.ts           # Shared API types
        ├── socket/
        │   ├── useGameSocket.ts    # WebSocket hook (connection + message handling)
        │   ├── protocol.ts        # Message type definitions (mirrors Java enum)
        │   └── reconnect.ts       # Reconnection logic with backoff
        ├── stores/
        │   ├── gameStore.ts        # Zustand: game state (GameView, prompt, phases)
        │   ├── deckStore.ts        # Zustand: deck builder state
        │   └── lobbyStore.ts       # Zustand: lobby/settings state
        ├── components/
        │   ├── game/
        │   │   ├── GameBoard.tsx   # Main game layout
        │   │   ├── Battlefield.tsx # Permanent zone
        │   │   ├── Hand.tsx        # Player hand
        │   │   ├── Stack.tsx       # Spell stack
        │   │   ├── Graveyard.tsx   # Graveyard zone overlay
        │   │   ├── Exile.tsx       # Exile zone overlay
        │   │   ├── PlayerPanel.tsx # Life, mana pool, avatar
        │   │   ├── PhaseBar.tsx    # Turn phase indicator
        │   │   ├── PromptBar.tsx   # Engine prompt + OK/Cancel buttons
        │   │   ├── GameLog.tsx     # Scrolling game log
        │   │   └── CardDetail.tsx  # Enlarged card view on hover
        │   ├── deck/
        │   │   ├── DeckEditor.tsx  # Deck list with add/remove
        │   │   ├── CardSearch.tsx  # Search + filter panel
        │   │   └── DeckList.tsx    # Saved deck listing
        │   ├── lobby/
        │   │   ├── LobbyScreen.tsx # Format + deck + AI selection
        │   │   └── FormatPicker.tsx
        │   └── shared/
        │       ├── Card.tsx        # Single card rendering (Scryfall image)
        │       ├── ManaSymbol.tsx  # Mana symbol rendering
        │       └── Layout.tsx      # App shell / navigation
        ├── hooks/
        │   ├── useCardImages.ts    # Scryfall image URL generation
        │   └── usePrompt.ts       # Manages engine prompt/response cycle
        └── pages/
            ├── HomePage.tsx
            ├── DeckBuilderPage.tsx
            └── GamePage.tsx
```

### Structure Rationale

- **`socket/` separated from `stores/`:** The WebSocket layer is a transport concern, not state. The socket hook pushes data into Zustand stores; components subscribe to stores, never to the socket directly.
- **`stores/` with Zustand:** Each major domain (game, deck, lobby) gets its own store. Zustand's selector-based subscriptions prevent unnecessary re-renders when only one zone changes on a board with hundreds of cards.
- **`protocol.ts` mirrors Java `MessageType.java`:** Keeps client and server message types in sync. A shared enum-like structure reduces deserialization bugs.
- **`components/game/` is the heaviest folder:** The game board is the most complex view. Each zone is its own component to enable independent rendering and targeted state updates.

## Architectural Patterns

### Pattern 1: Request-Response Over WebSocket (Engine Input Bridge)

**What:** The Java game engine blocks on player input (via `PlayerControllerHuman`). The web implementation uses a `CompletableFuture` that the WebSocket handler completes when the client sends a response.

**When to use:** Every time the engine asks the player for a decision (choose target, declare attackers, pay mana, confirm trigger).

**Trade-offs:** Simple conceptual model (engine blocks, client responds). But requires careful timeout handling -- if the client disconnects mid-prompt, the engine thread hangs forever without a timeout.

**Example (Java side):**
```java
// WebGuiGame.java
public <T> List<T> getChoices(String message, int min, int max, List<T> choices) {
    String requestId = UUID.randomUUID().toString();
    OutboundMessage msg = new OutboundMessage(MessageType.CHOOSE, requestId);
    msg.put("message", message);
    msg.put("min", min);
    msg.put("max", max);
    msg.put("choices", serialize(choices));
    session.send(msg.toJson());

    // Block until client responds (with timeout)
    CompletableFuture<List<Integer>> future = new CompletableFuture<>();
    pendingInputs.put(requestId, future);
    List<Integer> indices = future.get(5, TimeUnit.MINUTES);
    return indices.stream().map(choices::get).collect(Collectors.toList());
}
```

**Example (React side):**
```typescript
// usePrompt.ts
function handleChooseMessage(msg: ChooseMessage) {
  gameStore.setState({
    prompt: {
      requestId: msg.requestId,
      message: msg.message,
      choices: msg.choices,
      min: msg.min,
      max: msg.max,
    },
  });
}

function submitChoice(requestId: string, selectedIndices: number[]) {
  socket.send(JSON.stringify({
    type: 'CHOOSE_RESPONSE',
    requestId,
    indices: selectedIndices,
  }));
  gameStore.setState({ prompt: null });
}
```

### Pattern 2: Unidirectional State Flow (Engine -> Store -> Component)

**What:** Game state always flows from engine to WebSocket to Zustand store to React component. Player actions flow the reverse direction. Components never mutate game state directly.

**When to use:** All game state updates.

**Trade-offs:** Predictable, debuggable. Adds one extra layer (store) compared to putting state directly in component, but enables cross-component state sharing (e.g., PromptBar needs to know what cards are selectable on Battlefield).

```
Engine -> WebGuiGame -> WebSocket -> useGameSocket -> gameStore -> Components
Components -> onClick -> usePrompt -> WebSocket -> MessageRouter -> PlayerControllerHuman -> Engine
```

### Pattern 3: Optimistic Local State for Non-Game UI

**What:** For deck builder and lobby, update local Zustand state immediately on user action, then sync with server via REST. Revert on error.

**When to use:** Deck editing, format selection, lobby settings -- any non-game interaction.

**Trade-offs:** Feels instant to the user. Slight complexity in error rollback, but deck CRUD is simple enough that conflicts are rare (single user, local only).

### Pattern 4: Zone-Based Component Decomposition

**What:** Each MTG zone (hand, battlefield, graveyard, exile, stack, library, command zone) is its own React component that subscribes only to its own zone data from the store.

**When to use:** Game board rendering.

**Trade-offs:** Excellent performance -- when a card moves from hand to battlefield, only Hand and Battlefield re-render, not the entire board. Requires careful store slicing with Zustand selectors.

```typescript
// Only re-renders when hand cards change
const handCards = useGameStore((s) => s.zones.hand);
```

## Data Flow

### Game Session Lifecycle

```
1. LOBBY PHASE (REST)
   Browser                          Server
   ──────                          ──────
   GET /api/formats          -->   Return format list
   GET /api/decks            -->   Return saved decks
   POST /api/match/start     -->   Create HostedMatch, return WebSocket URL
     { format, deckId, aiDiff }

2. GAME INIT (WebSocket)
   Browser                          Server
   ──────                          ──────
   Connect WS to /ws/game/:id -->  GameWebSocket.onConnect()
                              <--  GAME_STATE (full GameView JSON)
                              <--  PROMPT (mulligan decision)

3. GAME LOOP (WebSocket, repeating)
   Browser                          Server
   ──────                          ──────
                              <--  PHASE_UPDATE { phase: "main1" }
                              <--  PROMPT { type: "CHOOSE", choices: [...] }
   CHOOSE_RESPONSE { indices } -->  Unblocks PlayerControllerHuman
                              <--  ZONE_UPDATE { hand: [...], battlefield: [...] }
                              <--  COMBAT { attackers: [...], blockers: [...] }
                              <--  STACK_UPDATE { stack: [...] }
                              <--  LIFE_UPDATE { players: [...] }

4. GAME END (WebSocket)
                              <--  GAME_OVER { winner: "Player 1", ... }
   WS disconnect              -->  Cleanup game session
```

### State Management Architecture

```
┌──────────────────────────────────────────────────────┐
│  Zustand Stores                                       │
│                                                       │
│  gameStore                                            │
│  ├── gameView: GameView        (full board snapshot)  │
│  ├── zones: {                                        │
│  │     hand: CardView[]                              │
│  │     battlefield: CardView[]                       │
│  │     graveyard: CardView[]                         │
│  │     exile: CardView[]                             │
│  │     stack: SpellAbilityView[]                     │
│  │   }                                               │
│  ├── players: PlayerView[]                           │
│  ├── prompt: PromptState | null                      │
│  ├── phase: PhaseType                                │
│  ├── activePlayer: string                            │
│  ├── selectableCards: Set<number>                    │
│  └── gameLog: LogEntry[]                             │
│                                                       │
│  deckStore                                            │
│  ├── decks: DeckSummary[]                            │
│  ├── activeDeck: Deck | null                         │
│  └── searchResults: CardResult[]                     │
│                                                       │
│  lobbyStore                                           │
│  ├── selectedFormat: string                          │
│  ├── selectedDeck: string                            │
│  └── aiDifficulty: string                            │
└──────────────────────────────────────────────────────┘
```

### Key Data Flows

1. **Engine prompt -> player response:** Engine calls `IGuiGame.getChoices()` on `WebGuiGame`, which serializes to JSON, sends over WebSocket, sets `prompt` in `gameStore`. `PromptBar` renders choices. User clicks. `usePrompt` sends response over WebSocket. `MessageRouter` completes the `CompletableFuture`. Engine resumes.

2. **Zone update -> component re-render:** Engine moves a card (e.g., resolves a spell). `IGuiGame.updateZones()` fires. `WebGuiGame` serializes changed zones to JSON. `useGameSocket` receives message, calls `gameStore.setState()` with new zone data. Only the affected zone components re-render via Zustand selectors.

3. **Card search (deck builder):** User types in search box. Debounced REST call `GET /api/cards?q=lightning&format=standard`. Server queries `StaticData` card database. Returns card list JSON. `deckStore` updates `searchResults`. CardSearch component re-renders.

## Build Order (Dependencies)

The following build order reflects hard dependencies between components.

| Phase | What to Build | Depends On | Rationale |
|-------|---------------|------------|-----------|
| 1 | **Embedded HTTP server + StaticData init** | Nothing | Foundation: server must boot and load card database before anything else works |
| 2 | **REST endpoints (cards, decks, formats)** | Phase 1 | Stateless CRUD. Can be tested independently with curl/Postman |
| 3 | **WebSocket infrastructure + message protocol** | Phase 1 | Transport layer for game sessions. Define all message types up front |
| 4 | **WebGuiGame + WebGuiBase (IGuiGame/IGuiBase impl)** | Phase 3 | The bridge. Every engine callback must serialize to a defined message type |
| 5 | **Match start flow (lobby -> HostedMatch)** | Phase 2, 4 | REST endpoint creates HostedMatch wired to WebGuiGame; first end-to-end test |
| 6 | **React app scaffold + WebSocket hook + Zustand stores** | Phase 3 (protocol defs) | Frontend foundation. Can connect to server and receive messages |
| 7 | **Game board UI (zones, prompt bar, phase bar)** | Phase 6 | Core gameplay rendering. Build zone by zone |
| 8 | **Deck builder UI** | Phase 2 (REST), Phase 6 (scaffold) | REST-only feature, no WebSocket needed |
| 9 | **Polish: animations, error handling, reconnection** | Phase 7, 8 | Final layer after core functionality works |

**Critical path:** Phases 1 -> 3 -> 4 -> 5 must be sequential. The frontend (Phase 6-8) can begin in parallel once the message protocol (Phase 3) is defined, even before the backend is complete, by mocking WebSocket messages.

## Anti-Patterns

### Anti-Pattern 1: Polling for Game State

**What people do:** Use REST polling (setInterval + fetch) to check for game state changes instead of WebSocket push.
**Why it's wrong:** MTG games have irregular timing -- sometimes rapid (combat), sometimes slow (player thinking). Polling either wastes bandwidth or introduces visible latency. The engine already pushes events via `IGuiGame`; WebSocket is the natural fit.
**Do this instead:** Single persistent WebSocket per game session. All state changes pushed by server.

### Anti-Pattern 2: Full GameView on Every Update

**What people do:** Serialize and send the entire `GameView` (all zones, all cards, all players) on every state change.
**Why it's wrong:** A late-game board can have 50+ permanents, 20+ cards in graveyards, full game log. Sending everything on each card movement wastes bandwidth and causes UI flicker.
**Do this instead:** Send incremental `PlayerZoneUpdate` objects. The engine already produces these -- `IGuiGame.updateZones()` receives only the changed zones.

### Anti-Pattern 3: Game Logic in the Frontend

**What people do:** Duplicate rule checking (e.g., "can this creature attack?") in JavaScript to provide immediate feedback.
**Why it's wrong:** MTG rules are extraordinarily complex (20,000+ unique cards with interactions). Any frontend logic will diverge from the engine, creating bugs. The engine is authoritative.
**Do this instead:** The engine sends `selectableCards` with each prompt -- the set of valid choices. Frontend highlights those cards. Zero rule logic in JavaScript.

### Anti-Pattern 4: One Giant Game Component

**What people do:** Build a single `<Game>` component that holds all state and renders everything.
**Why it's wrong:** Any state change re-renders the entire board. With complex board states, this causes visible jank.
**Do this instead:** Zone-based decomposition with Zustand selectors. Each zone subscribes to its own slice of state.

### Anti-Pattern 5: Blocking the WebSocket Server Thread

**What people do:** Handle `PlayerControllerHuman` blocking calls on the WebSocket message thread, causing the server to stop processing other messages.
**Why it's wrong:** If the server's WebSocket thread blocks waiting for player input, it cannot process keepalive pings, other messages, or handle disconnection.
**Do this instead:** Run each game session's engine on its own thread. The engine thread blocks on `CompletableFuture.get()`. The WebSocket handler thread completes the future when a response arrives. Neither blocks the other.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| **Scryfall API** | Client-side only. `<img src="https://api.scryfall.com/cards/{scryfallId}?format=image">` | No server proxy needed. Rate limit: 10 req/sec. Use `loading="lazy"` on images. Cache via browser. |
| **Forge Card Database (StaticData)** | Server-side only. Loaded at startup from `res/cardsfolder/` and edition files | ~30 second load time. Load once, keep in memory. |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| **React <-> Java (game)** | WebSocket (JSON frames) | Typed message protocol. Each message has a `type` field and `requestId` for request-response correlation. |
| **React <-> Java (CRUD)** | REST (JSON) | Standard HTTP. Card search supports pagination. Deck save returns ID. |
| **WebGuiGame <-> Engine** | Direct Java method calls via `IGuiGame` interface | WebGuiGame IS the IGuiGame. Engine calls its methods directly. WebGuiGame serializes and sends over WebSocket. |
| **WebSocket Handler <-> Engine** | `CompletableFuture` completion | Handler receives player action message, finds the pending future by `requestId`, completes it. Engine thread unblocks. |
| **Frontend stores <-> Components** | Zustand selectors | Components subscribe to specific slices. No prop drilling for game state. |

## Threading Model

This is a critical architectural concern because the Forge engine uses blocking calls for player input.

```
Server Threads:
  Main Thread           - HTTP server event loop (Jetty/Javalin)
  WebSocket Thread      - Handles WS frame read/write (non-blocking)
  Game Thread (per game) - Runs HostedMatch.startGame() (blocks on player input)

Flow:
  Game Thread: engine calls WebGuiGame.getChoices() -> sends WS message -> blocks on CompletableFuture
  WebSocket Thread: receives player response -> completes CompletableFuture
  Game Thread: unblocks, returns choice to engine
```

**Key rule:** Never run game logic on the WebSocket thread. Always on a dedicated game thread.

## Sources

- Forge codebase: `IGuiGame.java`, `IGuiBase.java`, `AbstractGuiGame.java`, `HostedMatch.java`, `PlayerControllerHuman.java`
- [React TypeScript WebSocket guide](https://www.xjavascript.com/blog/react-typescript-websocket-example/)
- [Zustand vs Redux comparison](https://www.edstem.com/blog/zustand-vs-redux-why-simplicity-wins-in-modern-react-state-management)
- [State Management in 2025](https://dev.to/themachinepulse/do-you-need-state-management-in-2025-react-context-vs-zustand-vs-jotai-vs-redux-1ho)
- [WebSocket game architecture patterns](https://dev.to/sauravmh/building-a-multiplayer-game-using-websockets-1n63)
- [Lobby-based multiplayer browser games with React](https://riven.ch/en/news/build-lobby-based-online-multiplayer-browser-games-with-react-and-nodejs)

---
*Architecture research for: Forge MTG Web Client*
*Researched: 2026-03-16*
