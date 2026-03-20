# Architecture Research: v2.0 Feature Integration

**Domain:** MTG web client -- gameplay UX, format support, simulation, image quality
**Researched:** 2026-03-20
**Confidence:** HIGH (based on direct codebase analysis, not external sources)

## System Overview: Current Architecture

```
+-----------------------------------------------------------------+
|                     Frontend (React 19)                         |
| +----------+ +----------+ +---------+ +---------+ +----------+ |
| | GameBoard| | DeckEditor| |GameLobby| |ActionBar| |GameStore | |
| +----+-----+ +----+-----+ +----+----+ +----+----+ +----+-----+ |
|      |             |            |           |           |       |
|      +------+------+------+----+-----+-----+           |       |
|             |              |         |                  |       |
|        TanStack Query   GameWebSocket class     Zustand (immer) |
+-----------------------------------------------------------------+
         |  REST              |  WebSocket
+-----------------------------------------------------------------+
|                    Backend (Javalin 7)                           |
| +---------------+ +------------------+ +-----------+            |
| | CardSearch    | | WebServer        | | GameSession|            |
| | DeckHandler   | | WS /ws/game/{id} | | WebGuiGame |            |
| | FormatValid.  | | handleStartGame  | | InputBridge|            |
| | ImportExport  | |                  | | ViewRegistry|           |
| +-------+-------+ +--------+---------+ +-----+-----+            |
|         |                   |                 |                  |
+-----------------------------------------------------------------+
|                  Forge Engine (forge-game)                       |
| +--------+ +--------+ +---------+ +--------+ +-------+         |
| |GameView| |GameLog | |MagicStack| |HostedMatch| |GameType|    |
| |CardView| |GameLog | |   .undo()| |.startMatch | |       |    |
| |PlayerV.| |EntryType| |         | |           | |       |    |
| +--------+ +--------+ +---------+ +-----------+ +-------+     |
+-----------------------------------------------------------------+
```

## v2.0 Integration Map: New vs Modified Components

### Component Change Matrix

| Component | Status | Change Description |
|-----------|--------|-------------------|
| **Backend -- Java** | | |
| `WebGuiGame.java` | MODIFY | Add `GAME_LOG` sends; wire `isUiSetToSkipPhase` to phase-skip store |
| `WebServer.java` | MODIFY | Add `UNDO`, `PASS_UNTIL_EOT`, `SET_AUTO_YIELD`, `CONCEDE` inbound handlers; add `/api/jumpstart/packs` and `/api/simulate` REST routes |
| `MessageType.java` | MODIFY | Add 5 new message types (see below) |
| `GameStateDto.java` | MODIFY | Add `priorityPlayerId`, `canUndo` fields |
| `CardDto.java` | MODIFY | Add `setCode` field (from `CardView.getCurrentState().getSetCode()`) |
| `GameLogDto.java` | NEW | DTO for game log entries: `{type, message, timestamp}` |
| `JumpstartHandler.java` | NEW | REST handler: list available packs, generate pack contents |
| `SimulationHandler.java` | NEW | REST handler: start headless AI-vs-AI batch, poll results |
| `HeadlessGuiGame.java` | NEW | No-op `IGuiGame` for headless simulation (no WebSocket) |
| `SimulationRunner.java` | NEW | Orchestrates N headless games, collects win/loss/turn stats |
| **Frontend -- TypeScript** | | |
| `gameTypes.ts` | MODIFY | Add new message types and DTOs |
| `gameWebSocket.ts` | MODIFY | Add `sendUndo`, `sendPassUntilEOT`, `sendSetAutoYield`, `sendConcede` |
| `gameStore.ts` | MODIFY | Add `gameLog`, `priorityPlayerId`, `canUndo`, `autoYields` state |
| `GameBoard.tsx` | MODIFY | Integrate GameLog panel, priority indicator |
| `ActionBar.tsx` | MODIFY | Undo button, keyboard shortcut hints, pass-until-EOT button |
| `PhaseStrip.tsx` | MODIFY | Click-to-toggle auto-yield per phase |
| `GameLobby.tsx` | MODIFY | Jumpstart pack selection flow (conditional on format) |
| `GameCardImage.tsx` | MODIFY | Use `setCode` from CardDto for direct Scryfall URL instead of name-based |
| `GameLogPanel.tsx` | NEW | Scrollable, filterable game log component |
| `PriorityIndicator.tsx` | NEW | Visual indicator of who has priority |
| `KeyboardShortcuts.tsx` | NEW | Global keyboard handler + help overlay |
| `JumpstartPackPicker.tsx` | NEW | Pack selection UI for Jumpstart lobby flow |
| `SimulationPage.tsx` | NEW | Page to configure and run deck simulations |
| `SimulationResults.tsx` | NEW | Display win rates, average turns, matchup data |
| `useSimulation.ts` | NEW | Hook for simulation API (TanStack Query mutations) |

## Detailed Data Flow Changes

### 1. Gameplay UX -- Priority State + Game Log

**Current flow:** Engine calls `WebGuiGame.updateButtons()` -> client infers priority from `playerId` field.

**New flow -- explicit priority:**
```
Engine: updatePhase() / updateButtons()
    |
WebGuiGame: enriches GAME_STATE with priorityPlayerId and canUndo
    |
WebSocket: GAME_STATE { ..., priorityPlayerId: 0, canUndo: true }
    |
gameStore: sets state.priorityPlayerId, state.canUndo
    |
PriorityIndicator + ActionBar: render based on who has priority, show/hide undo
```

**Implementation detail:** `priorityPlayerId` comes from `GameView.getPlayerTurn()` combined with the phase. However, the more accurate source is the `humanPlayerId` field + checking if `buttons.enable1` is true (meaning the human has priority). The engine already sends this via `BUTTON_UPDATE`. The cleaner approach: add a `priorityPlayerId` field to `GameStateDto.from()` using the player who currently has the input queue active. The `BUTTON_UPDATE.playerId` already carries this, but having it in `GAME_STATE` too makes the frontend simpler.

**Game Log flow:**
```
Engine: Game.getGameLog() (already populated by GameLogFormatter event visitor)
    |
WebGuiGame: on updateZones/updatePhase/updateStack, send latest log entries
    |  New method: sendLogDelta() -- sends entries since last sequence number
    |
WebSocket: GAME_LOG { entries: [{type: "STACK_ADD", message: "...", seq: 42}, ...] }
    |
gameStore: appends to state.gameLog array
    |
GameLogPanel: renders scrollable list, filterable by GameLogEntryType
```

**Key insight:** The engine's `GameLog` already captures everything we need. It has typed entries (`STACK_ADD`, `ZONE_CHANGE`, `DAMAGE`, `COMBAT`, etc.). We just need to serialize and send them. The `GameLog` is accessible via `Game.getGameLog()`, but `WebGuiGame` only has access to `GameView`, not `Game` directly. Solution: either pass the `Game` reference to `WebGuiGame`, or (cleaner) subscribe to `GameLog` as an `Observer` since `GameLog extends Observable`.

### 2. Undo Support

**Current state:** Forge engine fully supports undo. `IGameController.undoLastAction()` exists. `MagicStack.undo()` exists. `InputPassPriority` already shows "Undo" as the cancel button label when undo is available. The web client just needs to recognize it.

**What's already working for free:** When `canUndo` is true, the engine already sends a `BUTTON_UPDATE` with `label2: "Undo"`. The frontend currently renders this but sends `BUTTON_CANCEL` which triggers `gc.selectButtonCancel()` -- and `InputPassPriority.onCancel()` already calls `tryUndoLastAction()`. So undo **already partially works** through the cancel button path.

**What's needed:**
```
Frontend: explicit UNDO inbound message type (cleaner than overloading BUTTON_CANCEL)
    |
WebServer: case UNDO -> session.webGuiGame.getGameController().undoLastAction()
    |
Engine: MagicStack.undo() -> reverts last spell/ability
    |
WebGuiGame: receives state updates -> sends new GAME_STATE to frontend
```

**Also needed:** `canUndo` field in `GameStateDto` so the frontend can show/hide the undo button proactively, not just when the engine happens to label the cancel button "Undo".

### 3. Auto-Yield / Auto-Pass

**Current state:** `WebGuiGame.isUiSetToSkipPhase()` returns `false` always. `AbstractGuiGame` already has the `autoYields` Set and `shouldAutoYield(key)` method. `autoPassUntilEndOfTurn` is also implemented in `AbstractGuiGame`.

**New flow:**
```
Frontend: user clicks phase in PhaseStrip to toggle auto-yield
    |
WebSocket: SET_AUTO_YIELD { phase: "UPKEEP", enabled: true }
    |
WebServer: session.webGuiGame.setSkipPhase(phase, enabled)
    |
Engine: next time this phase comes up, isUiSetToSkipPhase returns true,
    |  PlayerControllerHuman auto-passes
    |
No UI prompt sent to client -- game flows faster

Frontend: user clicks "Pass Turn" button
    |
WebSocket: PASS_UNTIL_EOT {}
    |
WebServer: session.webGuiGame.getGameController().passPriorityUntilEndOfTurn()
    |
Engine: auto-passes all remaining phases this turn
```

**Override for `isUiSetToSkipPhase`:**
```java
// In WebGuiGame, override to check a stored set of skipped phases:
private final Set<PhaseType> skippedPhases = EnumSet.noneOf(PhaseType.class);

@Override
public boolean isUiSetToSkipPhase(PlayerView playerTurn, PhaseType phase) {
    return skippedPhases.contains(phase);
}

public void setSkipPhase(PhaseType phase, boolean skip) {
    if (skip) skippedPhases.add(phase); else skippedPhases.remove(phase);
}
```

### 4. Jumpstart Format Support

**Key discovery:** There is NO `GameType.Jumpstart` in Forge's engine. Jumpstart is handled as a special deck construction mode (pick 2 packs, combine into 40-card deck), then played as `GameType.Constructed`. The mobile Adventure mode uses `SealedTemplate` from `specialBoosters` storage (`boosters-special.txt`) filtered by edition code.

**Architecture:**
```
REST: GET /api/jumpstart/packs?edition={code}
    |
JumpstartHandler:
    1. StaticData.instance().getSpecialBoosters() -- get all SealedTemplates
    2. Filter by edition code (e.g., "J22" for Jumpstart 2022)
    3. Randomly select 6 packs (JUMPSTART_TO_PICK_FROM = 6)
    4. For each: UnOpenedProduct(template).get() -> List<PaperCard>
    5. Return as List<JumpstartPackDto> { name, theme, cards: [{name, setCode, collectorNumber}] }
    |
Frontend: JumpstartPackPicker shows 6 packs, user picks 2
    |
REST: POST /api/decks { name, format: "Jumpstart", cards: [...combined 40 cards] }
    |
GameLobby: starts game with format "Constructed" (engine GameType)
    |
WebServer.handleStartGame: maps "Jumpstart" format to GameType.Constructed
    (same as "Casual 60-card" -- there's no engine-level Jumpstart)
```

**Lobby flow change:**
```
GameLobby
    |-- format = "Jumpstart"?
    |   YES -> show JumpstartPackPicker instead of DeckPicker
    |          -> after 2 packs selected, auto-create deck, then proceed to start
    |   NO  -> existing DeckPicker flow
```

### 5. Headless Deck Simulation

**Critical design decision:** Simulation must NOT use WebSocket or any UI. It needs a no-op `IGuiGame` implementation.

**Architecture:**
```
REST: POST /api/simulate
    Body: { deckName, opponentDeckNames: [...], gamesPerMatchup: 10, format }
    |
SimulationHandler:
    1. Validates decks exist
    2. Creates SimulationRunner on a background thread
    3. Returns { simulationId } immediately
    |
SimulationRunner (on dedicated thread pool):
    For each matchup (deck vs opponent):
        For each game:
            1. Load decks
            2. Create HeadlessGuiGame (no-op IGuiGame)
            3. Register both players as AI
            4. hostedMatch.startMatch(GameType.Constructed, null, players, guis)
            5. Wait for game to complete
            6. Record: winner, turn count, life totals
    Aggregate: win rate, avg turns, per-matchup breakdown
    |
REST: GET /api/simulate/{id}  -- poll for results
    Returns: { status: "running"|"complete", progress: 7/10, results: {...} }
```

**HeadlessGuiGame implementation:**
```java
public class HeadlessGuiGame extends AbstractGuiGame {
    // Every fire-and-forget method: no-op (don't send to WebSocket)
    // Every blocking method (getChoices, confirm, etc.):
    //   Return defaults -- but this shouldn't matter because both
    //   players are AI, so PlayerControllerAi handles all decisions.
    //   The IGuiGame is only needed for the human player's GUI.
    //
    // Key question: does HostedMatch.startMatch require an IGuiGame
    //   for the human player? Yes -- the guis map must contain
    //   at least one entry. For AI-vs-AI, we register both as AI
    //   players, so the guis map can be empty or contain a dummy.

    @Override public void openView(TrackableCollection<PlayerView> p) { }
    @Override public void afterGameEnd() { super.afterGameEnd(); }
    // ... all overrides return no-ops or safe defaults
    @Override public boolean isUiSetToSkipPhase(PlayerView p, PhaseType ph) { return false; }
    @Override public <T> List<T> getChoices(...) { return Collections.emptyList(); }
    @Override public boolean confirm(...) { return defaultIsYes; }
}
```

**Threading consideration:** Each simulation game runs on the game thread via `ThreadUtil.invokeInGameThread()`. For batch simulation, we need to ensure games run sequentially on the game thread or create separate game threads. The existing `ThreadUtil` uses a single-thread executor for game logic.

**Recommendation:** Run simulation games sequentially on a dedicated simulation thread (not the interactive game thread). A 10-game simulation at ~2 seconds per AI-vs-AI game = 20 seconds. Acceptable for a local tool. Use a separate single-thread executor for simulation to avoid blocking interactive play.

### 6. Card Image Quality (Scryfall Set/Language Filtering)

**Current state:**
- Deck editor (`CardImage.tsx`): Uses `getScryfallImageUrl(setCode, collectorNumber)` -- already uses direct URLs
- Game board (`GameCardImage.tsx`): Uses name-based Scryfall URL (`/cards/named?exact=...`) because `CardDto` lacks `setCode`/`collectorNumber`

**Problem:** Name-based Scryfall URLs return the "most recent" printing, which may be a special art or alternate frame.

**Solution -- two changes:**

1. **Add `setCode` to game `CardDto`:**
```java
// In CardDto.from(CardView cv):
final CardStateView state = cv.getCurrentState();
if (state != null) {
    dto.setCode = state.getSetCode();  // Already available on CardStateView
}
```
Note: `collectorNumber` is NOT available on `CardView`/`CardStateView` (only on `PaperCard`). But `setCode` + name is sufficient for Scryfall.

2. **Update frontend image URL construction:**
The Scryfall `/cards/named` endpoint accepts a `set` query param:
```typescript
function getGameCardImageUrl(name: string, setCode?: string): string {
  const params = new URLSearchParams({ exact: name, format: 'image', version: 'normal' })
  if (setCode) params.set('set', setCode.toLowerCase())
  return `https://api.scryfall.com/cards/named?${params}`
}
```

This uses the deck's actual printing rather than Scryfall's "most recent" default. No engine changes needed.

## New WebSocket Message Types

### Outbound (Server -> Client)

| Type | Payload | Trigger |
|------|---------|---------|
| `GAME_LOG` | `{ entries: [{type: string, message: string, seq: number}] }` | After each engine state change |

### Inbound (Client -> Server)

| Type | Payload | Handler |
|------|---------|---------|
| `UNDO` | `null` | `gc.undoLastAction()` |
| `PASS_UNTIL_EOT` | `null` | `gc.passPriorityUntilEndOfTurn()` |
| `SET_AUTO_YIELD` | `{ key: string, enabled: boolean }` | `webGuiGame.setSkipPhase(phase, enabled)` |
| `CONCEDE` | `null` | `gc.concede()` |

### Modified Outbound

| Type | Change |
|------|--------|
| `GAME_STATE` | Add `priorityPlayerId: number`, `canUndo: boolean` to `GameStateDto` |

## New REST Endpoints

| Method | Path | Handler | Purpose |
|--------|------|---------|---------|
| `GET` | `/api/jumpstart/packs` | `JumpstartHandler::getPacks` | Get 6 random Jumpstart packs for selection |
| `POST` | `/api/simulate` | `SimulationHandler::start` | Start headless simulation batch |
| `GET` | `/api/simulate/{id}` | `SimulationHandler::status` | Poll simulation progress and results |

## Architectural Patterns

### Pattern 1: GameLog Delta Streaming

**What:** Track a "last sent sequence number" per session and only send new log entries, not the full log.

**When to use:** Any append-only data stream over WebSocket.

**Trade-offs:** More efficient than full snapshots. Requires client to handle append-only semantics. Since we already have a `sequenceCounter` on outbound messages, we can detect gaps.

**Implementation sketch:**
```java
// In WebGuiGame:
private int lastSentLogIndex = 0;

private void sendLogDelta() {
    List<GameLogEntry> all = game.getGameLog().getLogEntries(null);
    if (all.size() <= lastSentLogIndex) return;

    List<Map<String,Object>> newEntries = new ArrayList<>();
    for (int i = lastSentLogIndex; i < all.size(); i++) {
        GameLogEntry e = all.get(i);
        newEntries.add(payloadMap("type", e.type().name(),
            "message", e.message(), "seq", i));
    }
    lastSentLogIndex = all.size();
    send(MessageType.GAME_LOG, newEntries);
}
```

### Pattern 2: No-Op GUI for Headless Execution

**What:** An `IGuiGame` implementation where every method is a no-op or returns a safe default. Allows the engine to run without any UI connected.

**When to use:** AI-vs-AI simulation, testing, any headless game execution.

**Trade-offs:** Must cover all 48+ `IGuiGame` methods. Safe because AI players use `PlayerControllerAi`, not `PlayerControllerHuman`, so blocking GUI methods should never be called for AI players. If the engine calls a GUI method unexpectedly, the no-op handles it silently.

### Pattern 3: Lobby Flow Branching by Format

**What:** The GameLobby conditionally renders different setup flows based on the selected format.

**When to use:** Any format requiring non-standard deck construction before game start.

**Implementation:**
```tsx
{selectedFormat === 'Jumpstart' ? (
  <JumpstartPackPicker onPacksSelected={(deckName) => setSelectedDeck(deckName)} />
) : (
  <DeckPicker ... />
)}
```

## Anti-Patterns

### Anti-Pattern 1: Polling GameLog from Frontend via REST

**What people do:** Timer-based GET `/api/game/{id}/log` every 500ms.

**Why it's wrong:** The WebSocket is already open. Adding REST polling creates two competing state channels, ordering issues, and unnecessary load.

**Do this instead:** Stream game log entries over the existing WebSocket as a new message type.

### Anti-Pattern 2: Running Simulation on the Interactive Game Thread

**What people do:** Send "SIMULATE" over WebSocket, run N games on the same game thread.

**Why it's wrong:** Blocks any interactive game the user might want to play. Simulation doesn't need bidirectional WebSocket communication.

**Do this instead:** REST endpoint with dedicated simulation executor. Return simulation ID, let client poll results.

### Anti-Pattern 3: Adding CollectorNumber to CardView TrackableProperties

**What people do:** Modify `CardView`/`CardState` to track collector number for Scryfall URLs.

**Why it's wrong:** `CardView` is in `forge-game`, a core module used by all UIs. Adding properties for one UI's needs pollutes the engine. Trackable properties cost serialization overhead on every state update.

**Do this instead:** Use `setCode` (already on `CardStateView`) with Scryfall's `set` parameter on name-based URLs. Same result, no engine changes.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Scryfall API | Direct URL construction: `/cards/named?exact={name}&set={setCode}&format=image` | No API key needed; 10 req/s rate limit; `set` param constrains to specific printing |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| `WebGuiGame` <-> `GameLog` | Observer pattern or direct reference | `GameLog extends Observable`; `WebGuiGame` can register as observer for new entry notifications |
| `SimulationRunner` <-> `HostedMatch` | Direct method call | `HeadlessGuiGame` passed in guis map; game runs synchronously |
| `JumpstartHandler` <-> `StaticData` | Direct API call | `StaticData.instance().getSpecialBoosters()` for pack templates |
| `GameLobby` <-> `JumpstartPackPicker` | React props/callbacks | Pack selection result flows up via callback |

## Build Order (Dependency-Driven)

```
1. GameStateDto enrichment (priorityPlayerId, canUndo, setCode in CardDto)
   |-- No dependencies, unlocks: priority UI, undo visibility, image quality
   |
2. New inbound message handlers (UNDO, PASS_UNTIL_EOT, SET_AUTO_YIELD, CONCEDE)
   |-- Depends on: nothing (engine APIs already exist on IGameController)
   |-- Unlocks: all gameplay UX features
   |
3. GameLog streaming (GAME_LOG message type + WebGuiGame observer wiring)
   |-- Depends on: WebGuiGame getting Game/GameLog reference
   |-- Unlocks: GameLogPanel frontend
   |
4. Frontend UX components (PriorityIndicator, Undo button, PhaseStrip auto-yield)
   |-- Depends on: #1 and #2
   |
5. GameLogPanel
   |-- Depends on: #3
   |
6. Keyboard shortcuts
   |-- Depends on: #2 and #4 (needs actions to bind)
   |
7. Card image quality (setCode in GameCardImage)
   |-- Depends on: #1 (setCode in CardDto)
   |-- Independent of gameplay features
   |
8. Jumpstart pack selection
   |-- Independent (engine APIs exist, new REST + frontend)
   |
9. Headless simulation
   |-- Independent (new REST + backend, no WebSocket needed)
```

**Recommended phase grouping:**
1. **Phase 1: Backend plumbing** -- #1, #2, #3 (backend changes that unlock all frontend work)
2. **Phase 2: Gameplay UX** -- #4, #5, #6, #7 (frontend features consuming Phase 1)
3. **Phase 3: Jumpstart** -- #8 (self-contained vertical slice)
4. **Phase 4: Simulation** -- #9 (most isolated, most new code, highest risk)

## Sources

- Direct codebase analysis of `forge-gui-web`, `forge-gui`, `forge-game`, `forge-core` modules
- `IGameController.java`: `undoLastAction()`, `concede()`, `passPriorityUntilEndOfTurn()` already in interface
- `AbstractGuiGame.java`: `autoYields` Set, `shouldAutoYield(key)`, `autoPassUntilEndOfTurn` all implemented
- `GameLog.java` / `GameLogEntryType.java`: 18 log entry types, Observable pattern for notifications
- `CardView.CardStateView.getSetCode()`: set code available for game cards without engine changes
- `StaticData.getSpecialBoosters()`: Jumpstart pack templates loaded from `boosters-special.txt`
- `PlayerControllerHuman.java`: undo flow via `tryUndoLastAction()` -> `MagicStack.undo()`
- `AdventureEventController.getJumpstartBoosters()`: reference implementation for pack generation
- `GameType.java`: no Jumpstart enum value -- Jumpstart plays as `GameType.Constructed`

---
*Architecture research for: Forge Web Client v2.0 feature integration*
*Researched: 2026-03-20*
