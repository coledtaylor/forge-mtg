# Pitfalls Research

**Domain:** Adding headless simulation, Jumpstart format, gameplay UX, and card image quality to existing Forge Web Client
**Researched:** 2026-03-20
**Confidence:** HIGH (derived from direct codebase analysis of Forge engine internals + v1.0 architecture knowledge)

## Critical Pitfalls

### Pitfall 1: GamePlayerUtil.guiPlayer is a Static Singleton -- Breaks Parallel Headless Games

**What goes wrong:**
`GamePlayerUtil.guiPlayer` is a `private static final LobbyPlayer` -- a single shared instance across the entire JVM. When running headless AI vs AI simulations, if any code path references `GamePlayerUtil.getGuiPlayer()`, all concurrent games share the same LobbyPlayer identity. This causes the engine to confuse which game a player belongs to, corrupt game state, or deadlock when multiple games try to check `isGuiPlayer()`.

**Why it happens:**
The simulation code naturally tries to reuse the existing `handleStartGame` flow which calls `humanPlayer.setPlayer(GamePlayerUtil.getGuiPlayer())`. For headless AI vs AI, you might skip the human player, but `HostedMatch.startGame()` at line 249 checks `humanCount == 0` and calls `GuiBase.getInterface().getNewGuiGame()` to create a spectator -- which hits the singleton GUI interface.

**How to avoid:**
For headless simulation, do NOT use the `HostedMatch` game-start flow that exists for interactive games. Instead:
1. Create two `LobbyPlayerAi` instances per simulation game (no human player at all)
2. Pass an empty `guis` map: `Map.of()` -- this triggers the spectator/watch path
3. OR create a minimal `HeadlessGuiGame` that implements `IGuiGame` with no-ops for all methods (no WebSocket, no WsContext)
4. Never call `GamePlayerUtil.getGuiPlayer()` in simulation code

**Warning signs:**
- `NullPointerException` when headless games try to send WebSocket messages
- Games blocking forever on `CompletableFuture.get()` because nobody is there to respond
- Intermittent wrong-player-wins results when running multiple simulations

**Phase to address:**
Deck Simulation phase -- must be the first design decision when building the headless runner

---

### Pitfall 2: ThreadUtil.gameThreadPool is a Shared CachedThreadPool -- Unbounded Thread Growth

**What goes wrong:**
`ThreadUtil.invokeInGameThread()` uses `Executors.newCachedThreadPool()` (line 23 of ThreadUtil.java). A cached thread pool creates new threads on demand with no upper bound. Running 50+ headless simulations concurrently will spawn 50+ game threads, each consuming significant memory (default ~1MB stack per thread) plus the game state objects (card database views, zone contents, etc.). The JVM runs out of memory or thrashes with excessive context switching.

**Why it happens:**
The pool was designed for the desktop GUI where at most 1-2 games run simultaneously. Simulation wants to run many games in parallel for statistical significance, and the natural approach is to call `ThreadUtil.invokeInGameThread()` for each one.

**How to avoid:**
Do NOT use `ThreadUtil.invokeInGameThread()` for simulation games. Instead:
1. Create a dedicated `ExecutorService` with a bounded thread pool sized to CPU cores (e.g., `Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())`)
2. Queue simulation games and run them in batches
3. Track memory per game -- each Forge game allocates significant heap for card state tracking. Profile to find the safe concurrency level (likely 4-8 games per 4GB heap)
4. Run simulations sequentially if memory is tight -- even sequential runs of 100 games complete in minutes

**Warning signs:**
- `OutOfMemoryError: unable to create new native thread`
- JVM heap exhaustion (`OutOfMemoryError: Java heap space`)
- System becoming unresponsive during simulation runs
- Thread count in JMX/logs growing linearly with simulation count

**Phase to address:**
Deck Simulation phase -- thread pool design is foundational

---

### Pitfall 3: FModel Singleton State Corruption During Concurrent Games

**What goes wrong:**
`FModel` is initialized once at server startup (WebServer.java line 66) and holds static references to preferences, card database, and game formats. Multiple concurrent games read from `FModel.getPreferences()` -- which is safe for reads. But `HostedMatch.getDefaultRules()` reads preferences like `FPref.UI_ANTE`, `FPref.UI_MANABURN`, and `FPref.UI_MATCHES_PER_GAME` to build `GameRules`. If simulation games need different rules (e.g., no ante, different games-per-match), modifying preferences globally would corrupt running interactive games.

**Why it happens:**
Forge's preference system was designed for a single-user desktop app. There's no concept of per-session preferences.

**How to avoid:**
Never mutate `FModel` preferences for simulation. Instead:
1. Build `GameRules` manually for simulation games, bypassing `HostedMatch.getDefaultRules()`
2. Set rules directly: `new GameRules(GameType.Constructed)` then configure mana burn, ante, etc. explicitly
3. Call `HostedMatch.startMatch(gameRules, ...)` with the explicit rules overload, not the `GameType` overload that reads FModel

**Warning signs:**
- Interactive game suddenly has different rules than expected
- Simulation results inconsistent because preferences changed mid-batch

**Phase to address:**
Deck Simulation phase -- rule construction must not touch global state

---

### Pitfall 4: No Jumpstart GameType Exists in Forge Engine

**What goes wrong:**
The `GameType` enum (forge-game) has no `Jumpstart` entry. The current WebServer (line 229) maps format strings to only `Commander` or `Constructed`. If you send `"Jumpstart"` as the format, it silently falls through to `GameType.Constructed`, which uses `DeckFormat.Constructed` -- a 60-card minimum format. A merged Jumpstart deck (two 20-card packs = 40 cards) would fail validation under Constructed rules.

**Why it happens:**
Jumpstart is not a separate game type in Forge's engine -- it's a deck construction method, not a gameplay variant. The actual game rules are identical to Constructed. The confusion comes from the v1.0 frontend treating "Jumpstart" as a format string alongside "Commander" and "Standard."

**How to avoid:**
1. Do NOT try to add a new `GameType.Jumpstart` -- that's modifying the core engine
2. Use `GameType.Constructed` for Jumpstart games but bypass deck size validation
3. Build the Jumpstart flow as a UI-only concept: pack selection merges two 20-card packs into a 40-card deck, then start a `Constructed` game with that merged deck
4. For format validation (the v1.0 bug), treat "Jumpstart" as a UI label that maps to no `GameFormat` -- skip validation or validate pack composition instead of standard constructed rules

**Warning signs:**
- 400 errors when validating Jumpstart decks (this is the existing v1.0 bug)
- Merged 40-card decks rejected by engine for being under 60 cards
- Attempting to modify forge-game module for a UI concern

**Phase to address:**
Jumpstart Format phase -- pack merging and game start must be designed together

---

### Pitfall 5: Undo Only Works on the Stack, Not on Arbitrary Game Actions

**What goes wrong:**
Developers assume "undo" means reversing any game action. In Forge, `GameStack.undo()` only reverses the last spell/ability put on the stack, and only if the current player has priority and the stack item hasn't resolved. You cannot undo land plays (separate system), combat declarations, or resolved spells. Promising users a general "undo" creates expectations the engine cannot fulfill.

**Why it happens:**
MTG rules are inherently stateful and many actions are irreversible once they happen (triggers fire, state-based actions occur). Forge has `EXPERIMENTAL_RESTORE_SNAPSHOT` (checked in HostedMatch line 167) which is a separate, experimental save/restore system -- but it's explicitly experimental and not reliable for production use.

**How to avoid:**
1. Label the feature "Undo Last Spell" not "Undo" -- scope the UX precisely to what the engine supports
2. Only show the undo button when `canUndoLastAction()` returns true (priority player + stack has undoable items)
3. Do NOT expose `EXPERIMENTAL_RESTORE_SNAPSHOT` -- it's labeled experimental in the engine for a reason
4. Wire the undo button to `IGameController.undoLastAction()` which already has proper guards
5. Disable undo during AI turns, combat, and resolution phases

**Warning signs:**
- Users clicking undo after combat and nothing happening
- Undo button always visible but usually non-functional
- Crashes when undoing complex stack interactions

**Phase to address:**
Gameplay UX phase -- undo button visibility must be tightly coupled to engine state

---

### Pitfall 6: Priority System Display Without Showing What's Actually Happening

**What goes wrong:**
The MTG priority system is the most confusing part of digital MTG for players. The current web client sends raw prompt messages from the engine (e.g., "Select action for Human") without contextualizing what phase it is, why the player has priority, or what the default action means. Adding "priority indicators" without restructuring how prompts display will result in more UI elements that still confuse players.

**Why it happens:**
The engine's prompt system (`showPromptMessage`, `updateButtons`) sends text strings designed for the desktop GUI which has visible phase bars, static player areas, and tooltips. The web client receives these strings verbatim but lacks the spatial context that makes them meaningful.

**How to avoid:**
1. Parse `PHASE_UPDATE` messages to show a clear phase indicator (Upkeep / Draw / Main 1 / Combat / Main 2 / End)
2. Combine `BUTTON_UPDATE` labels with phase context: instead of raw "OK"/"Cancel", show "Pass Priority (Main Phase 1)" and "Hold Priority"
3. Highlight the active player prominently and show whose turn it is
4. Implement auto-pass for phases where the player has no actions -- this eliminates most of the priority confusion
5. Use `isUiSetToSkipPhase()` (currently hardcoded to `return false` in WebGuiGame line 868) as the mechanism for auto-yield

**Warning signs:**
- Users not understanding why they keep getting prompted during opponent's turn
- "OK" button with no context about what it confirms
- Players unable to distinguish "pass priority" from "end turn"

**Phase to address:**
Gameplay UX phase -- this is the core of the UX overhaul and should be addressed before auto-yield

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Running simulation on the shared `gameThreadPool` | No new thread pool to manage | Unbounded thread growth, OOM crashes, interferes with interactive games | Never -- always use a dedicated bounded pool |
| Using name-based Scryfall URLs for game cards | Works without adding setCode/collectorNumber to game DTOs | Slower image loads (Scryfall redirect), wrong printing shown, no control over art quality | Only during v1.0 -- must add set identifiers to CardDto for v2.0 |
| Hardcoding `isUiSetToSkipPhase` to `return false` | Simplifies v1.0 implementation | Every phase prompts the user, terrible UX for experienced players | Only in v1.0 -- must implement auto-yield in v2.0 |
| Skipping format validation for Jumpstart | Avoids the GameFormat mapping problem | Users can build invalid Jumpstart decks, no guardrails | Never -- validate pack composition at the UI level instead |
| Mapping all non-Commander formats to `GameType.Constructed` | Simple two-way branch | Jumpstart 40-card decks may hit validation issues in engine | Acceptable IF the engine doesn't enforce deck size for `GameType.Constructed` (it does via `DeckFormat.Constructed`) |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Scryfall Image API | Hitting the API for every card render without caching, exceeding 10 req/s rate limit | Browser caches images by URL automatically; ensure stable URLs (same set+collector). The real gotcha is changing URLs (e.g., switching from name-based to set-based) invalidates the entire browser cache |
| Scryfall set code mapping | Forge set codes don't always match Scryfall set codes (e.g., Forge uses "M21" but some promotional sets differ) | Use Forge's `setCode` field directly in Scryfall URLs -- Scryfall accepts most standard set codes. For mismatches, maintain a small lookup table of known exceptions |
| Scryfall collector numbers | Collector numbers can contain letters (e.g., "123a", "456b" for variants) and must be URL-encoded | Always use `encodeURIComponent()` on collector numbers (already done in scryfall.ts) |
| Scryfall language filtering | Requesting English-only by adding `lang=en` parameter doesn't work with the image redirect endpoint | The `/cards/{set}/{collector}` endpoint already returns English by default. Only add language filtering if using the search API |
| Forge `DeckSerializer` | Deserializing deck files on the game thread can block game progression | Deck loading for simulation should happen before game thread submission, not inside the game thread callback |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Sending full `GameStateDto` on every zone/card update | UI stutter, WebSocket message queue grows, client GC pauses | WebGuiGame currently sends full GAME_STATE on every `updateZones`, `updateCards`, `updateManaPool`, `updateLives` call. For simulation, this is catastrophic -- hundreds of state snapshots per game. Headless games must skip all WebSocket sends | Noticeable at 10+ games, critical at 50+ |
| Creating `ObjectMapper` per serialization | Memory allocation, class metadata overhead | Use the shared `objectMapper` instance (already done in WebServer). For simulation, skip JSON serialization entirely | N/A if headless path avoids serialization |
| Scryfall API rate limiting (10 requests/second) | Images fail to load with 429 status, then all subsequent requests fail for a cooldown period | Browser image tags naturally batch and cache. The danger is changing from name-based to set-based URLs for ALL existing cards simultaneously -- the browser re-fetches everything. Roll out URL changes incrementally or pre-warm the cache | Hits at ~100 unique card images loaded in <10 seconds (rapid scrolling through deck list) |
| Simulation memory: each Game object retains full card state | Heap exhaustion after N games without GC | Explicitly null out game references after collecting results. Do not retain Game objects in a results list -- extract win/loss/stats immediately and discard | ~20-30 concurrent games on 4GB heap |
| Auto-yield checking on every priority pass | Slows down the game loop if checking involves UI round-trips | `isUiSetToSkipPhase` must be a pure local check (no WebSocket round-trip). Store yield preferences in a local `Set<PhaseType>` on the `WebGuiGame` instance | Would be immediate if implemented as a sendAndWait |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Showing undo button at all times | Users click it when it can't work, feel the feature is broken | Only render undo button when engine reports `canUndoLastAction() == true`. Send this state as part of BUTTON_UPDATE |
| Priority prompts with no phase context | Users don't know what they're responding to, spam OK | Combine phase name + prompt message. "Main Phase 1: Cast spells or pass priority" vs just "Select action" |
| Auto-yield as a global toggle | Either all phases are skipped or none are, no granularity | Per-phase yield toggles: users want to auto-pass during opponent's upkeep but hold during their own main phase |
| Jumpstart pack selection showing raw card lists | Overwhelming, users can't evaluate 20-card packs at a glance | Show pack name/theme, key cards preview, color identity summary. Let users compare packs side by side |
| Simulation results as raw numbers | Win rate alone doesn't help deck improvement | Show per-matchup results, average game length, common loss patterns (mana screw/flood detection via land-to-spell ratio at loss) |
| Keyboard shortcuts conflicting with browser defaults | Ctrl+Z triggers browser undo instead of game undo | Use non-conflicting keys (Space for OK, Escape for Cancel, Z for undo without Ctrl). Prevent default on game-focused keys when game board has focus |
| Game log as a flat text dump | Unreadable wall of text, no way to find key moments | Structured log entries with turn/phase markers, expandable sections, card name highlighting, and filtering by type (combat, spells, triggers) |

## "Looks Done But Isn't" Checklist

- [ ] **Headless simulation:** Often missing proper cleanup -- verify Game objects are GC'd after results are extracted (no lingering event bus subscriptions holding references)
- [ ] **Headless simulation:** Often missing timeout handling -- verify games that deadlock (infinite loops in AI) are killed after a configurable timeout, not left consuming a thread forever
- [ ] **Jumpstart pack merge:** Often missing land adjustment -- real Jumpstart packs include specific basic lands. Verify merged decks have correct land counts, not double-lands from both packs
- [ ] **Jumpstart deck saving:** Often missing persistence distinction -- verify Jumpstart decks are stored/labeled as Jumpstart so they don't pollute the Constructed deck list
- [ ] **Auto-yield:** Often missing re-enable on state change -- verify auto-yield cancels when an opponent plays a spell during your end step (you might want to respond)
- [ ] **Undo:** Often missing UI state rollback -- verify that when the engine undoes a stack item, the frontend removes the card from the stack panel and restores the source zone
- [ ] **Card image quality:** Often missing fallback chain -- verify that if a set/collector URL 404s (promo cards, special printings), it falls back to name-based search rather than showing broken image
- [ ] **Game log:** Often missing timing information -- verify log entries include turn number and phase so users can reconstruct game flow
- [ ] **Keyboard shortcuts:** Often missing focus management -- verify shortcuts only fire when the game board is focused, not when typing in a chat/search field

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Shared guiPlayer corrupting simulation | MEDIUM | Extract simulation into isolated code path with no GamePlayerUtil dependency. Only requires changing the simulation runner, not the interactive game code |
| Thread pool exhaustion from simulation | LOW | Replace unbounded pool with bounded pool + queue. No architectural change needed |
| FModel preference corruption | LOW | Build GameRules explicitly instead of via getDefaultRules(). Localized change |
| No Jumpstart GameType causing validation errors | MEDIUM | Create UI-level validation for Jumpstart (pack composition rules) and map to Constructed for gameplay. Requires new validation path but no engine changes |
| Undo expectations mismatch | LOW | Rename button label and conditionally show/hide. UI-only change |
| Priority confusion from raw prompts | HIGH | Requires restructuring how WebGuiGame formats outbound messages -- adding phase context to every prompt. Touches many methods in WebGuiGame |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Static guiPlayer singleton | Deck Simulation | Simulation runner creates only LobbyPlayerAi instances. No test references GamePlayerUtil.getGuiPlayer() |
| Unbounded thread growth | Deck Simulation | Thread pool is bounded. Load test with 50 queued simulations shows stable thread count |
| FModel preference corruption | Deck Simulation | Simulation builds GameRules without calling FModel.getPreferences(). Run simulation while interactive game is active, verify no interference |
| No Jumpstart GameType | Jumpstart Format | Merged 40-card deck starts and plays without engine validation errors. Format label displays correctly |
| Undo expectations | Gameplay UX | Undo button hidden when canUndoLastAction() is false. Button label says "Undo Last Spell" |
| Priority confusion | Gameplay UX | Phase indicator visible. OK/Cancel buttons show context-aware labels. Auto-yield preferences persist |
| Scryfall URL migration cache invalidation | Card Image Quality | Existing deck builder images still load after URL scheme change. No burst of 429 errors in network tab |
| Simulation WebSocket spam | Deck Simulation | Headless games produce zero WebSocket messages. Verified by checking wsContext is null/unused |
| Game object memory leaks | Deck Simulation | After 100 sequential simulations, heap usage returns to baseline (within 10%) |
| Auto-yield implemented as WebSocket round-trip | Gameplay UX | `isUiSetToSkipPhase` reads from local Set, no network call. Measurable by checking game loop latency unchanged |

## Sources

- Direct codebase analysis of `forge-gui-web` module (WebServer.java, WebGuiGame.java, WebInputBridge.java)
- Direct codebase analysis of `forge-core/ThreadUtil.java` -- unbounded cached thread pool at line 23
- Direct codebase analysis of `forge-gui/GamePlayerUtil.java` -- static singleton guiPlayer at line 22
- Direct codebase analysis of `forge-game/GameType.java` -- no Jumpstart enum value
- Direct codebase analysis of `forge-gui/HostedMatch.java` -- startGame flow, humanCount check at line 249
- Direct codebase analysis of `forge-gui/PlayerControllerHuman.java` -- undo constraints at lines 2360-2384
- Scryfall API documentation: 10 requests/second rate limit, `/cards/{set}/{collector}` endpoint behavior
- v1.0 known tech debt from PROJECT.md -- format validation, AI deck fallback, duplicate types

---
*Pitfalls research for: Forge Web Client v2.0 features*
*Researched: 2026-03-20*
