# Stack Research: v2.0 Additions

**Domain:** Gameplay UX polish, Jumpstart format, headless simulation, card image quality for Forge Web Client
**Researched:** 2026-03-20
**Confidence:** HIGH

This document covers ONLY stack additions/changes for v2.0 features. The validated v1.0 stack (React 19, TypeScript, Vite 8, Tailwind CSS 4, Zustand, TanStack Query, Javalin 7, Jackson 2.21) remains unchanged.

## New Frontend Libraries

### Keyboard Shortcuts

| Library | Version | Purpose | Why Recommended |
|---------|---------|---------|-----------------|
| react-hotkeys-hook | 5.x | Keyboard shortcut handling for game actions | Most mature React keyboard library (707+ dependents on npm). Declarative `useHotkeys` hook integrates naturally with React component lifecycle. Supports scoped shortcuts (only active when component is mounted/focused), which is critical for game board vs deck editor contexts. Tiny bundle (~2KB). |

**Why not alternatives:**
- **tinykeys** (~650B): Framework-agnostic, requires manual `useEffect` cleanup. Saves ~1.3KB but loses React lifecycle integration and scope management that `react-hotkeys-hook` provides out of the box. For a game UI with multiple shortcut contexts (game board, dialogs, deck editor), the scoping alone justifies the library.
- **Native `useEffect` + `addEventListener`**: Viable for 2-3 shortcuts, but v2.0 needs 8-12 shortcuts (OK/Cancel/Pass/Undo/auto-yield toggles/zone overlays). Manual management becomes error-prone with multiple active contexts.
- **react-keyhub**: Newer, less battle-tested. Overkill for our needs -- we don't need a built-in shortcut sheet component.

**Key shortcuts to implement:**
- `Space` / `Enter` -- OK/confirm (maps to `BUTTON_OK`)
- `Escape` -- Cancel (maps to `BUTTON_CANCEL`)
- `P` -- Pass priority (same as OK when it means "pass")
- `Z` -- Undo (when engine supports it)
- `Y` -- Toggle auto-yield for current prompt
- `F2` -- Auto-pass until end of turn
- `G` -- Toggle graveyard overlay
- `E` -- Toggle exile overlay

**Integration pattern:**
```tsx
// Scoped to GameBoard component -- shortcuts only active during gameplay
useHotkeys('space', () => wsRef.current?.sendButtonOk(), { enabled: buttons?.enable1 })
useHotkeys('escape', () => wsRef.current?.sendButtonCancel(), { enabled: buttons?.enable2 })
```

### Game Log Display

**No new library needed.** The game log is a scrollable list of text entries. The existing stack already handles this:
- Zustand store: Add a `gameLog: string[]` array to `GameState`
- Backend: The engine already calls `showPromptMessage`, `message`, and `showCardPromptMessage` on `WebGuiGame`. These currently send `MESSAGE` type WebSocket messages that the frontend largely ignores. Capture these as log entries.
- Frontend: A simple `<div>` with `overflow-y-auto` and `ref` for auto-scroll-to-bottom. No virtualization needed -- MTG games rarely exceed 200-300 log entries.

**New WebSocket message type needed:** `GAME_LOG` (or reuse `MESSAGE` with a `log` sub-type). The backend should accumulate log entries and send them. The frontend appends to the Zustand `gameLog` array.

### Advanced Deck Statistics

**No new library needed.** The existing `deck-stats.ts` already computes mana curve, color distribution, and type breakdown. The v2.0 additions (removal count, ramp density, draw density) are keyword/oracle-text pattern matching -- pure TypeScript string operations on the card data already available in the deck editor state.

**Implementation approach:**
- Parse `oracleText` field (already in `CardSearchDto` and `DeckCardEntry`) for keywords: "destroy", "exile", "damage" (removal); "add {" mana patterns (ramp); "draw a card", "draw cards" (draw).
- This is heuristic-based and imperfect, but matches what other deck builders do. No NLP library needed.

## Backend Additions

### Headless Simulation Engine

**No new Java dependencies needed.** The simulation feature runs AI vs AI games using the existing Forge engine infrastructure.

| Component | Exists | What to Build |
|-----------|--------|---------------|
| `HostedMatch` | Yes | Reuse directly -- already supports `Map<RegisteredPlayer, IGuiGame>` where an empty map means no human GUI |
| `GamePlayerUtil.createAiPlayer()` | Yes | Both players use AI profiles |
| `RegisteredPlayer` | Yes | Wrap both decks as AI players |
| `GameRules` / `GameType` | Yes | Use `GameType.Constructed` (or Commander) |
| Game thread management | Yes | `ThreadUtil.invokeInGameThread()` already handles this |

**What's new:**
1. **`HeadlessGuiGame`** -- A no-op `IGuiGame` implementation for the "observer" side. Unlike `WebGuiGame`, it never sends WebSocket messages. It just records game outcome (winner, turn count, life totals). This is simpler than `WebGuiGame` -- most methods are empty. Extend `AbstractGuiGame` and override the abstract methods with no-ops.

2. **`SimulationHandler`** -- A new REST handler (`POST /api/simulate`) that:
   - Accepts: `{ deckName: string, opponents: string[], gameCount: number, format: string }`
   - Runs N games sequentially on the game thread (NOT the Javalin request thread)
   - Returns: `{ wins: number, losses: number, draws: number, avgTurns: number, results: [...] }`
   - Uses `CompletableFuture` to bridge the async REST request to the synchronous game execution

3. **Progress reporting via SSE or polling** -- For 10+ game simulations that take 30+ seconds:
   - **Use polling** (`GET /api/simulate/{jobId}/status`). SSE would work but adds complexity for a local-only tool. Store simulation jobs in a `ConcurrentHashMap<String, SimulationJob>` (same pattern as `activeSessions`).
   - Frontend polls every 2 seconds with TanStack Query's `refetchInterval`.

**Key insight from `HostedMatch.startMatch()`:** When the `guis` map is empty (no human player mapped), the engine assigns `LobbyPlayerAi` controllers to all players. The game runs to completion without any GUI interaction. This is exactly what we need -- no special "headless mode" flag required.

**Thread safety concern:** The current `WebServer` runs one game at a time per WebSocket session. Simulation games must NOT share the game thread with an active interactive game. Use a separate `ExecutorService` (single-threaded or pool of 2) for simulation games.

### Jumpstart Format Support

**No new Java dependencies needed.** Jumpstart uses existing engine infrastructure but needs format mapping work.

| Issue | Current State | Solution |
|-------|---------------|----------|
| No `GameType.Jumpstart` enum | `GameType` has no Jumpstart entry | Use `GameType.Constructed` with `DeckFormat.Constructed` -- Jumpstart decks are just 40-card constructed decks. Do NOT add a new `GameType` enum (would require engine changes across many files). |
| Format validation returns 400 | `FormatValidationHandler` tries to find a `GameFormat` named "Jumpstart" | Add a special case: if format is "Jumpstart", validate as 40-card minimum with no banned list. The Forge engine has no `GameFormat` for Jumpstart, and adding one is unnecessary for deck validation. |
| Jumpstart pack data | Forge has Jumpstart edition data (`res/editions/Jumpstart.txt`, `Jumpstart 2022.txt`, `Foundations Jumpstart.txt`) | Use `CardEdition` to enumerate cards in Jumpstart sets. Build pack definitions as curated 20-card lists server-side. |

**Pack builder approach:**
- New REST endpoint: `GET /api/jumpstart/packs` -- returns available 20-card packs
- New REST endpoint: `POST /api/jumpstart/combine` -- takes two pack names, returns a combined 40-card deck
- Pack data can come from Forge's existing edition card lists or from hardcoded pack definitions (Jumpstart products define specific named packs like "Phyrexian", "Rainbow", etc.)

### Card Image Quality (Preferred Printings)

**No new dependencies.** This is a Scryfall URL construction change on the frontend and a data enrichment on the backend.

**Current state:**
- Deck builder / card search: Uses `setCode` + `collectorNumber` from `CardSearchDto` -- already uses direct Scryfall URLs via `getScryfallImageUrl()`. These show whatever printing Forge's `CardDb` returns (often the first printing).
- Game board: Uses name-based lookup via `getScryfallNameImageUrl()` in `GameCardImage.tsx` because `CardDto` (game state) lacks `setCode`/`collectorNumber`. Scryfall's name-based endpoint returns the most recent printing, which is usually fine but inconsistent with the deck builder.

**Solution -- two parts:**

1. **Add `setCode` and `collectorNumber` to `CardDto`** (game state DTO):
   - `CardView` -> `CardView.getCurrentState()` -> can access the underlying `Card` which has `PaperCard` with edition info
   - Actually, `CardView` does not directly expose `PaperCard`. The `CardView.getCurrentState().getSetCode()` is available through the card's state. Check if `CardView` has a `getSetCode()` method or if we need to pass it through `GameStateDto`.
   - If not directly available, add `setCode`/`collectorNumber` fields to `GameStateDto`'s card representation during `GameStateDto.from(GameView)` by looking up the card's paper representation.

2. **Preferred printing logic on the backend** (for card search):
   - Forge's `CardDb` already has `getCardFromEditions()` and can filter by edition type.
   - `CardEdition.Type.CORE` identifies core sets. Prefer `CORE` or `EXPANSION` printings over `PROMO`, `REPRINT`, etc.
   - Add a `preferredPrinting` parameter to `CardSearchHandler` or always return the core/expansion printing.
   - For Scryfall URL, always append `?format=image&version=normal` (already done). The `lang` parameter is optional -- omitting it returns English by default, which is what we want.

**Scryfall API details (verified):**
- Direct image URL: `https://api.scryfall.com/cards/{setCode}/{collectorNumber}?format=image&version=normal`
- Language override: `https://api.scryfall.com/cards/{setCode}/{collectorNumber}/en?format=image&version=normal`
- The `/en` suffix is unnecessary since English is the default, but can be added for explicitness.
- Image versions: `small` (146x204), `normal` (488x680), `large` (672x936), `png` (highest quality), `art_crop`, `border_crop`

## Installation

```bash
# Frontend -- single new dependency
cd forge-gui-web/frontend
npm install react-hotkeys-hook@^5
```

No backend dependency changes. No dev dependency changes.

## What NOT to Add

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| `react-virtuoso` or `react-window` for game log | Game logs are 200-300 entries max. Virtualization adds complexity for no measurable benefit at this scale. | Plain `overflow-y-auto` div with `scrollTo` |
| SSE (Server-Sent Events) for simulation progress | Adds a new transport mechanism alongside REST + WebSocket. For a local-only tool, polling is simpler and sufficient. | TanStack Query with `refetchInterval: 2000` polling a status endpoint |
| `chart.js` or `recharts` for simulation stats | Simulation results are simple win/loss/draw numbers. A bar or pie chart is nice-to-have but not worth a charting library dependency. | Tailwind-styled progress bars and text stats |
| New `GameType.Jumpstart` enum value | Would require changes across the Forge engine codebase (DeckFormat, GameRules, etc.). Jumpstart decks play as normal Constructed games. | Use `GameType.Constructed` with custom 40-card validation |
| WebSocket for simulation progress | Simulation is a request-response pattern, not a bidirectional stream. WebSocket is for interactive games. | REST polling with job IDs |
| `hotkeys-js` or `mousetrap` | Not React-aware. Require manual lifecycle management. | `react-hotkeys-hook` with built-in React integration |
| `tinykeys` | Saves ~1.3KB but loses React scoping, which matters for multi-context shortcuts | `react-hotkeys-hook` |

## Version Compatibility

| Package | Compatible With | Notes |
|---------|-----------------|-------|
| react-hotkeys-hook@5 | React 18+ / React 19 | Uses `useEffect` and `useCallback` internally, fully compatible with React 19 |
| Existing stack | No changes | All v1.0 dependencies remain at current versions |

## Architecture Impact

### New Backend Classes (Java)

| Class | Package | Purpose |
|-------|---------|---------|
| `HeadlessGuiGame` | `forge.web` | No-op `IGuiGame` for AI vs AI simulation |
| `SimulationHandler` | `forge.web.api` | REST handler for `/api/simulate` endpoints |
| `SimulationJob` | `forge.web` | Tracks simulation progress and results |
| `JumpstartHandler` | `forge.web.api` | REST handler for Jumpstart pack listing/combining |

### New Frontend Files (TypeScript)

| File | Purpose |
|------|---------|
| `hooks/useKeyboardShortcuts.ts` | Central keyboard shortcut registration for game board |
| `components/game/GameLog.tsx` | Scrollable game action log panel |
| `api/simulation.ts` | TanStack Query hooks for simulation API |
| `api/jumpstart.ts` | TanStack Query hooks for Jumpstart pack API |
| `components/simulation/SimulationPanel.tsx` | UI for configuring and viewing simulation results |
| `components/jumpstart/PackPicker.tsx` | Jumpstart pack selection UI |

### Modified Backend Classes

| Class | Change |
|-------|--------|
| `CardDto` | Add `setCode`, `collectorNumber` fields populated from game state |
| `GameStateDto` | Ensure card DTOs include printing identifiers |
| `WebGuiGame` | Add game log accumulation (capture `message()`/`showPromptMessage()` calls) |
| `WebServer` | Add simulation and Jumpstart REST routes, add separate executor for simulation |
| `FormatValidationHandler` | Handle "Jumpstart" format with 40-card minimum validation |

### Modified Frontend Files

| File | Change |
|------|--------|
| `stores/gameStore.ts` | Add `gameLog: string[]` state and `appendLog` action |
| `lib/gameWebSocket.ts` | Handle `GAME_LOG` messages |
| `lib/gameTypes.ts` | Add `GAME_LOG` to `OutboundMessageType` |
| `components/game/GameBoard.tsx` | Add game log panel, integrate keyboard shortcuts |
| `components/game/GameCardImage.tsx` | Switch from name-based to set/collector URL when available |
| `lib/scryfall.ts` | Already supports set/collector URLs -- no change needed |
| `lib/deck-stats.ts` | Add removal/ramp/draw density calculations |

## Sources

- [Scryfall API: Cards by Collector Number](https://scryfall.com/docs/api/cards/collector) -- verified URL format and language parameter (HIGH confidence)
- [Scryfall API: Card Imagery](https://scryfall.com/docs/api/images) -- image version options (HIGH confidence)
- [react-hotkeys-hook on npm](https://www.npmjs.com/package/react-hotkeys-hook) -- version 5.x, 707+ dependents (HIGH confidence)
- [tinykeys on GitHub](https://github.com/jamiebuilds/tinykeys) -- evaluated as alternative (HIGH confidence)
- Forge engine source: `GameType.java`, `HostedMatch.java`, `CardEdition.java` -- verified directly from codebase (HIGH confidence)
- Forge engine source: `CardDto.java`, `WebGuiGame.java`, `WebServer.java` -- verified current implementation (HIGH confidence)

---
*Stack research for: Forge Web Client v2.0 features*
*Researched: 2026-03-20*
