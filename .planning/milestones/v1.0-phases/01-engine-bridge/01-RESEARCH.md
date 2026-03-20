# Phase 1: Engine Bridge - Research

**Researched:** 2026-03-16
**Domain:** Java WebSocket bridge for synchronous game engine (Forge MTG)
**Confidence:** HIGH

## Summary

Phase 1 builds a new Maven module `forge-gui-web` that bridges the Forge MTG engine to WebSocket-connected clients. The core challenge is the sync-to-async impedance mismatch: the Forge engine blocks on `CountDownLatch.await()` whenever it needs human input, and the web bridge must convert this to non-blocking WebSocket request-response. Five of eight identified critical pitfalls are Phase 1 concerns.

The existing `NetGuiGame` (Forge's LAN play implementation) provides a direct reference implementation. It extends `AbstractGuiGame` and uses a `send()` (fire-and-forget) / `sendAndWait()` (blocking request-response) pattern with a `GameProtocolSender`. The web implementation follows the same architecture but serializes to JSON via Jackson instead of Forge's binary `TrackableSerializer`. The `HeadlessGuiDesktop` test class demonstrates headless initialization of the engine -- the web module needs a similar but dedicated `WebGuiBase` that provides a single-threaded executor as pseudo-EDT.

**Primary recommendation:** Build the CompletableFuture-based input bridge first, prove it unblocks a mulligan decision via WebSocket, then layer in full IGuiGame coverage and DTO serialization.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Javalin 7 as the embedded HTTP + WebSocket server
- Exclude Forge's unused Jetty 9.4 transitive dependency; use Javalin's bundled Jetty 12
- Server runs on port 8080
- Headless initialization of Forge's StaticData/FModel -- strip GUI dependencies, initialize only card database and game data. Fail fast if desktop code is needed
- Jackson 2.21 (LTS) for JSON serialization
- Flat DTO classes with ID references to avoid circular reference issues in TrackableObject graph
- Incremental zone updates over WebSocket using Forge's existing PlayerZoneUpdate infrastructure
- CompletableFuture-based bridge: engine thread blocks on CompletableFuture.get(), WebSocket handler completes the future when player responds
- Single-threaded executor as pseudo-EDT to satisfy Forge's FThreads.assertExecutedByEdt() assertions
- Timeout with cleanup: set a timeout on CompletableFuture (e.g., 5 min). If expired, end the game session and clean up resources
- Implement ALL ~90 IGuiGame methods -- no stubs. Complete coverage from the start
- Also implement all IGuiBase methods needed for web context
- Full game loop integration test: start game, mulligan, play a land, cast a creature, attack, end turn, verify AI responds

### Claude's Discretion
- Exact DTO class structure and field naming
- WebSocket message protocol format (message types, envelope structure)
- Input correlation mechanism (inputId for nested input stack)
- Error serialization format
- Test framework choice (TestNG matches existing Forge tests)

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| API-03 | Java backend exposes WebSocket endpoint for real-time game state during matches | Javalin 7 WebSocket API, message protocol design, session management patterns |
| API-04 | Backend implements IGuiGame interface to bridge Forge engine events to WebSocket messages | NetGuiGame reference implementation, AbstractGuiGame base class, 90+ method categorization |
| API-05 | Backend implements IGuiBase interface for web-compatible platform operations | HeadlessGuiDesktop reference, pseudo-EDT executor pattern, GuiBase.setInterface() init sequence |
| API-06 | Backend initializes Forge card database and static data on startup | FModel.initialize() flow, ForgeConstants.ASSETS_DIR dependency on GuiBase, StaticData constructor |
</phase_requirements>

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Javalin | 7.0.x | Embedded HTTP + WebSocket server | Lightweight, first-class WS support, bundled Jetty 12, Java 17+. User decision. |
| Jackson Databind | 2.21.x | JSON serialization for DTOs and WS messages | LTS release Jan 2026, thread-safe ObjectMapper, polymorphic type support. User decision. |
| Jackson Module Parameter Names | 2.21.x | Constructor auto-detection | Avoids @JsonProperty boilerplate on every DTO field |
| TestNG | 7.10.2 | Test framework | Already used by forge-game module. Consistent with project conventions. |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| SLF4J | 2.0.x (existing) | Logging facade | Already in Forge. Javalin uses SLF4J natively. |
| javalin-jackson | 7.0.x | Javalin's Jackson integration plugin | Configure custom ObjectMapper for Javalin's ctx.json() and WS sendAsClass() |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Javalin 7 | Spring Boot 3.x | Massive overkill for localhost single-user tool. User explicitly chose Javalin. |
| Jackson 2.21 | Gson | Jackson faster, better streaming, polymorphic type handling for GameEvent subclasses |
| TestNG 7.10 | JUnit 5 | TestNG already used in forge-game tests. Staying consistent. |

**Installation (Maven):**
```xml
<!-- forge-gui-web/pom.xml -->
<dependencies>
    <dependency>
        <groupId>${project.groupId}</groupId>
        <artifactId>forge-gui</artifactId>
        <version>${project.version}</version>
        <exclusions>
            <exclusion>
                <groupId>org.eclipse.jetty</groupId>
                <artifactId>*</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>io.javalin</groupId>
        <artifactId>javalin</artifactId>
        <version>7.0.1</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.21.0</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.module</groupId>
        <artifactId>jackson-module-parameter-names</artifactId>
        <version>2.21.0</version>
    </dependency>
    <dependency>
        <groupId>org.testng</groupId>
        <artifactId>testng</artifactId>
        <version>7.10.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Architecture Patterns

### Recommended Project Structure

```
forge-gui-web/
├── pom.xml
└── src/
    ├── main/java/forge/web/
    │   ├── WebServer.java              # Entry point: Javalin config, server startup
    │   ├── WebGuiBase.java             # IGuiBase impl with pseudo-EDT executor
    │   ├── WebGuiGame.java             # IGuiGame impl extending AbstractGuiGame
    │   ├── WebInputBridge.java         # CompletableFuture-based input correlation
    │   ├── ViewRegistry.java           # Maps int IDs -> live TrackableObject instances
    │   ├── dto/
    │   │   ├── GameStateDto.java       # Flat top-level game state
    │   │   ├── PlayerDto.java          # Player with zone card-ID lists
    │   │   ├── CardDto.java            # Flat card with ID references
    │   │   ├── SpellAbilityDto.java    # Stack item representation
    │   │   ├── CombatDto.java          # Combat assignment state
    │   │   └── ZoneUpdateDto.java      # Incremental zone change
    │   └── protocol/
    │       ├── MessageType.java        # Enum: all inbound/outbound message types
    │       ├── OutboundMessage.java    # Server -> client envelope
    │       └── InboundMessage.java     # Client -> server envelope
    └── test/java/forge/web/
        ├── WebGuiBaseTest.java         # EDT executor tests
        ├── WebInputBridgeTest.java     # CompletableFuture timeout/completion tests
        ├── DtoSerializationTest.java   # Round-trip JSON tests, circular ref detection
        └── GameLoopIntegrationTest.java # Full game: start, mulligan, play, attack
```

### Pattern 1: Send / SendAndWait (from NetGuiGame)

**What:** Two communication modes matching Forge's existing network play architecture.
**When to use:** Every IGuiGame method falls into one of two categories.

```java
// Fire-and-forget: engine notifies client of state change
// Used by: updatePhase, updateZones, showPromptMessage, updateButtons, etc.
private void send(MessageType type, Object payload) {
    String json = objectMapper.writeValueAsString(
        new OutboundMessage(type, null, payload));
    wsContext.send(json);
}

// Blocking request-response: engine needs player decision
// Used by: getChoices, confirm, assignCombatDamage, etc.
private <T> T sendAndWait(MessageType type, String inputId, Object payload, Class<T> responseType) {
    CompletableFuture<String> future = new CompletableFuture<>();
    inputBridge.register(inputId, future);
    send(type, inputId, payload);
    try {
        String rawResponse = future.get(5, TimeUnit.MINUTES);
        return objectMapper.readValue(rawResponse, responseType);
    } catch (TimeoutException e) {
        inputBridge.remove(inputId);
        throw new GameSessionExpiredException("Input timeout");
    }
}
```

**Reference:** `NetGuiGame.java` lines 87-97 use this exact pattern with `sender.send()` and `sender.sendAndWait()`.

### Pattern 2: Pseudo-EDT via SingleThreadExecutor

**What:** A single-threaded executor that satisfies `FThreads.assertExecutedByEdt()` and `FThreads.isGuiThread()` checks.
**When to use:** WebGuiBase implementation. All EDT-related methods delegate to this executor.

```java
// WebGuiBase.java
private final ExecutorService edtExecutor = Executors.newSingleThreadExecutor(
    r -> { Thread t = new Thread(r, "Web-EDT"); t.setDaemon(true); return t; }
);

@Override
public boolean isGuiThread() {
    return Thread.currentThread().getName().equals("Web-EDT");
}

@Override
public void invokeInEdtLater(Runnable proc) {
    edtExecutor.submit(proc);
}

@Override
public void invokeInEdtAndWait(Runnable proc) {
    if (isGuiThread()) {
        proc.run();
    } else {
        try {
            edtExecutor.submit(proc).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

@Override
public void invokeInEdtNow(Runnable proc) {
    proc.run(); // Called when already on EDT
}
```

**Critical:** `FThreads.assertExecutedByEdt(false)` is called in `InputSyncronizedBase.awaitLatchRelease()` -- it asserts the current thread is NOT the EDT. The game thread must NOT be the Web-EDT thread. `FThreads.invokeInEdtNowOrLater()` in `InputSyncronizedBase.stop()` posts to the EDT to call `setFinished()`. Both must work correctly.

### Pattern 3: Initialization Sequence

**What:** GuiBase must be set BEFORE ForgeConstants is loaded (it reads `GuiBase.getInterface().getAssetsDir()` at class init time).
**When to use:** WebServer startup.

```java
// WebServer.java - main()
public static void main(String[] args) {
    // 1. MUST be first -- ForgeConstants reads GuiBase at class load
    GuiBase.setInterface(new WebGuiBase("./"));

    // 2. Initialize FModel (loads card database, ~5-30 seconds)
    FModel.initialize(null, preferences -> {
        preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
        preferences.setPref(FPref.UI_LANGUAGE, "en-US");
        return null;
    });

    // 3. Start Javalin server
    Javalin app = Javalin.create(config -> {
        config.jsonMapper(new JavalinJackson(objectMapper));
    });
    app.ws("/ws/game/{gameId}", ws -> { ... });
    app.start(8080);
}
```

**Reference:** `HeadlessGuiDesktop` and `AITest.java` both use this exact `GuiBase.setInterface() -> FModel.initialize()` sequence.

### Pattern 4: Input Stack Awareness (inputId Correlation)

**What:** Forge's `InputQueue` is a `BlockingDeque<InputSynchronized>` -- a STACK, not a queue. Inputs nest (e.g., cast spell -> select target -> pay mana). Each prompt sent to client must carry an `inputId`, and responses must match.

```java
// Message envelope
{
    "type": "PROMPT_CHOICE",
    "inputId": "uuid-1234",         // Correlates response to this specific input
    "sequenceNumber": 42,           // Monotonic for desync detection
    "payload": {
        "message": "Choose a target",
        "choices": [...],
        "min": 1, "max": 1
    }
}

// Client response
{
    "type": "CHOICE_RESPONSE",
    "inputId": "uuid-1234",         // Must match current top-of-stack
    "payload": { "indices": [2] }
}
```

**Server must reject responses whose inputId doesn't match the current top-of-stack input.**

### Anti-Patterns to Avoid

- **No-oping IGuiGame methods:** User explicitly requires ALL ~90 methods implemented, not stubbed. Every method must serialize to a WebSocket message or perform the appropriate action.
- **Running game logic on WebSocket thread:** The engine blocks on CompletableFuture.get(). If this runs on the WS thread, all other WS messages are blocked. Use `game.getAction().invoke()` which runs on `ThreadUtil.gameThreadPool`.
- **Forgetting GuiBase.setInterface() before ForgeConstants:** `ForgeConstants.ASSETS_DIR` calls `GuiBase.getInterface().getAssetsDir()` during class initialization. NPE if GuiBase not set first.
- **Treating InputQueue as request-response:** It's a stack. Nested inputs (target selection during mana payment) must resolve LIFO.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Game thread management | Custom thread pool | `ThreadUtil.invokeInGameThread()` / `game.getAction().invoke()` | Forge already has a cached game thread pool. Thread names starting with "Game" are checked by `ThreadUtil.isGameThread()` |
| Match lifecycle | Custom game loop | `HostedMatch.startMatch()` + `startGame()` | HostedMatch handles player registration, game creation, AI setup, match continuation |
| Base IGuiGame behavior | Implement from scratch | Extend `AbstractGuiGame` | Provides ~30 default method implementations: `oneOrNone`, `one`, `reveal`, `getInteger` variants, `many` -> `order` delegation, auto-yield, auto-pass, highlight tracking, selectable tracking |
| Zone update model | Custom change tracking | `PlayerZoneUpdate` / `PlayerZoneUpdates` | Already designed for incremental updates with `EnumSet<ZoneType>` per player |
| Event forwarding | Custom event bus | `FControlGameEventHandler` + `game.subscribeToEvents()` | Forge's EventBus system routes GameEvents to the correct GUI. NetGuiGame shows how to use `GameEventForwarder` |
| Input state machine | Custom input tracking | `InputQueue` + `InputProxy` + `InputSyncronizedBase` | The input stack, latch-based blocking, and Observer notification are all built in |

**Key insight:** The `AbstractGuiGame` base class already implements 30+ of the ~90 IGuiGame methods. WebGuiGame only needs to implement the abstract/unimplemented ones: the `send` methods (notifications) and `sendAndWait` methods (blocking choices). NetGuiGame is the template -- it has exactly 47 method overrides.

## Common Pitfalls

### Pitfall 1: Deadlocked Game Thread
**What goes wrong:** Engine blocks on `CountDownLatch.await()` in `InputSyncronizedBase.awaitLatchRelease()`. If the WebSocket response never completes the future, the game thread hangs forever.
**Why it happens:** Missing timeout, wrong inputId matching, client disconnection without cleanup.
**How to avoid:** Always use `CompletableFuture.get(timeout, TimeUnit)`. On timeout or disconnect, call `InputQueue.onGameOver(true)` to release all latches. Register a WebSocket `onClose` handler to clean up.
**Warning signs:** Server thread count grows, CPU at zero, game frozen after opening hand.

### Pitfall 2: Circular References in TrackableObject Serialization
**What goes wrong:** `CardView` -> `PlayerView` -> `CardView` cycles cause `StackOverflowError` with Jackson.
**Why it happens:** Forge's view objects use direct object references for in-process rendering.
**How to avoid:** Flat DTOs with ID references. `CardDto.ownerId` instead of embedded `PlayerView`. Jackson custom serializers that extract IDs, never traverse full object graph.
**Warning signs:** `StackOverflowError` during JSON serialization, WS messages > 100KB.

### Pitfall 3: Wrong Thread for EDT Assertions
**What goes wrong:** `FThreads.assertExecutedByEdt(false)` throws `IllegalStateException` because game thread is incorrectly identified as EDT.
**Why it happens:** `isGuiThread()` checks `Thread.currentThread().getName()` or platform-specific logic. If the pseudo-EDT thread name collides with game thread names, assertions fail.
**How to avoid:** Name the pseudo-EDT thread distinctly ("Web-EDT"), never "Game-*". Verify `ThreadUtil.isGameThread()` checks for "Game" prefix -- the pseudo-EDT must NOT start with "Game".
**Warning signs:** `IllegalStateException` from `FThreads.assertExecutedByEdt` during game start.

### Pitfall 4: Type Erasure in Generic Choice Methods
**What goes wrong:** `getChoices()` returns `List<T>` where T can be `CardView`, `SpellAbilityView`, `PlayerView`, `String`, `Integer`, or `Serializable`. Client sends back indices as JSON numbers. Server must resolve indices back to the original typed Java objects.
**Why it happens:** JSON erases Java generic type information. Client cannot send back typed objects.
**How to avoid:** Build a `ViewRegistry` mapping `int id -> TrackableObject`. Client responses use indices into the original choices list (not IDs). Server resolves: `choices.get(index)` returns the correctly-typed object.
**Warning signs:** `ClassCastException` in `PlayerControllerHuman` after receiving client response.

### Pitfall 5: FModel.initialize() Desktop Dependencies
**What goes wrong:** `FModel.initialize()` calls `FThreads.invokeInEdtLater()` for progress bar updates, accesses `ForgePreferences` which reads files, and triggers `SoundSystem.instance` initialization.
**Why it happens:** FModel was designed for desktop with Swing EDT, filesystem access, and audio.
**How to avoid:** WebGuiBase must provide working `invokeInEdtLater` (routes to pseudo-EDT executor). Pass `null` for progressBar. Set preferences to disable audio (`FPref.UI_ENABLE_SOUNDS` = false). Reference: `HeadlessGuiDesktop` + `AITest.java` show the minimal init path.
**Warning signs:** NPE during startup, `SoundSystem` errors, missing resource files.

## Code Examples

### Javalin 7 WebSocket Configuration
```java
// Source: Javalin official docs (javalin.io/documentation)
Javalin app = Javalin.create(config -> {
    config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new ParameterNamesModule());
    }));
});

app.ws("/ws/game/{gameId}", ws -> {
    ws.onConnect(ctx -> {
        String gameId = ctx.pathParam("gameId");
        ctx.enableAutomaticPings();  // Keepalive
        // Register session, send initial game state
    });
    ws.onMessage(ctx -> {
        InboundMessage msg = ctx.messageAsClass(InboundMessage.class);
        // Route to appropriate handler based on msg.type
    });
    ws.onClose(ctx -> {
        // Clean up: cancel pending futures, release game thread latches
    });
    ws.onError(ctx -> {
        // Log error, attempt cleanup
    });
});

app.start(8080);
```

### WebGuiGame Method Categorization
```java
// Source: Direct analysis of IGuiGame.java (90+ methods) + AbstractGuiGame.java

// CATEGORY A: Already implemented by AbstractGuiGame (DO NOT override unless adding WS serialization)
// ~30 methods: oneOrNone, one, reveal, getInteger(4 overloads), getChoices(1-arg),
// many(3 overloads), order(1 overload), insertInList, confirm(2 overloads),
// message(0-arg), showErrorDialog(0-arg), showConfirmDialog(3 overloads),
// showInputDialog(3 overloads), updateButtons(3-arg), setHighlighted, setUsedToPay,
// setSelectables, clearSelectables, isSelecting, isGamePaused, setGamePause,
// getGameSpeed, setGameSpeed, autoPassUntilEndOfTurn, autoPassCancel, mayAutoPass,
// awaitNextInput, cancelAwaitNextInput, updateAutoPassPrompt, shouldAutoYield,
// setShouldAutoYield, clearAutoYields, shouldAlwaysAcceptTrigger, etc.

// CATEGORY B: Fire-and-forget (send to client, don't block)
// ~25 methods: openView, afterGameEnd, showCombat, showPromptMessage,
// showCardPromptMessage, updateButtons(6-arg), flashIncorrectAction, alertUser,
// updatePhase, updateTurn, updatePlayerControl, enableOverlay, disableOverlay,
// finishGame, showManaPool, hideManaPool, updateStack, notifyStackAddition,
// notifyStackRemoval, handleLandPlayed, handleGameEvent, updateZones,
// updateSingleCard, updateCards, updateManaPool, updateLives, updateShards,
// updateDependencies, setCard, setPanelSelection, setPlayerAvatar,
// setCurrentPlayer (via updateCurrentPlayer), updateDayTime, setSelectables,
// clearSelectables, message(2-arg), showErrorDialog(2-arg)

// CATEGORY C: Blocking request-response (sendAndWait, blocks game thread)
// ~15 methods: getChoices(6-arg), confirm(4-arg), showConfirmDialog(5-arg),
// showOptionDialog, showInputDialog(6-arg), getAbilityToPlay,
// assignCombatDamage, assignGenericAmount, tempShowZones, chooseSingleEntityForEffect,
// chooseEntitiesForEffect, manipulateCardList, sideboard, order(8-arg),
// openZones, isUiSetToSkipPhase
```

### HostedMatch Integration
```java
// Source: HostedMatch.java lines 108-150, 162-302
// Starting a game match for web:

WebGuiGame webGui = new WebGuiGame(wsContext, objectMapper, inputBridge, viewRegistry);

RegisteredPlayer humanPlayer = new RegisteredPlayer(deck)
    .setPlayer(GamePlayerUtil.getGuiPlayer());
RegisteredPlayer aiPlayer = new RegisteredPlayer(aiDeck)
    .setPlayer(GamePlayerUtil.createAiPlayer());

List<RegisteredPlayer> players = List.of(humanPlayer, aiPlayer);
Map<RegisteredPlayer, IGuiGame> guis = Map.of(humanPlayer, webGui);

HostedMatch hostedMatch = new HostedMatch();
hostedMatch.startMatch(GameType.Constructed, null, players, guis, null);
// Game now runs on a game thread via game.getAction().invoke()
// Engine will call webGui methods when it needs to notify or prompt the player
```

### ViewRegistry for Type-Safe ID Resolution
```java
// Resolves client IDs back to live Java objects for engine consumption
public class ViewRegistry {
    private final Map<Integer, TrackableObject> registry = new ConcurrentHashMap<>();

    public void register(TrackableObject obj) {
        registry.put(obj.getId(), obj);
    }

    @SuppressWarnings("unchecked")
    public <T extends TrackableObject> T resolve(int id, Class<T> type) {
        TrackableObject obj = registry.get(id);
        if (obj == null) throw new IllegalArgumentException("Unknown ID: " + id);
        if (!type.isInstance(obj)) throw new ClassCastException(
            "ID " + id + " is " + obj.getClass().getSimpleName() + ", expected " + type.getSimpleName());
        return (T) obj;
    }

    public void clear() { registry.clear(); }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Forge binary `TrackableSerializer` for network play | JSON via Jackson for web client | This project | Enables browser clients; `TrackableSerializer` uses ordinal-based binary format unsuitable for JS consumption |
| Swing EDT as threading model | Single-threaded executor as pseudo-EDT | This project | Decouples from desktop GUI toolkit while preserving threading contracts |
| `CountDownLatch.await()` with no timeout | `CompletableFuture.get(5, TimeUnit.MINUTES)` | This project | Prevents permanent thread hangs on client disconnect |

**Existing patterns to preserve:**
- `game.getAction().invoke()` for running game logic on game threads (uses `ThreadUtil.gameThreadPool`)
- `InputQueue` as `BlockingDeque<InputSynchronized>` for nested input stack
- `FControlGameEventHandler` for routing `GameEvent`s to the GUI
- `AbstractGuiGame` as base class for all GUI implementations

## Open Questions

1. **FModel singleton initialization in multi-game context**
   - What we know: `FModel` uses `Suppliers.memoize()` for lazy singletons. `StaticData.lastInstance` is a static field. Both are designed for single-instance use.
   - What's unclear: Can two concurrent game sessions share the same `FModel`/`StaticData` instance safely? The card database is read-only after init, so likely yes.
   - Recommendation: Share a single `FModel` instance across all sessions. Initialize once at server startup. One `WebGuiGame` instance per game session.

2. **HostedMatch event subscription lifecycle**
   - What we know: `HostedMatch.startGame()` subscribes `FControlGameEventHandler` to game events. `endCurrentGame()` clears human controllers and calls `afterGameEnd()`.
   - What's unclear: Does the event bus automatically unsubscribe when the game ends, or do we need explicit cleanup to prevent memory leaks across sessions?
   - Recommendation: Call `endCurrentGame()` on game over. Implement `WebGuiGame.afterGameEnd()` to close WS connection and release resources.

3. **SoundSystem.instance initialization**
   - What we know: `HostedMatch.startMatch()` line 146 calls `this.match.subscribeToEvents(SoundSystem.instance)`. SoundSystem is a singleton that may fail in headless mode.
   - What's unclear: Will SoundSystem initialization throw in a headless context?
   - Recommendation: Investigate `SoundSystem.instance` initialization path. May need to set `FPref.UI_ENABLE_SOUNDS` to false or provide a no-op SoundSystem.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | TestNG 7.10.2 |
| Config file | None -- see Wave 0 |
| Quick run command | `mvn test -pl forge-gui-web -Dtest=WebInputBridgeTest -q` |
| Full suite command | `mvn test -pl forge-gui-web -q` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| API-03 | WebSocket endpoint accepts connections and sends game state | integration | `mvn test -pl forge-gui-web -Dtest=GameLoopIntegrationTest#testWebSocketConnection -q` | Wave 0 |
| API-04 | IGuiGame methods serialize to WebSocket messages | unit | `mvn test -pl forge-gui-web -Dtest=WebGuiGameTest -q` | Wave 0 |
| API-04 | Blocking choice methods (getChoices, confirm, etc.) complete via WS response | integration | `mvn test -pl forge-gui-web -Dtest=WebInputBridgeTest -q` | Wave 0 |
| API-05 | IGuiBase pseudo-EDT executes runnables on correct thread | unit | `mvn test -pl forge-gui-web -Dtest=WebGuiBaseTest -q` | Wave 0 |
| API-06 | StaticData/FModel initialize in headless mode | integration | `mvn test -pl forge-gui-web -Dtest=GameLoopIntegrationTest#testHeadlessInit -q` | Wave 0 |
| API-06 | Full game loop: start, mulligan, play land, cast creature, attack | integration | `mvn test -pl forge-gui-web -Dtest=GameLoopIntegrationTest#testFullGameLoop -q` | Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn test -pl forge-gui-web -q`
- **Per wave merge:** `mvn test -pl forge-gui-web -q` (same -- single module)
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `forge-gui-web/pom.xml` -- Maven module configuration with TestNG dependency
- [ ] `forge-gui-web/src/test/java/forge/web/WebGuiBaseTest.java` -- pseudo-EDT thread tests
- [ ] `forge-gui-web/src/test/java/forge/web/WebInputBridgeTest.java` -- CompletableFuture timeout/completion
- [ ] `forge-gui-web/src/test/java/forge/web/DtoSerializationTest.java` -- JSON round-trip, no circular refs
- [ ] `forge-gui-web/src/test/java/forge/web/GameLoopIntegrationTest.java` -- full game loop
- [ ] Root `pom.xml` -- add `forge-gui-web` module declaration

## Sources

### Primary (HIGH confidence)
- Direct codebase analysis: `IGuiGame.java` (90+ methods), `IGuiBase.java` (35+ methods), `AbstractGuiGame.java` (990 lines, ~30 default implementations)
- Direct codebase analysis: `NetGuiGame.java` (350 lines) -- existing remote IGuiGame implementation using send/sendAndWait pattern
- Direct codebase analysis: `HeadlessGuiDesktop.java` (95 lines) -- existing headless IGuiBase for testing
- Direct codebase analysis: `InputQueue.java` (BlockingDeque stack), `InputSyncronizedBase.java` (CountDownLatch blocking), `InputProxy.java` (Observer pattern)
- Direct codebase analysis: `FThreads.java` (EDT assertion via `GuiBase.getInterface().isGuiThread()`)
- Direct codebase analysis: `HostedMatch.java` (530 lines) -- match lifecycle, game thread launch via `game.getAction().invoke()`
- Direct codebase analysis: `ThreadUtil.java` -- game thread pool (`Executors.newCachedThreadPool`), thread naming ("Game" prefix)
- Direct codebase analysis: `FModel.java` -- initialization sequence, `Suppliers.memoize()` for StaticData
- Direct codebase analysis: `ForgeConstants.java` -- ASSETS_DIR reads from `GuiBase.getInterface().getAssetsDir()` at class load time
- [Javalin 7 Documentation](https://javalin.io/documentation) -- WebSocket API, WsContext methods, Jackson integration

### Secondary (MEDIUM confidence)
- `.planning/research/PITFALLS.md` -- 8 critical pitfalls, 5 Phase 1 concerns
- `.planning/research/ARCHITECTURE.md` -- System architecture, threading model, data flow
- `.planning/research/STACK.md` -- Technology recommendations with versions

### Tertiary (LOW confidence)
- None -- all findings derived from direct codebase analysis and official documentation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- user decisions locked, versions verified against existing pom.xml
- Architecture: HIGH -- derived from existing `NetGuiGame` reference implementation and direct code analysis
- Pitfalls: HIGH -- derived from direct analysis of `InputSyncronizedBase`, `FThreads`, `TrackableObject` circular refs
- Threading model: HIGH -- verified against `FThreads.java`, `ThreadUtil.java`, `GuiDesktop.java` EDT implementation

**Research date:** 2026-03-16
**Valid until:** 2026-04-16 (stable domain -- Forge engine internals change slowly)
