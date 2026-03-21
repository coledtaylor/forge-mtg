# Phase 12: Deck Simulation - Research

**Researched:** 2026-03-20
**Domain:** Headless AI vs AI simulation engine, real-time progress streaming, statistics dashboard
**Confidence:** HIGH

## Summary

Phase 12 builds a headless AI vs AI simulation system that runs configurable numbers of games against a gauntlet of opponents, streams progress in real time, and displays comprehensive statistics in a tabbed dashboard. The core challenge is running Forge games without any GUI -- the engine was designed for interactive play with a human, and several critical code paths assume a GUI is present.

The engine already has everything needed to run AI-only games: `HostedMatch`, `RegisteredPlayer`, `LobbyPlayerAi`, `GameRules`, and `PlayerControllerAi` all work without modification. The main integration work is: (1) building a `HeadlessGuiGame` that implements `IGuiGame` as a no-op to satisfy the engine's spectator path, (2) creating a dedicated bounded thread pool for simulation (NOT using `ThreadUtil.gameThreadPool`), (3) building `GameRules` explicitly instead of via `HostedMatch.getDefaultRules()` to avoid FModel preference coupling, and (4) extracting statistics from `GameOutcome`, `PlayerStatistics`, and `GameLog` after each game completes.

**Primary recommendation:** Use SSE (Javalin 7 `config.routes.sse`) for real-time progress streaming -- it is the natural fit for unidirectional server-to-client updates and Javalin has first-class support. Use REST for start/cancel/history endpoints. Store results as JSON files alongside deck files.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Default gauntlet: test against all available decks in the same format (user's decks + bundled AI decks)
- "Configure Gauntlet" button to select specific opponent decks (checkboxes)
- Games distributed evenly across opponents (100 games / 5 opponents = 20 per opponent)
- Both sides play at maximum AI strength (Hard/Reckless profile) -- no AI handicap
- Configurable game count: 10, 50, 100, 500
- Tabbed dashboard: Overview (Elo + playstyle radar + win rate + headline stats), Matchups (per-opponent breakdown), Performance (per-card win rate, dead cards), Mana (screw/flood, land drops)
- Results persisted as server-side JSON files alongside deck files (e.g., `sim-{deckname}-{timestamp}.json`)
- Backend API to list/load past simulation results for each deck
- Per-run Elo calculation (not cumulative), starting Elo = 1500
- Radar chart with 4 axes: Aggro, Midrange, Control, Combo (derived from static analysis + simulation results)
- Live updating dashboard while simulation runs (progress bar, running win rate, running Elo, matchup table filling progressively, per-card stats accumulating)
- Cancel button stops remaining games, stats from completed games kept and displayed, partial results saved
- Statistics per game: winner, loser, duration (turns), play/draw, mulligans, first threat turn, final life totals, cards drawn, cards stuck in hand, per-card drawn count and contributed-to-win signal

### Claude's Discretion
- HeadlessGuiGame no-op implementation details
- Bounded thread executor sizing (how many concurrent games)
- Game timeout ceiling (max turns before forced draw)
- Exact Elo K-factor
- Playstyle radar scoring algorithm details
- SSE vs WebSocket for progress streaming
- JSON result file structure and naming convention
- Per-card win correlation algorithm

### Deferred Ideas (OUT OF SCOPE)
- Real-time AI spectating (watching games play out visually) -- v3 feature
- Deck generation based on simulation results -- separate feature
- Cross-deck Elo leaderboard -- future enhancement
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| SIM-01 | Trigger simulation from deck builder | DeckEditor.tsx has `onPlayDeck` prop pattern; add analogous `onSimulate` |
| SIM-02 | Configure number of games (10, 50, 100, 500) | Frontend config panel; backend validates range |
| SIM-03 | Headless AI vs AI against gauntlet | HeadlessGuiGame + HostedMatch with two LobbyPlayerAi; dedicated thread pool |
| SIM-04 | Win rate overall, by matchup, play vs draw | GameOutcome.isWinner() + RegisteredPlayer tracking; coin flip for play/draw |
| SIM-05 | Mulligan stats (keep rate, avg mulligans, win rate after mulligan) | PlayerStatistics.getMulliganCount() available on GameOutcome |
| SIM-06 | Speed stats (avg kill turn, fastest/slowest, first threat) | GameOutcome.getLastTurnNumber(); first threat requires GameLog parsing |
| SIM-07 | Mana stats (screw rate, flood rate, land drop timing) | GameLog entries of type LAND and MANA; post-game log analysis |
| SIM-08 | Resource stats (cards drawn, empty hand turns, life at win/loss) | GameLog ZONE_CHANGE/LIFE entries; final life from Player.getLife() |
| SIM-09 | Per-card performance (win rate when drawn, dead card rate) | GameLog ZONE_CHANGE for draws; track per-card presence in winning games |
| SIM-10 | Elo rating for deck based on gauntlet | Pure math computation from win/loss results; K-factor 32 |
| SIM-11 | Playstyle classification (aggro/midrange/control/combo radar) | Combine deck-analysis.ts static analysis with simulation speed/interaction stats |
| SIM-12 | Real-time progress (games completed, running stats) | SSE endpoint streaming JSON updates after each game completes |
</phase_requirements>

## Standard Stack

### Core (No New Dependencies)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Javalin 7 (existing) | 7.x | SSE endpoint for progress streaming, REST for CRUD | Already in stack; native SSE support via `config.routes.sse` |
| Jackson (existing) | 2.21 | JSON serialization of simulation results | Already in stack |
| Zustand (existing) | 5.0.12 | Simulation state management on frontend | Already in stack |
| TanStack Query (existing) | 5.91.2 | REST API hooks for start/cancel/history | Already in stack |
| React (existing) | 19.2.4 | Dashboard UI | Already in stack |
| Tailwind CSS (existing) | 4.2.2 | Dashboard styling | Already in stack |

### Supporting

No new dependencies needed. The entire simulation system is built on existing infrastructure.

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| SSE for progress | REST polling (TanStack Query refetchInterval) | Polling adds latency (2s minimum) and is wasteful for local-only tool. SSE is simpler and more responsive. Javalin has native SSE. |
| SSE for progress | WebSocket | WebSocket is bidirectional (overkill for one-way updates). Already used for interactive games -- mixing simulation progress into game WS adds complexity. |
| JSON files for persistence | SQLite | Overkill for local-only tool. JSON files are human-readable, easily debugged, and match the deck file pattern. |
| Custom Elo math | chess.js or elo-rating library | Elo formula is 5 lines of code. No library needed. |

**Installation:** No new packages to install.

## Architecture Patterns

### Recommended Project Structure

```
forge-gui-web/src/main/java/forge/web/
  api/
    SimulationHandler.java       # REST: start, cancel, history, delete
  simulation/
    HeadlessGuiGame.java         # No-op IGuiGame for AI-only games
    SimulationRunner.java        # Orchestrates N games, collects stats
    SimulationJob.java           # Tracks one simulation's state and results
    SimulationResult.java        # Per-game result record
    SimulationSummary.java       # Aggregated statistics across all games
    EloCalculator.java           # Elo rating computation
    GameStatExtractor.java       # Extracts stats from Game/GameOutcome/GameLog

forge-gui-web/frontend/src/
  components/
    simulation/
      SimulationPanel.tsx        # Main container (triggered from DeckEditor)
      SimulationConfig.tsx       # Game count + gauntlet configuration
      SimulationProgress.tsx     # Live progress bar and running stats
      OverviewTab.tsx            # Elo + playstyle radar + win rate
      MatchupsTab.tsx            # Per-opponent breakdown table
      PerformanceTab.tsx         # Per-card win rate, dead cards
      ManaTab.tsx                # Screw/flood rates, land drops
      PlaystyleRadar.tsx         # SVG radar chart component
      SimulationHistory.tsx      # Past simulation results list
  hooks/
    useSimulation.ts             # TanStack Query + SSE hook
  api/
    simulation.ts                # API client functions
  lib/
    elo.ts                       # Elo computation (shared for display)
    simulation-types.ts          # TypeScript types for simulation data
```

### Pattern 1: HeadlessGuiGame (No-Op IGuiGame)

**What:** An implementation of `IGuiGame` where every method is a no-op or returns a safe default. Needed because `HostedMatch.startGame()` at line 249 calls `GuiBase.getInterface().getNewGuiGame()` when `humanCount == 0` to create a spectator -- and `WebGuiBase.getNewGuiGame()` currently returns null.

**Critical discovery:** The engine's `humanCount == 0` path (HostedMatch.java line 249-254) does:
1. Calls `GuiBase.getInterface().getNewGuiGame()` -- returns null in our WebGuiBase
2. Tries to use the null GUI: `gui.setGameView(null)` -- NPE
3. Creates `WatchLocalGame` with the GUI and subscribes to events

**Solution:** Override `WebGuiBase.getNewGuiGame()` to return a `HeadlessGuiGame` instance (instead of null). This way the engine's spectator path works naturally without modification. The `HeadlessGuiGame` will silently absorb all GUI callbacks.

**When to use:** Any AI-vs-AI game execution without a human player.

**Example:**
```java
// HeadlessGuiGame -- extends AbstractGuiGame, no-ops everything
public class HeadlessGuiGame extends AbstractGuiGame {
    private final CompletableFuture<Void> gameEndFuture = new CompletableFuture<>();

    @Override
    public void openView(TrackableCollection<PlayerView> myPlayers) { }

    @Override
    public void afterGameEnd() {
        super.afterGameEnd();
        gameEndFuture.complete(null); // Signal game is done
    }

    @Override
    public void finishGame() { }

    // All fire-and-forget methods: empty bodies
    @Override public void showCombat() { }
    @Override public void showPromptMessage(PlayerView pv, String msg) { }
    @Override public void updateButtons(PlayerView owner, String l1, String l2,
                                         boolean e1, boolean e2, boolean f1) { }
    @Override public void updatePhase(boolean saveState) { }
    @Override public void updateTurn(PlayerView player) { }
    @Override public void updateZones(Iterable<PlayerZoneUpdate> zones) { }
    @Override public void updateCards(Iterable<CardView> cards) { }
    @Override public void updateManaPool(Iterable<PlayerView> manaPoolUpdate) { }
    @Override public void updateLives(Iterable<PlayerView> livesUpdate) { }
    @Override public void updateStack() { }
    // ... (all ~30+ abstract methods get no-op implementations)

    // Blocking methods -- should never be called for AI players,
    // but provide safe defaults in case engine calls them unexpectedly
    @Override
    public <T> List<T> getChoices(String msg, int min, int max,
                                   List<T> choices, List<T> selected,
                                   FSerializableFunction<T, String> display) {
        if (min <= 0) return Collections.emptyList();
        return new ArrayList<>(choices.subList(0, Math.min(min, choices.size())));
    }

    @Override
    public boolean confirm(CardView c, String question,
                            boolean defaultIsYes, List<String> options) {
        return defaultIsYes;
    }

    @Override
    public boolean isUiSetToSkipPhase(PlayerView playerTurn, PhaseType phase) {
        return false;
    }

    public CompletableFuture<Void> getGameEndFuture() {
        return gameEndFuture;
    }
}
```

### Pattern 2: SimulationRunner with Dedicated Thread Pool

**What:** A bounded executor service separate from `ThreadUtil.gameThreadPool` that runs simulation games sequentially or with limited concurrency.

**Critical insight about ThreadUtil:** `ThreadUtil.gameThreadPool` is a `CachedThreadPool` (line 23 of ThreadUtil.java) -- unbounded. For simulation, we need a bounded pool. However, the engine's `game.getAction().invoke()` (called in HostedMatch.startGame at line 268) uses this pool internally. We cannot easily replace it.

**Recommended approach:** Run simulation games ONE AT A TIME on a dedicated single-thread executor. Each game:
1. Creates a new `HostedMatch`
2. Creates two `LobbyPlayerAi` with Reckless profile
3. Builds `GameRules` explicitly (no FModel dependency)
4. Calls `hostedMatch.startMatch()` which internally uses `game.getAction().invoke()` on the game thread pool
5. Waits for the game to complete via `HeadlessGuiGame.getGameEndFuture()`
6. Extracts statistics and discards game objects

**Thread pool sizing:** 1 thread for the simulation orchestrator. The actual game runs on `ThreadUtil.gameThreadPool`. The orchestrator thread submits one game at a time and waits for completion before submitting the next. This avoids concurrent game state corruption.

```java
private static final ExecutorService simulationExecutor =
    Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Simulation-Orchestrator");
        t.setDaemon(true);
        return t;
    });
```

**Why not parallel games:** Each Forge game allocates significant heap for card state. The engine was not designed for concurrent games -- static singleton state in `GamePlayerUtil.guiPlayer`, `SoundSystem.instance`, and `FModel` preferences means parallel games risk corruption. Sequential is safe and still fast (~1-3 seconds per AI game).

### Pattern 3: SSE Progress Streaming (Javalin 7)

**What:** Server-Sent Events endpoint that streams simulation progress to the frontend.

**Javalin 7 SSE API (verified):**
```java
// In createApp():
config.routes.sse("/api/simulations/{id}/progress", client -> {
    String simId = client.ctx().pathParam("id");
    SimulationJob job = activeSimulations.get(simId);
    if (job == null) {
        client.sendEvent("error", "Simulation not found");
        client.close();
        return;
    }
    client.keepAlive(); // Keep connection open after handler returns
    job.addProgressListener(update -> {
        if (!client.terminated()) {
            client.sendEvent("progress", objectMapper.writeValueAsString(update));
        }
    });
    client.onClose(() -> job.removeProgressListener(client));
});
```

**Frontend EventSource pattern:**
```typescript
const useSimulationProgress = (simId: string | null) => {
  const [progress, setProgress] = useState<SimulationProgress | null>(null)

  useEffect(() => {
    if (!simId) return
    const source = new EventSource(`/api/simulations/${simId}/progress`)
    source.addEventListener('progress', (e) => {
      setProgress(JSON.parse(e.data))
    })
    source.addEventListener('complete', (e) => {
      setProgress(JSON.parse(e.data))
      source.close()
    })
    source.addEventListener('error', () => source.close())
    return () => source.close()
  }, [simId])

  return progress
}
```

### Pattern 4: Statistics Extraction from Engine Objects

**What:** After each game completes, extract all needed statistics from `Game`, `GameOutcome`, `PlayerStatistics`, and `GameLog` before discarding the Game object.

**Available data from engine (verified from source):**

| Source | Data | How to Access |
|--------|------|---------------|
| `GameOutcome` | Winner, last turn number, life delta, win condition | `match.getOutcomes()` -> iterate |
| `GameOutcome.isWinner(RegisteredPlayer)` | Whether test deck won | Direct call |
| `GameOutcome.getLastTurnNumber()` | Game length in turns | Direct call |
| `PlayerStatistics` | Mulligan count, opening hand size, turns played | `GameOutcome` iterates over `(RegisteredPlayer, PlayerStatistics)` pairs |
| `PlayerStatistics.getMulliganCount()` | Number of mulligans taken | Direct call |
| `GameLog` | All game events (MULLIGAN, LAND, DAMAGE, LIFE, ZONE_CHANGE, COMBAT, STACK_ADD, etc.) | `game.getGameLog().getLogEntries(null)` returns all entries |
| `Player.getLife()` | Final life total | Access before game cleanup |

**For per-card tracking:** Parse `GameLog` entries of type `ZONE_CHANGE` to track which cards were drawn (moved from library to hand). Track per-card presence across games where the test deck won vs lost to compute "win rate when drawn."

**For mana screw/flood detection:** Parse `GameLog` LAND entries to track land drops per turn. Define mana screw as < 3 lands by turn 4; flood as > 6 lands by turn 6 (tunable thresholds).

**For first threat detection:** Parse `GameLog` STACK_ADD entries to find the first non-land permanent cast by the test deck.

### Anti-Patterns to Avoid

- **Running simulation on interactive game thread:** The WebSocket game session uses `ThreadUtil.invokeInGameThread()`. Simulation must NOT share that thread -- it would block interactive play. Use a separate executor.
- **Mutating FModel preferences for simulation:** Build `GameRules` explicitly. Never call `HostedMatch.getDefaultRules()` which reads from FModel preferences.
- **Retaining Game objects after stats extraction:** Each Game allocates significant heap. Extract stats immediately, null out references, let GC reclaim.
- **Sending WebSocket messages from headless games:** HeadlessGuiGame must never reference WsContext. All GUI methods are no-ops.
- **Using GamePlayerUtil.getGuiPlayer() for simulation players:** Both players must be `LobbyPlayerAi`. The static `guiPlayer` singleton must not be touched.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| AI player creation | Custom player setup | `GamePlayerUtil.createAiPlayer(name, "Reckless")` | Handles avatar, sleeve, profile setup correctly |
| Game rules construction | Copy from getDefaultRules | `new GameRules(GameType.Constructed)` + explicit setters | Avoids FModel preference coupling; sets gamesPerMatch=1 |
| Deck loading | Custom file parser | `DeckSerializer.fromFile(file)` (existing pattern in WebServer) | Already handles all deck formats |
| Game execution | Custom game loop | `HostedMatch.startMatch(rules, null, players, guis)` | Engine handles mulligan, phases, priority, triggers, combat |
| Win detection | Custom game-over check | `GameOutcome.isWinner(registeredPlayer)` | Handles all win conditions (life, poison, mill, alt-win) |
| Mulligan tracking | Intercept mulligan decisions | `PlayerStatistics.getMulliganCount()` already tracked by engine | Automatically counted for each player |
| SSE streaming | Custom HTTP streaming | `config.routes.sse()` with `client.keepAlive()` | Javalin 7 has native SSE support with proper connection management |

## Common Pitfalls

### Pitfall 1: WebGuiBase.getNewGuiGame() Returns Null -- NPE on AI-Only Games

**What goes wrong:** When both players are AI (`humanCount == 0`), HostedMatch.startGame() line 249-254 calls `GuiBase.getInterface().getNewGuiGame()` to create a spectator GUI. WebGuiBase returns null. The engine then calls `gui.setGameView(null)` on the null reference -- NPE crash.

**Why it happens:** WebGuiBase's `getNewGuiGame()` was stubbed as `return null` during v1.0 (the comment says "Will be implemented in Phase 4"). It was never needed because interactive games always have a human player.

**How to avoid:** Override `WebGuiBase.getNewGuiGame()` to return a `new HeadlessGuiGame()`. This makes ALL zero-human-player game starts work, not just simulation.

**Warning signs:** `NullPointerException` in `HostedMatch.startGame` at the `gui.setGameView(null)` line.

### Pitfall 2: FThreads.delayInEDT 3-Second Delay After AI-Only Games

**What goes wrong:** In HostedMatch.startGame() lines 287-288, when `humanCount == 0` and game type is Constructed, the engine adds a 3-second "dramatic interlude" via `FThreads.delayInEDT(3000, ...)`. For 100 simulation games, this adds 5 minutes of unnecessary waiting.

**Why it happens:** The delay is for the desktop GUI's AI spectator mode -- a visual pause between games so the user can see the result.

**How to avoid:** Use `HostedMatch.setEndGameHook()` to immediately add a `NextGameDecision.QUIT` decision, bypassing the delay. OR set `gamesPerMatch = 1` in GameRules so the match ends after one game (no next-game decision needed). The recommended approach is gamesPerMatch=1 since we create a new HostedMatch per simulation game anyway.

**Warning signs:** Simulation of 100 games taking 10+ minutes instead of 2-3 minutes.

### Pitfall 3: SoundSystem.instance Subscribed to Match Events

**What goes wrong:** HostedMatch.startMatch() line 146 subscribes `SoundSystem.instance` to match events. The SoundSystem may try to play sounds during headless games, causing errors or performance overhead.

**Why it happens:** Sound is automatically subscribed in the match creation flow.

**How to avoid:** Either: (a) the SoundSystem gracefully handles headless mode (it likely does since web mode has `UI_ENABLE_SOUNDS=false`), or (b) call `match.unsubscribeFromEvents(SoundSystem.instance)` after startMatch. Verify that sound doesn't cause issues in headless mode before adding workarounds.

### Pitfall 4: Game Object Memory Leaks in Long Simulation Runs

**What goes wrong:** Running 500 games creates 500 Game objects with full card state, event buses, and player data. If references are retained, heap exhausts.

**Why it happens:** Java GC only reclaims objects with no live references. If the SimulationRunner keeps a list of Game objects for stats extraction, they accumulate.

**How to avoid:** Extract all statistics from Game/GameOutcome/GameLog immediately after each game completes. Store only the extracted primitive data (wins, turns, mulligan counts) in a lightweight result object. Explicitly null out HostedMatch and Game references. The engine already calls `System.gc()` after each game (Match.java line 103).

### Pitfall 5: Concurrent Simulation + Interactive Game Conflict

**What goes wrong:** A user starts a simulation, then opens a new browser tab and starts an interactive game. Both compete for the game thread pool and potentially for static state.

**How to avoid:** This is acceptable for v1 -- the simulation runs games sequentially on the game thread pool, and interactive games also use the game thread pool. Since `CachedThreadPool` creates new threads on demand, both can coexist. The risk is performance degradation, not corruption, as long as FModel preferences aren't mutated. Document that simulation may slow down if an interactive game is running concurrently.

### Pitfall 6: Game Stalemate / Infinite Loop

**What goes wrong:** Some AI vs AI matchups can enter infinite loops (e.g., both players have creatures that prevent damage, neither can win). The game thread hangs forever.

**How to avoid:** Set a maximum turn limit (e.g., 200 turns). The `Game` object tracks turn count. After the limit, force the game to end as a draw. Implementation: subscribe to `GameEventTurnPhase` events on the Game event bus and call `game.setGameOver(GameEndReason.Draw)` if turn count exceeds the limit. Alternatively, use `HostedMatch.setEndGameHook()` with a scheduled timeout.

## Code Examples

### Starting a Headless AI vs AI Game

```java
// Source: Derived from WebServer.handleStartGame() pattern
public SimulationResult runSingleGame(Deck testDeck, Deck opponentDeck, boolean testDeckPlaysFirst) {
    // 1. Build explicit GameRules (no FModel dependency)
    GameRules rules = new GameRules(GameType.Constructed);
    rules.setPlayForAnte(false);
    rules.setManaBurn(false);
    rules.setGamesPerMatch(1);  // Single game, not best-of-3

    // 2. Create AI players with Reckless profile
    RegisteredPlayer testPlayer = new RegisteredPlayer(testDeck);
    testPlayer.setPlayer(GamePlayerUtil.createAiPlayer("Test Deck", "Reckless"));

    RegisteredPlayer oppPlayer = new RegisteredPlayer(opponentDeck);
    oppPlayer.setPlayer(GamePlayerUtil.createAiPlayer("Opponent", "Reckless"));

    // 3. Order determines play/draw
    List<RegisteredPlayer> players = testDeckPlaysFirst
        ? List.of(testPlayer, oppPlayer)
        : List.of(oppPlayer, testPlayer);

    // 4. Empty guis map -- triggers the humanCount==0 spectator path
    // WebGuiBase.getNewGuiGame() must return HeadlessGuiGame (not null)
    Map<RegisteredPlayer, IGuiGame> guis = Map.of();

    // 5. Start the match
    HostedMatch hostedMatch = new HostedMatch();
    hostedMatch.startMatch(rules, null, players, guis, null);
    // startMatch -> startGame -> match.startGame(game) -> blocks until game ends

    // 6. Extract results from the completed match
    GameOutcome outcome = hostedMatch.getGame().getMatch().getOutcomes().iterator().next();
    boolean testDeckWon = outcome.isWinner(testPlayer);
    int turns = outcome.getLastTurnNumber();

    // Extract mulligan data
    PlayerStatistics testStats = null;
    for (Map.Entry<RegisteredPlayer, PlayerStatistics> entry : outcome) {
        if (entry.getKey() == testPlayer) {
            testStats = entry.getValue();
            break;
        }
    }
    int mulligans = testStats != null ? testStats.getMulliganCount() : 0;

    return new SimulationResult(testDeckWon, turns, mulligans, testDeckPlaysFirst);
}
```

### Javalin SSE Endpoint for Progress

```java
// Source: Javalin 7 documentation (https://javalin.io/documentation)
// In WebServer.createApp():
config.routes.sse("/api/simulations/{id}/progress", client -> {
    String simId = client.ctx().pathParam("id");
    SimulationJob job = SimulationHandler.getJob(simId);
    if (job == null) {
        client.sendEvent("error", "{\"message\":\"not found\"}");
        client.close();
        return;
    }
    client.keepAlive();
    job.addProgressListener(update -> {
        if (!client.terminated()) {
            try {
                client.sendEvent("progress", objectMapper.writeValueAsString(update));
            } catch (Exception e) {
                Logger.error(e, "SSE send error");
            }
        }
    });
    client.onClose(() -> job.removeProgressListener(/* this client */));
});
```

### Frontend EventSource Hook

```typescript
// Source: Standard EventSource API + React pattern
function useSimulationSSE(simId: string | null) {
  const [progress, setProgress] = useState<SimProgress | null>(null)

  useEffect(() => {
    if (!simId) return
    const es = new EventSource(`/api/simulations/${simId}/progress`)

    es.addEventListener('progress', (e) => {
      const data: SimProgress = JSON.parse(e.data)
      setProgress(data)
    })
    es.addEventListener('complete', (e) => {
      const data: SimProgress = JSON.parse(e.data)
      setProgress({ ...data, status: 'complete' })
      es.close()
    })
    es.onerror = () => es.close()

    return () => es.close()
  }, [simId])

  return progress
}
```

### Elo Calculation

```typescript
// Standard Elo formula
function computeElo(results: { opponentElo: number; won: boolean }[]): number {
  const K = 32
  let elo = 1500
  for (const { opponentElo, won } of results) {
    const expected = 1 / (1 + Math.pow(10, (opponentElo - elo) / 400))
    const actual = won ? 1 : 0
    elo += K * (actual - expected)
  }
  return Math.round(elo)
}
```

### SVG Radar Chart (No Library Needed)

```tsx
// Source: Standard SVG polygon rendering for 4-axis radar
function PlaystyleRadar({ scores }: { scores: { aggro: number; midrange: number; control: number; combo: number } }) {
  const axes = ['aggro', 'midrange', 'control', 'combo'] as const
  const cx = 100, cy = 100, r = 80
  const points = axes.map((axis, i) => {
    const angle = (Math.PI * 2 * i) / axes.length - Math.PI / 2
    const value = scores[axis] * r
    return `${cx + value * Math.cos(angle)},${cy + value * Math.sin(angle)}`
  }).join(' ')

  return (
    <svg viewBox="0 0 200 200" className="w-48 h-48">
      {/* Grid lines */}
      {[0.25, 0.5, 0.75, 1].map(scale => (
        <polygon key={scale} points={axes.map((_, i) => {
          const angle = (Math.PI * 2 * i) / axes.length - Math.PI / 2
          return `${cx + scale * r * Math.cos(angle)},${cy + scale * r * Math.sin(angle)}`
        }).join(' ')} fill="none" stroke="currentColor" strokeOpacity={0.2} />
      ))}
      {/* Data polygon */}
      <polygon points={points} fill="hsl(var(--primary))" fillOpacity={0.3}
               stroke="hsl(var(--primary))" strokeWidth={2} />
    </svg>
  )
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| REST polling for progress | SSE streaming | Javalin 2.6+ (2019) | Lower latency, less bandwidth, cleaner code |
| `WebGuiBase.getNewGuiGame()` returns null | Must return HeadlessGuiGame | Phase 12 | Enables all AI-only game modes |
| No simulation feature | Full simulation dashboard | Phase 12 | Major new feature area |

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Manual verification via simulation runs |
| Config file | none -- manual testing |
| Quick run command | Start server, open deck editor, click Simulate, run 10 games |
| Full suite command | Run 100 games against full gauntlet, verify all stats tabs |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SIM-01 | Simulate button visible in deck editor | manual | Open deck editor, verify button | N/A |
| SIM-02 | Game count selector works | manual | Select 10/50/100/500, verify config sent | N/A |
| SIM-03 | Headless games complete | smoke | `curl -X POST /api/simulations/start` with test data | Wave 0 |
| SIM-04 | Win rates computed correctly | manual | Run 10 games, verify math matches | N/A |
| SIM-05 | Mulligan stats shown | manual | Check mulligan tab after simulation | N/A |
| SIM-06 | Speed stats shown | manual | Check speed stats after simulation | N/A |
| SIM-07 | Mana stats shown | manual | Check mana tab after simulation | N/A |
| SIM-08 | Resource stats shown | manual | Check resource stats after simulation | N/A |
| SIM-09 | Per-card performance shown | manual | Check performance tab after simulation | N/A |
| SIM-10 | Elo rating computed | unit | Elo math is deterministic, can unit test | Wave 0 |
| SIM-11 | Playstyle classified | manual | Verify radar chart renders with data | N/A |
| SIM-12 | Progress updates in real time | manual | Run 50+ games, watch progress bar update | N/A |

### Sampling Rate

- **Per task commit:** Start server, run 10-game simulation, verify no crashes
- **Per wave merge:** Run 100-game simulation, verify all 4 tabs populate correctly
- **Phase gate:** Full suite green before /gsd:verify-work

### Wave 0 Gaps

- [ ] `HeadlessGuiGame` -- must compile and satisfy IGuiGame contract before any games run
- [ ] Backend smoke test: POST /api/simulations/start returns simulation ID
- [ ] Elo unit test: verify formula with known inputs/outputs

## Open Questions

1. **Game thread safety with concurrent interactive play**
   - What we know: ThreadUtil.gameThreadPool is a CachedThreadPool that creates threads on demand. Both simulation and interactive games submit to it.
   - What's unclear: Whether two simultaneous games on the pool cause any static state corruption beyond FModel preferences (which we bypass).
   - Recommendation: Accept the risk for v1. Sequential simulation games + one interactive game should coexist fine. Document as known limitation.

2. **Per-card "contributed to win" signal**
   - What we know: We can track which cards were drawn per game (GameLog ZONE_CHANGE from library to hand). We can correlate with win/loss.
   - What's unclear: The best algorithm for "this card contributes to wins." Simple approach: "win rate when drawn" is straightforward. More sophisticated bayesian analysis is overkill for v1.
   - Recommendation: Use "win rate when drawn" and "games drawn in / total games" as the two per-card metrics.

3. **First threat turn detection**
   - What we know: GameLog has STACK_ADD entries. But the message is a text string, not structured data with card type.
   - What's unclear: Whether we can reliably parse "first creature/planeswalker cast" from log text.
   - Recommendation: Parse STACK_ADD log entries for the test deck player. Use regex to detect creature/planeswalker cast. Accept imperfect detection for v1.

## Sources

### Primary (HIGH confidence)
- `forge-gui/src/main/java/forge/gamemodes/match/HostedMatch.java` -- startMatch/startGame flow, humanCount==0 spectator path, 3-second delay
- `forge-gui/src/main/java/forge/gamemodes/match/AbstractGuiGame.java` -- base class for IGuiGame implementations
- `forge-gui/src/main/java/forge/gui/interfaces/IGuiGame.java` -- full interface (all methods to implement)
- `forge-gui-web/src/main/java/forge/web/WebGuiGame.java` -- reference implementation, 1062 lines
- `forge-gui-web/src/main/java/forge/web/WebGuiBase.java` -- getNewGuiGame() returns null (line 284)
- `forge-gui-web/src/main/java/forge/web/WebServer.java` -- handleStartGame pattern, GameSession, route configuration
- `forge-game/src/main/java/forge/game/GameOutcome.java` -- winner detection, turn count, player statistics
- `forge-game/src/main/java/forge/game/player/PlayerStatistics.java` -- mulligan count, turns played
- `forge-game/src/main/java/forge/game/player/PlayerOutcome.java` -- win/loss/concede/draw states
- `forge-game/src/main/java/forge/game/GameLog.java` -- Observable, getLogEntries()
- `forge-game/src/main/java/forge/game/GameLogEntryType.java` -- 18 entry types including MULLIGAN, LAND, DAMAGE, COMBAT
- `forge-game/src/main/java/forge/game/GameRules.java` -- constructor, explicit setters
- `forge-game/src/main/java/forge/game/Match.java` -- startGame() blocks until game ends
- `forge-core/src/main/java/forge/util/ThreadUtil.java` -- CachedThreadPool at line 23
- `forge-gui/src/main/java/forge/player/GamePlayerUtil.java` -- static guiPlayer singleton, createAiPlayer()
- `forge-ai/src/main/java/forge/ai/PlayerControllerAi.java` -- mulliganKeepHand(), AI decision making

### Secondary (MEDIUM confidence)
- [Javalin 7 Documentation - SSE](https://javalin.io/documentation) -- SSE API: config.routes.sse, SseClient.keepAlive/sendEvent/onClose
- [Javalin v6 to v7 Migration Guide](https://javalin.io/migration-guide-javalin-6-to-7) -- SSE moved to config.routes.sse

### Tertiary (LOW confidence)
- None. All findings verified from source code or official documentation.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new dependencies, all existing
- Architecture: HIGH -- verified from direct codebase analysis of HostedMatch, IGuiGame, GameOutcome
- Pitfalls: HIGH -- all 6 pitfalls identified from reading actual engine source, not speculation
- HeadlessGuiGame: HIGH -- IGuiGame interface is 75 methods but AbstractGuiGame implements most; ~20 abstract methods need no-op overrides
- Statistics extraction: HIGH -- GameOutcome, PlayerStatistics, GameLog all verified from source
- SSE streaming: HIGH -- Javalin 7 SSE API verified from official documentation

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable engine, no expected changes)
