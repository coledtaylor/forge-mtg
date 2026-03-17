# Pitfalls Research

**Domain:** Wrapping synchronous Java game engine (Forge MTG) with async WebSocket-based web client
**Researched:** 2026-03-16
**Confidence:** HIGH (derived from direct codebase analysis of Forge engine internals)

## Critical Pitfalls

### Pitfall 1: Sync-to-Async Bridge Deadlocks

**What goes wrong:**
The Forge game engine runs on its own thread and blocks via `CountDownLatch.await()` in `InputSyncronizedBase.showAndWait()` whenever it needs human input. If the web `IGuiGame` implementation does not correctly route the WebSocket response back to release the latch, the game thread deadlocks permanently. The user sees a frozen game with no error message.

This is the single most dangerous pitfall. `PlayerControllerHuman` is 3,487 lines with dozens of methods that each call `IGuiGame` methods expecting synchronous responses (e.g., `getChoices()`, `confirm()`, `one()`, `chooseSingleEntityForEffect()`). Every one of these is a potential deadlock site.

**Why it happens:**
Developers assume the blocking call is simple request-response, but Forge's `InputQueue` uses a `BlockingDeque<InputSynchronized>` with an Observer pattern. The input is pushed, the game thread blocks on a `CountDownLatch`, and the GUI must call `stop()` on the input to release the latch. Missing any step in this chain -- or calling it on the wrong thread -- causes a silent hang.

**How to avoid:**
- Build the `CompletableFuture`-based bridge as the very first thing. Before any UI work, prove that a WebSocket message can unblock a waiting `InputSyncronizedBase`.
- Create a `WebInputProxy` that mirrors the desktop `InputProxy` but routes through WebSocket instead of EDT.
- Add a timeout (e.g., 5 minutes) on all `CountDownLatch.await()` calls in the web implementation. If it fires, log the input type, send an error to the client, and abort the game rather than hanging forever.
- Write an integration test: start a game, reach mulligan decision, send response via WebSocket, verify game advances.

**Warning signs:**
- Game starts but immediately freezes after showing the opening hand
- Server thread count grows but CPU is zero (threads parked on latches)
- Works for "pass priority" but breaks on any card-specific choice

**Phase to address:**
Phase 1 (Core Bridge). This must be proven before any UI work begins. If this doesn't work, nothing else matters.

---

### Pitfall 2: Incomplete IGuiGame Method Coverage

**What goes wrong:**
`IGuiGame` has 90+ methods. Developers implement the obvious ones (`updatePhase`, `updateZones`, `showPromptMessage`) and stub the rest. Then specific cards or game situations call an unimplemented method, the web client silently ignores it, and the game enters an inconsistent state. The player sees a board that doesn't match reality -- cards appear in wrong zones, combat damage doesn't resolve, or choices are auto-selected incorrectly.

**Why it happens:**
The interface is enormous and many methods are only called for specific MTG mechanics. You won't hit `manipulateCardList()` until someone plays a Brainstorm-like effect. You won't hit `assignCombatDamage()` with multiple blockers until a creature with trample is blocked by two creatures. The "long tail" of MTG interactions means you can play 50 games without triggering certain methods.

**How to avoid:**
- Categorize all `IGuiGame` methods into tiers:
  - **Tier 1 (game flow):** `updatePhase`, `updateTurn`, `updateZones`, `showPromptMessage`, `updateButtons`, `finishGame` -- implement first
  - **Tier 2 (choices):** `getChoices`, `confirm`, `one`, `oneOrNone`, `many`, `order`, `chooseSingleEntityForEffect` -- implement second, these are the blocking input methods
  - **Tier 3 (display):** `showCombat`, `updateStack`, `handleGameEvent`, `setCard`, `updateManaPool` -- implement for visual completeness
  - **Tier 4 (edge cases):** `manipulateCardList`, `sideboard`, `insertInList`, `assignGenericAmount` -- implement when testing specific mechanics
- Every unimplemented method must throw an explicit `UnsupportedOperationException` with the method name, not silently return null/empty. This converts invisible bugs into visible crashes.
- Track which methods get called during test games to measure coverage.

**Warning signs:**
- Games "work" in testing but players report that specific cards cause hangs or wrong behavior
- `NullPointerException` in `PlayerControllerHuman` after calling an `IGuiGame` method
- Game state diverges between what the engine thinks and what the client shows

**Phase to address:**
Phase 1-2 (Tier 1-2 in bridge phase, Tier 3-4 in gameplay phase). Use the tier system to know when you're "done enough" for each phase.

---

### Pitfall 3: TrackableObject Serialization Circular References

**What goes wrong:**
`CardView` contains references to `PlayerView` (owner, controller), `PlayerView` contains collections of `CardView` (hand, battlefield, graveyard -- all 13+ zones), and `CardView` can reference other `CardView` objects (attached cards, enchanted entity, paired cards, clone origin, exiled cards, haunted-by, etc.). Naive JSON serialization with Jackson or Gson hits `StackOverflowError` from circular references, or produces multi-megabyte payloads that choke the WebSocket.

The `TrackableProperty` enum alone has 180+ properties including nested `CardViewCollectionType` references in at least 20 places (AttachedCards, ChosenCards, MergedCardsCollection, ImprintedCards, ExiledCards, HauntedBy, EncodedCards, UntilLeavesBattlefield, GainControlTargets, Commander, Ante, Battlefield, Command, Exile, Flashback, Graveyard, Hand, Library, Sideboard, etc.).

**Why it happens:**
Forge's `TrackableObject` system was designed for in-process use where object identity handles references. It has its own serialization system (ordinal-based `TrackableSerializer`/`TrackableDeserializer`) used for network play, but this is a binary protocol not suitable for JSON/WebSocket.

**How to avoid:**
- Use ID-based references in JSON. Every `TrackableObject` has an `int getId()`. Serialize `CardView` with `ownerId: 42` instead of embedding the full `PlayerView`.
- Build a dedicated `GameStateDTO` layer that flattens the object graph:
  ```
  { players: [...], cards: [...], stack: [...] }
  ```
  where cards reference players by ID, and players list card IDs per zone.
- Send incremental updates (changed properties only) rather than full state. Forge's `TrackableProperty` system with `FreezeMode` already tracks what changed -- leverage this.
- Set a hard size limit on WebSocket messages (e.g., 256KB). If a full state dump exceeds this, you have a serialization problem.

**Warning signs:**
- `StackOverflowError` or `OutOfMemoryError` during JSON serialization
- WebSocket messages exceeding 100KB for routine board states
- Client rendering slows as game progresses (accumulating data)

**Phase to address:**
Phase 1 (Core Bridge). The DTO/serialization format must be designed before any gameplay features. Getting this wrong means rewriting every message handler later.

---

### Pitfall 4: EDT/Threading Assumptions in Forge Engine

**What goes wrong:**
Forge's GUI layer assumes an EDT (Event Dispatch Thread) model. `FThreads.assertExecutedByEdt(false)` is called in `InputSyncronizedBase.awaitLatchRelease()` -- it asserts the blocking call is NOT on the EDT. `FThreads.invokeInEdtNowOrLater()` is called in `stop()` to finalize inputs. The `IGuiBase.invokeInEdtLater()` and `invokeInEdtAndWait()` methods are used throughout.

A web server has no EDT. If the web `IGuiBase` implementation doesn't provide a thread that behaves like an EDT, these assertions will fire, or worse, thread-safety assumptions will be violated silently, causing intermittent state corruption.

**Why it happens:**
The threading model is implicit, scattered across `FThreads`, `InputBase`, `InputSyncronizedBase`, `InputProxy`, and `PlayerControllerHuman`. There's no single document that says "here's the threading contract." Developers see the EDT references and think they can just no-op them, but the assertions and synchronization depend on the single-threaded EDT guarantee.

**How to avoid:**
- Implement a web-specific `IGuiBase` that provides a single-threaded executor acting as a pseudo-EDT. All `invokeInEdtLater` calls go to this executor.
- The game thread, the WebSocket handler thread, and the pseudo-EDT must be three separate threads with clear responsibilities:
  - Game thread: runs the engine, blocks on `CountDownLatch`
  - WebSocket thread: receives player actions, posts them to pseudo-EDT
  - Pseudo-EDT: processes inputs, calls `stop()` on inputs, sends updates to WebSocket
- Never no-op the `assertExecutedByEdt` calls. Instead, make them check the correct thread for the web implementation.

**Warning signs:**
- `AssertionError` from `FThreads.assertExecutedByEdt`
- Intermittent `ConcurrentModificationException` in zone updates
- Race conditions where two inputs process simultaneously

**Phase to address:**
Phase 1 (Core Bridge). The threading model must be designed alongside the sync-to-async bridge.

---

### Pitfall 5: Mishandling the Input Stack (Not Just Request-Response)

**What goes wrong:**
Developers assume the engine sends one prompt, the player responds, and the game continues. In reality, Forge uses an `InputQueue` backed by a `BlockingDeque` -- a STACK of inputs. Inputs can nest. For example, casting a spell with targets pushes `InputPassPriority`, then `InputSelectTargets`, then potentially `InputPayManaOfCostPayment`, and a modal choice on top of that. Each must be resolved in LIFO order before the game thread unblocks.

If the web client treats each WebSocket message as an independent request-response, it will respond to the wrong input, and the game will hang or behave incorrectly.

**Why it happens:**
The desktop UI handles this naturally because `InputProxy` observes the `InputQueue` and always renders the top-of-stack input. Web developers who don't study `InputQueue.setInput()` / `removeInput()` / `getActualInput()` miss that this is a stack, not a queue.

**How to avoid:**
- Every prompt sent to the client must include an `inputId` (the input's identity on the stack).
- The client's response must include the `inputId` it's responding to.
- Server must reject responses that don't match the current top-of-stack input.
- Send the full input stack state to the client so it can display "resolving X in response to Y" context.

**Warning signs:**
- Casting a spell with mana payment works, but casting a spell that requires target selection AND mana payment hangs
- "Cannot remove input" log messages (the engine's own warning from `InputQueue.removeInput()`)
- Games work for simple actions but break on complex multi-step actions

**Phase to address:**
Phase 1 (Core Bridge). Must be designed into the protocol from the start, not bolted on later.

---

### Pitfall 6: Scryfall API Rate Limiting and Image Loading Strategy

**What goes wrong:**
Scryfall has a rate limit of 10 requests/second. A battlefield with 20+ cards, each needing an image, overwhelms the limit immediately. The client either gets 429 errors (and shows broken images) or the developer adds aggressive caching that serves stale/wrong art for reprinted cards.

In a typical Commander game, a player might have 30+ permanents on the battlefield, 7 cards in hand, 15+ in graveyard, plus the stack. That's 50+ unique card images potentially needed on first render.

**Why it happens:**
Developers test with small board states (5-10 cards) where Scryfall responds fine. Commander games or late-game board states with tokens, copies, and full graveyards expose the issue.

**How to avoid:**
- Use Scryfall's bulk data download for card-to-image-URL mapping. Download the `default_cards` JSON once (about 300MB), extract `image_uris` for every card, cache locally. Never hit the API per-card during gameplay.
- Use `https://api.scryfall.com/cards/{set}/{collector_number}?format=image` URLs directly -- these are CDN-hosted and not rate-limited in the same way as API calls.
- Implement progressive loading: load images for hand and battlefield first, graveyard/exile on hover or expand.
- Cache with `{name}_{set}_{collector_number}` as key to handle reprints correctly.

**Warning signs:**
- 429 responses from Scryfall in browser console
- Images load slowly or not at all during gameplay
- Wrong card art displayed (caching by name without set)

**Phase to address:**
Phase 2 (Deck Builder / Card Display). Solve before gameplay phase, since the deck builder will hit the same issue during card browsing.

---

### Pitfall 7: Game State Desync Between Server and Client

**What goes wrong:**
The server sends incremental zone updates (`PlayerZoneUpdate`), but if the client misses one WebSocket message (brief disconnection, browser tab backgrounding, message ordering), its local state diverges from the server. The player sees cards in wrong zones, incorrect life totals, or phantom cards. Unlike a video stream, a missed game state update compounds -- every subsequent update builds on the wrong base.

**Why it happens:**
WebSocket is TCP-based so messages are ordered and reliable in theory, but browser tab suspension, network interrupts, or client-side processing errors can cause the client to miss or fail to apply an update. The client has no way to detect it's out of sync until something visually wrong happens.

**How to avoid:**
- Include a monotonically increasing `sequenceNumber` on every server message.
- Client tracks the last processed sequence number. If a gap is detected, request a full state resync.
- Implement a `GET /api/game/{id}/state` REST endpoint that returns the complete current `GameView` as JSON for resync.
- Periodically (every 10 turns or on phase change) send a lightweight checksum of game state (e.g., hash of player life totals + card count per zone). Client can verify it matches local state.
- Handle browser `visibilitychange` events -- when tab returns to foreground, request a resync.

**Warning signs:**
- Players report "ghost cards" that aren't really there
- Life totals don't match what the engine thinks
- Gameplay works fine in focused tabs but breaks after alt-tabbing

**Phase to address:**
Phase 2 (Gameplay). Design the sequence numbering into the protocol from Phase 1, but the resync endpoint can come in Phase 2.

---

### Pitfall 8: Blocking Choice Methods Returning Wrong Types Over WebSocket

**What goes wrong:**
`IGuiGame` methods like `getChoices()`, `chooseSingleEntityForEffect()`, and `assignCombatDamage()` use Java generics (`<T> List<T> getChoices(...)`). The return types include `CardView`, `SpellAbilityView`, `GameEntityView`, `PlayerView`, `String`, `Integer`, and more. The WebSocket protocol sends everything as JSON. If the server doesn't correctly reconstruct the typed Java object from the JSON response, the engine gets a `ClassCastException` or silently uses wrong data.

The `assignCombatDamage` method returns `Map<CardView, Integer>` -- a map from blockers to damage amounts. The `assignGenericAmount` returns `Map<Object, Integer>`. These complex return types are not trivially serializable and deserializable.

**Why it happens:**
JSON erases Java type information. A CardView ID "42" coming back from the client must be resolved to the actual `CardView` instance that the engine holds. The server must maintain a registry of live `TrackableObject` instances and look them up by ID when deserializing client responses.

**How to avoid:**
- Build a `ViewRegistry` on the server that maps `int id -> TrackableObject`. Every `CardView`, `PlayerView`, `SpellAbilityView` etc. that gets sent to the client is registered.
- Client responses always use IDs: `{ "selectedCards": [42, 57] }` not serialized objects.
- Server deserializer resolves IDs back to live Java objects before passing to the engine.
- Type-specific response handlers: `ChoiceResponse<CardView>`, `ChoiceResponse<SpellAbilityView>`, etc. -- don't use a generic "parse anything" approach.
- Test with polymorphic choices: `chooseSingleEntityForEffect` where the options include both `CardView` and `PlayerView` objects.

**Warning signs:**
- `ClassCastException` in `PlayerControllerHuman` after receiving a client response
- Combat damage doesn't apply correctly (wrong card-to-damage mapping)
- Multi-target spells only work when targets are all the same type

**Phase to address:**
Phase 1 (Core Bridge). The `ViewRegistry` must be part of the bridge architecture.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Stub `IGuiGame` methods with no-ops instead of throwing | Get basic games running fast | Invisible bugs when unstubbed methods are called by specific cards | Only in Phase 1 if paired with logging. Remove all no-ops by Phase 2. |
| Send full `GameView` on every update instead of incremental deltas | Simpler protocol, no desync risk | 50-100KB per message in complex board states, stuttery UI | Acceptable for Phase 1 prototype. Must move to incremental by Phase 2. |
| Single-threaded game loop (no pseudo-EDT) | Avoid threading complexity | Cannot handle concurrent requests, fragile under reconnection | Never. Threading model must be correct from Phase 1. |
| Hardcoded card image URLs without caching | Quick prototype | Rate limit hits, slow load times, broken images | Phase 1 only, with a TODO to implement caching in Phase 2. |
| Skip WebSocket reconnection handling | Simpler client code | Any network blip kills the game permanently | Phase 1 prototype only. Must add reconnection by Phase 2. |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Scryfall API | Hitting `/cards/search` per-card during gameplay | Use bulk data for URL mapping; use CDN image URLs directly |
| Forge `StaticData` | Not initializing before handling requests; initialization takes 5-10 seconds for 20,000+ cards | Initialize `StaticData` on server startup, block until ready, show loading state to client |
| Forge `FModel` | Assuming `FModel.getPreferences()` works without desktop context | Create a web-specific `FModel` initialization that provides headless defaults for all preferences |
| `IGuiBase` | No-oping platform methods like `invokeInEdtLater` | Implement a proper single-threaded executor; GUI thread contract matters for correctness |
| Forge `GameRules` | Not validating deck format before `HostedMatch.startGame()` | Validate on the REST endpoint; engine validation happens too late and throws cryptically |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Serializing full `CardView` (1,942-line class with 180+ trackable properties) | Slow WebSocket messages, high memory churn | Serialize only changed properties; use ID references | Board states > 30 permanents |
| Creating new JSON serializer per message | GC pressure, latency spikes | Reuse Jackson `ObjectMapper` (it's thread-safe) | Under rapid game events (combat with 10+ creatures) |
| Rendering all zones simultaneously in React | Janky UI, dropped frames | Virtualize card lists; lazy-load graveyard/exile | > 50 cards visible at once |
| Scryfall image loading without lazy loading | Browser fetches 50+ images simultaneously | Intersection Observer for viewport-based loading; preload hand and battlefield only | Any Commander game |
| Not debouncing rapid game state updates | React re-renders on every zone change during combat resolution | Batch updates with `requestAnimationFrame` or React 18 automatic batching | Combat with 5+ attackers and blockers |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Exposing `IDevModeCheats` interface over WebSocket | Players can add cards, set life, etc. through dev tools | Do not expose dev mode in the web API. Remove or guard `IDevModeCheats` access entirely. |
| Not validating that submitted choices are in the valid options set | Malicious WebSocket messages could select illegal targets or cards | Server must verify every choice against the current input's valid options (engine already does this via `PlayerControllerHuman`, but verify the web layer doesn't bypass it) |
| Binding to `0.0.0.0` instead of `localhost` | Server accessible on LAN; another user could play as you or inject moves | Default to `127.0.0.1` only. If LAN play is desired later, make it opt-in. |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| No visual indicator of what the engine is waiting for | Player doesn't know it's their turn or what to click | Always show a prominent prompt message from `showPromptMessage()`. Highlight selectable cards from `setSelectables()`. Show "Waiting for AI..." during AI turns. |
| Not showing the stack | Player doesn't understand what's resolving or when to respond | Render the stack (`updateStack`) as a visible zone. MTG players expect to see the stack and respond to items on it. |
| Treating priority as a simple "your turn / their turn" | Missing the ability to respond to spells, pass priority through phases | Implement the full priority system. Show OK/Cancel buttons per `updateButtons()`. Respect auto-pass settings (`isUiSetToSkipPhase`, `mayAutoPass`). |
| No undo for accidental clicks | Misclick a land tap or wrong attacker, game is ruined | Forge supports undo through the input system (cancel buttons). Expose this clearly. |
| Card text too small to read | Player can't understand what cards do | Card hover/click should show a large card view with full oracle text. This is more important in web than desktop because screen sizes vary. |
| Not showing mana pool | Player can't track available mana during complex casting | Always display mana pool. Highlight when floating mana is available. |

## "Looks Done But Isn't" Checklist

- [ ] **Basic gameplay:** Often missing mana payment flow -- verify a spell with {2}{R} can be cast by tapping specific lands, not just auto-pay
- [ ] **Combat:** Often missing multi-blocker damage assignment -- verify a 5/5 trampler blocked by a 2/2 and a 1/1 shows the damage split dialog
- [ ] **Combat:** Often missing attacker/blocker ordering -- verify `order()` method works for ordering blockers
- [ ] **Triggered abilities:** Often missing "may" triggers -- verify `confirm()` dialog appears for optional triggers like "may draw a card"
- [ ] **Stack interaction:** Often missing the ability to respond to spells on the stack -- verify `InputPassPriority` correctly prompts for responses
- [ ] **Game over:** Often missing match continuation -- verify `finishGame()` handles best-of-3 matches, not just single games
- [ ] **Card selection:** Often missing multi-select -- verify `many()` and `getChoices(min, max)` work for "choose up to 3 creatures"
- [ ] **Zone visibility:** Often missing library/graveyard browsing -- verify `tempShowZones()` and `hideZones()` work for search effects
- [ ] **Auto-yields:** Often missing auto-pass configuration -- verify `shouldAutoYield`, `autoPassUntilEndOfTurn` work to speed up gameplay
- [ ] **Reconnection:** Often missing entirely -- verify a browser refresh during a game can reconnect and resume

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Deadlocked game thread | LOW | Add timeout to `CountDownLatch.await()`. On timeout, abort game and send error to client. Offer to start new game. |
| Desync'd client state | LOW | Full state resync via REST endpoint. Client requests complete `GameView`, replaces local state. |
| Circular reference in serialization | MEDIUM | Retrofit ID-based references into existing DTOs. Requires touching every serializer but no architecture change. |
| Wrong threading model | HIGH | Requires rewriting `IGuiBase` implementation and potentially the bridge layer. Get this right from Phase 1. |
| Incomplete IGuiGame coverage | MEDIUM | Each missing method can be implemented independently. Track with coverage logging. Fix as bugs are reported. |
| Scryfall rate limiting | LOW | Switch to bulk data approach. Can be done independently of other systems. |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Sync-to-async deadlocks | Phase 1 (Core Bridge) | Integration test: start game, reach mulligan, respond, verify game advances |
| Incomplete IGuiGame coverage | Phase 1-2 (incremental) | Coverage logging shows all Tier 1-2 methods called and handled |
| Serialization circular refs | Phase 1 (Core Bridge) | Full board state serializes to JSON under 50KB; no StackOverflowError |
| EDT threading assumptions | Phase 1 (Core Bridge) | No `AssertionError` from `FThreads`; no `ConcurrentModificationException` in tests |
| Input stack mishandling | Phase 1 (Core Bridge) | Cast a spell with targets + mana payment; all nested inputs resolve correctly |
| Scryfall rate limiting | Phase 2 (Card Display) | Load a 100-card Commander deck view without 429 errors |
| Game state desync | Phase 2 (Gameplay) | Background and foreground browser tab; verify state matches server |
| Wrong return types over WS | Phase 1 (Core Bridge) | Cast a spell targeting a mix of creatures and players; no ClassCastException |

## Sources

- Direct codebase analysis of `IGuiGame.java` (90+ methods), `PlayerControllerHuman.java` (3,487 lines), `InputSyncronizedBase.java` (CountDownLatch blocking), `InputQueue.java` (BlockingDeque stack), `TrackableProperty.java` (180+ properties with circular references), `FThreads.java` (EDT assertions)
- `CardView.java` (1,942 lines), `PlayerView.java` (618 lines), `GameView.java` (297 lines) -- view object complexity
- Scryfall API documentation: rate limits of 10 req/sec, bulk data availability
- Forge's existing network play serialization system (ordinal-based `TrackableSerializer`) as reference for what the engine team already solved differently

---
*Pitfalls research for: Forge MTG Web Client -- synchronous Java engine to async WebSocket bridge*
*Researched: 2026-03-16*
