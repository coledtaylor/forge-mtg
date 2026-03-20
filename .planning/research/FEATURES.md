# Feature Research

**Domain:** MTG digital client -- gameplay UX polish, format support, deck simulation, card image quality (v2.0)
**Researched:** 2026-03-20
**Confidence:** HIGH (features well-understood from Arena/MTGO/Forge desktop/Moxfield precedent)

## Feature Landscape

### Table Stakes (Users Expect These)

Features users of any MTG digital client assume exist. Missing these = the game feels unfinished. These are what v1.0 shipped without that need to be addressed.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Game log / action history | Every MTG client (MTGO, Arena, Forge desktop) shows a scrollable log of what happened. Without it, players cannot understand complex stack interactions or verify the engine is correct. MTGO has a persistent sidebar log; Arena relies on visual animations (controversial -- 3rd-party trackers fill the gap). Forge desktop has `GameLog` with typed entries (`GameLogEntryType`) and verbosity levels (`GameLogVerbosity`). | MEDIUM | Engine already has `GameLog` + `GameLogFormatter` + `GameLogEntry` with full history. Backend needs a new WebSocket message to stream log entries to client. Frontend needs a collapsible sidebar panel. The `GameLog` is `Observable` so backend can subscribe and push entries. |
| Keyboard shortcuts | Arena uses Enter/Space (pass priority), Z (undo), Ctrl (full control), Q+click (tap multiple lands). MTGO has F2/F4/F6 for passing priority through phases. Every digital card game has shortcuts for the most common action: "I'm done, move on." | LOW | Current `ActionBar` has buttons but no key bindings. Need a `useKeyboardShortcuts` hook that maps keys to `wsRef.current?.sendButtonOk()` etc. Scope to 5-8 shortcuts for v2. Pure frontend work. |
| Priority/phase clarity | Arena highlights the active phase on the phase strip, shows a glowing border when waiting for input, and makes it obvious whose turn it is. MTGO has a prompt box with blue highlight when action is on you. Current `PhaseStrip` shows phases and `ActionBar` shows "Priority" text but there is no visual urgency -- no pulsing, no glow, no "Your Turn" badge. | LOW | Enhance `PhaseStrip` with active-phase highlight (bold/glow on current phase). Add pulsing border to `ActionBar` when `buttons !== null`. Add turn indicator badge. Pure CSS/styling work on existing components. |
| Targeting feedback | Arena draws arrows from source to target, highlights valid targets with a green glow, dims invalid ones. MTGO shows targeting arrows and highlights in the game area. Current `GameBoard` has targeting mode via `PROMPT_CHOICE` but all cards look the same -- no visual differentiation between valid and invalid targets. | MEDIUM | Add highlight/glow state to `GameCard` for targetable cards (green border for valid, dim for invalid). Draw SVG arrows (reuse pattern from existing `CombatOverlay`) from source to selected targets. Backend needs to include valid card IDs in the prompt payload so frontend knows which cards to highlight. |
| Oracle text display | Every deck builder (Moxfield, Scryfall, Archidekt) shows oracle text alongside or below the card image. Current hover preview (`CardHoverPreview`, `GameHoverPreview`) shows image only. Players need to read text to understand cards, especially when images are small, fail to load, or have foreign art. | LOW | Add oracle text panel below/beside hover preview in both deck editor and game. Data requires extending `CardDto` with an `oracleText` field populated from Forge's `CardRules.getOracleText()`. Small backend DTO change + frontend text panel. |
| Card image quality (English, recognizable art) | Arena and MTGO always show English cards with the "default" printing. Current implementation uses Scryfall's `/cards/named?exact={name}&format=image` which returns the newest English printing by default. However, without `setCode`/`collectorNumber`, we cannot control which printing appears, and name-based lookup is slower than direct URL construction. Users expect recognizable, mainstream art. | LOW | Add `setCode` and `collectorNumber` to `CardDto` (from Forge's card database which has edition info). Use direct Scryfall image URLs: `https://api.scryfall.com/cards/{set}/{number}?format=image`. This is faster (no redirect), gives control over printings, and enables `prefer:default` behavior. Scryfall `lang:en` filter ensures English. |

### Differentiators (Competitive Advantage)

Features that set Forge Web Client apart from Arena/MTGO. Not expected, but valuable for the local-AI-testing niche this tool serves.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Deck simulation (AI vs AI headless) | No other free MTG tool offers automated AI-vs-AI batch testing with a real rules engine. ManaStack has a basic AI simulator. MTG community forums show persistent demand for "1000 goldfish simulations" to test decks statistically. This is Forge's unique killer feature -- its complete rules engine + AI opponents can run headless games producing real data. Lets users answer "is this deck actually good?" with numbers, not gut feeling. | HIGH | Requires a new backend subsystem: REST endpoint to configure simulation (deck, opponent decks/gauntlet, game count, format), headless `Game` instances with two `LobbyPlayerAi` (no GUI, no WebSocket), stat collection (win rate, average turns, mulligan rate, cards seen). Frontend needs a simulation config page and results dashboard with charts/tables. Consider running simulations on a background thread pool to avoid blocking the game server. |
| Advanced deck stats (removal, ramp, draw density) | Moxfield offers custom tags for card categorization but requires manual tagging per card. EDHREC shows community aggregate data. Auto-detecting removal/ramp/draw density from oracle text is a genuine differentiator -- zero manual effort, instant insight. No other free tool does this. | MEDIUM | Classify cards by keyword/oracle text patterns: removal = "destroy" / "exile target" / deals damage to target; ramp = "search your library for a...land" / "add {" mana production; draw = "draw a card" / "draw X". Compute as count and as density (count / deck size). Add to existing `deck-stats.ts` and extend `StatsPanel`. Depends on `oracleText` being available in `DeckCardEntry`. |
| Auto-yield / auto-pass | Arena auto-passes when you have no playable cards at instant speed. Full control (Ctrl) overrides this. Forge desktop has auto-yield per trigger via `IGuiGame.shouldAutoYield`. For a local AI client, aggressive auto-passing on the AI's turn (where you have no instants/abilities) dramatically improves game flow -- without it, you click "Pass Priority" dozens of times per turn. | HIGH | Requires engine integration. Backend needs to detect "player has no legal plays at instant speed" and auto-respond to the `CompletableFuture` without sending to client. Frontend needs: (1) a toggle button/key (Ctrl for full control like Arena), (2) per-phase stop settings as clickable indicators on `PhaseStrip`. The `CompletableFuture` input bridge can short-circuit when auto-pass conditions are met. |
| Goldfish / solitaire mode | Forge desktop has goldfish mode. Arena does not offer it. Essential for combo players testing sequencing without an opponent interfering. Start a game where the opponent does nothing. | LOW | Create game with a "goldfish" AI profile: opponent has basic lands only and always passes priority. Frontend: add "Goldfish" checkbox in `GameLobby`. Backend: create game with `LobbyPlayerAi` using a pass-only AI profile or no-op player. The Forge engine already supports this pattern -- desktop Forge has a solitaire/goldfish option. |
| Undo support (mana tapping) | Arena allows Z to undo mana tapping (before committing a spell). MTGO has no undo. Forge engine has `ManaRefundService` and `ManaPool.refundMana()` plus `undoTap` in `PhaseHandler`. For a single-player local tool, there is no competitive integrity concern, so undo is purely beneficial. | MEDIUM | Safe scope for v2: undo mana tapping only (before a spell resolves on the stack). The engine's `ManaRefundService` handles the refund logic. Backend needs a new WebSocket message type for undo; handler calls the engine's existing refund mechanism. Frontend needs Z key binding. Full game-state undo (rewinding arbitrary decisions) would require engine state snapshots which do not exist -- defer to v3+. |
| Jumpstart pack builder + dual-pack setup | Arena shows 3 pack choices for each half, then merges into a 40-card deck. No other free tool supports building custom Jumpstart packs (20-card themed half-decks) and combining them for play. Forge engine has NO `Jumpstart` GameType (confirmed by source inspection of `GameType.java`). The keyword "Jumpstart" in the engine refers only to the card mechanic (jump-start from graveyard), not the format. | HIGH | Three components: (1) Pack builder UI -- specialized 20-card deck editor with theme name. Uses the existing deck editor infrastructure but constrained to 20 cards. (2) Game setup flow -- pick two packs, merge into 40-card deck client-side or server-side, start game using `Constructed` GameType with the merged deck. (3) Pack library -- CRUD for saved packs. The actual game rules are standard Constructed (40-card minimum, no sideboard). Engine integration is straightforward since the merged deck is just a normal `Deck` object. |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Full game state undo / rewind | Players want to explore different lines of play, try "what if" scenarios | Forge engine does not support full state snapshots or restore. Implementing it means serializing/deserializing entire game state including all zones, counters, emblems, continuous effects, layer system, and triggered ability queues. The engine was not designed for checkpoint/restore. Massive complexity, extremely fragile, and likely to introduce subtle state corruption bugs. | Undo mana tapping only (Arena-style Z). For "what if" exploration, use goldfish mode and restart games quickly. The simulation feature addresses "is this deck good?" without needing replay. |
| Animated card effects / spell VFX | Arena's flashy animations make gameplay visually exciting and help communicate what is happening | Enormous effort for a local tool. Each card effect needs custom animation assets and timing. Arena has a dedicated VFX team with hundreds of custom animations. Animations also slow down gameplay for experienced players who want speed -- Arena players frequently complain about slow animations. | Subtle CSS transition animations for card movement between zones (slide, fade). State change transitions (tap/untap rotation). No per-card particle effects or spell VFX. |
| Real-time multiplayer | "Play with friends" is a natural desire | Architecture is single-user local. WebSocket protocol, game threading, `CompletableFuture` input bridge, and state management all assume one human player. Multiplayer requires authentication, matchmaking, game server infrastructure, anti-cheat, and connection handling. Completely different product. | Explicitly out of scope per PROJECT.md. Stay local-only. |
| AI deck auto-generation | Users want AI to suggest or build optimal decks for them | Deck building is a core MTG skill and part of the fun. Auto-generated decks are either too formulaic (template-based: "add 24 lands, 16 creatures, etc.") or require ML models trained on metagame data that would need constant updating. Neither approach produces satisfying results. | Provide curated starter decks per format. Import from external tools (Moxfield, MTG Goldfish). The import feature already exists in v1. |
| Card price integration | Users want to see TCGPlayer/CardKingdom/CK prices in the deck builder | This is a local playtesting tool, not a collection manager or purchasing tool. Price APIs add external dependencies, rate limits, API key management, and ongoing maintenance. Prices change daily and are irrelevant to gameplay testing. | Scryfall card page link from hover preview (Scryfall shows prices). No in-app price data. |
| Replay / game recording | Users want to review past games like chess replays | Requires serializing every game action with enough context to reconstruct state, building a replay viewer UI, handling state reconstruction for any point in the game. Large storage and significant engineering effort. | Game log (text) captures everything that happened. Add "Export game log" as a text file download after game ends. Covers 95% of the review use case at 5% of the effort. |
| Jumpstart random pack generation | Automatically generate themed 20-card packs from the full card pool | Algorithmically generating a "themed" pack that plays well requires understanding card synergy, power level, mana curve, and theme coherence. This is effectively a deck-building AI problem. Random generation produces unplayable garbage. | Manual pack building only. Users craft their own themed packs using the pack builder. Optionally ship a small set of pre-built example packs as starting points. |

## Feature Dependencies

```
[Priority/Phase Clarity]
    (no deps -- pure CSS enhancement on existing PhaseStrip + ActionBar)

[Keyboard Shortcuts]
    (no deps -- hooks into existing ActionBar button handlers via wsRef)

[Game Log Sidebar]
    └──requires──> [New WebSocket message type: GAME_LOG]
                       └──requires──> [Backend subscribes to GameLog Observable]

[Targeting Feedback]
    └──requires──> [Prompt payload enrichment with valid card IDs]
                       └──requires──> [Backend PROMPT_CHOICE includes targetable card IDs]

[Oracle Text Display]
    └──requires──> [CardDto.oracleText field]
                       └──requires──> [Backend DTO extension from CardRules]

[Card Image Quality]
    └──requires──> [CardDto.setCode + collectorNumber fields]
                       └──requires──> [Backend DTO extension from PaperCard/CardEdition]

[Advanced Deck Stats]
    └──requires──> [DeckCardEntry.oracleText field]
                       └──requires──> [CardDto.oracleText] (shared dependency with Oracle Text Display)

[Auto-Yield / Auto-Pass]
    └──requires──> [Keyboard Shortcuts] (Ctrl toggle for full control)
    └──requires──> [PhaseStrip stop indicators] (clickable phase stops)
    └──requires──> [Backend auto-pass logic in CompletableFuture bridge]

[Undo (Mana Tapping)]
    └──requires──> [Keyboard Shortcuts] (Z key binding)
    └──requires──> [New WebSocket message type: UNDO]
    └──requires──> [Backend integration with ManaRefundService]

[Goldfish Mode]
    └──requires──> [GameLobby UI update] (checkbox/toggle)
    └──requires──> [Backend pass-only AI player configuration]

[Deck Simulation]
    └──requires──> [New REST endpoints: POST /api/simulations, GET /api/simulations/{id}]
    └──requires──> [Backend headless game runner (thread pool)]
    └──requires──> [Frontend simulation config + results page]
    (fully independent of gameplay UX features)

[Jumpstart Pack Builder]
    └──requires──> [Pack CRUD REST endpoints]
    └──requires──> [Pack editor UI (constrained deck editor variant)]
    └──requires──> [Dual-pack selection + merge in GameLobby]
    (fully independent of gameplay UX and simulation)
```

### Dependency Notes

- **Oracle Text + Advanced Stats share a DTO extension:** Both need `oracleText` on `CardDto`/`DeckCardEntry`. Build the backend DTO extension once, both features benefit immediately.
- **Card Image Quality + Oracle Text share backend work:** Both benefit from enriching `CardDto` with more data from Forge's card database. Single backend change serves multiple features.
- **Auto-Yield depends on Keyboard Shortcuts + Phase Strip enhancements:** Must build the UX primitives first (phase stops, key bindings) before auto-yield logic makes sense.
- **Deck Simulation is fully independent:** Zero dependency on gameplay UX features. Can be built entirely in parallel as a separate subsystem.
- **Jumpstart is fully independent:** Zero dependency on gameplay UX or simulation. Can be built in parallel.
- **Priority/Phase Clarity and Keyboard Shortcuts are zero-dependency quick wins:** Ship first for immediate, visible UX improvement with minimal risk.
- **Game Log requires one new WebSocket message type:** Backend change is small (subscribe to existing `GameLog` Observable, push entries). Frontend is a new sidebar component. Medium effort, high value.

## MVP Definition (v2.0 Scope)

### Phase 1: Gameplay UX Foundations (P1 -- ship first)

Quick wins that make the existing game immediately more usable.

- [ ] **Priority/phase clarity** -- pulsing ActionBar border when waiting for input, active phase glow on PhaseStrip, turn indicator. Pure CSS, immediate impact.
- [ ] **Keyboard shortcuts** -- Enter/Space (OK/pass), Escape (cancel), Z (undo). 5 key bindings via `useKeyboardShortcuts` hook.
- [ ] **Oracle text display** -- extend `CardDto` with `oracleText`, show text alongside hover preview in deck editor and game.
- [ ] **Card image quality** -- extend `CardDto` with `setCode`/`collectorNumber`, switch to direct Scryfall URLs. Faster loads, correct printings.

### Phase 2: Gameplay UX Depth (P1 -- ship second)

Features requiring new WebSocket messages or backend logic.

- [ ] **Game log sidebar** -- new WebSocket message type, collapsible sidebar panel, verbosity toggle.
- [ ] **Targeting feedback** -- enrich prompt payload with valid card IDs, highlight valid targets, SVG arrows.
- [ ] **Goldfish mode** -- lobby checkbox, pass-only AI profile. Low complexity but needs backend AI config.
- [ ] **Advanced deck stats** -- removal/ramp/draw density computed from oracle text. Extends existing StatsPanel.

### Phase 3: Advanced Gameplay (P2 -- ship in v2.0 but after core polish)

Features requiring deeper engine integration.

- [ ] **Auto-yield / auto-pass** -- detect no legal plays at instant speed, auto-respond. Full control toggle. Phase stop indicators.
- [ ] **Undo (mana tapping)** -- Z key triggers `ManaRefundService`. Backend undo WebSocket message.

### Phase 4: Simulation Engine (P2 -- can be built in parallel)

Entirely new subsystem, independent of gameplay UX.

- [ ] **Deck simulation** -- headless AI vs AI game runner, REST API for config/results, frontend dashboard with win rate/turns/mulligan stats.

### Defer to v2.x+ (P3)

- [ ] **Jumpstart pack builder** -- full new subsystem with pack editor, pack library CRUD, dual-pack merge + game setup flow. Valuable but large scope; defer until gameplay UX and simulation are solid.
- [ ] **Jumpstart curated pack library** -- pre-built themed packs. Depends on pack builder.

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority | Dependencies |
|---------|------------|---------------------|----------|-------------|
| Priority/phase clarity | HIGH | LOW | P1 | None |
| Keyboard shortcuts | HIGH | LOW | P1 | None |
| Oracle text display | MEDIUM | LOW | P1 | Backend DTO |
| Card image quality | MEDIUM | LOW | P1 | Backend DTO |
| Game log sidebar | HIGH | MEDIUM | P1 | WebSocket msg |
| Targeting feedback | HIGH | MEDIUM | P1 | Backend prompt enrichment |
| Goldfish mode | MEDIUM | LOW | P1 | Backend AI config |
| Advanced deck stats | MEDIUM | LOW | P1 | oracleText on DTO |
| Auto-yield / auto-pass | HIGH | HIGH | P2 | Shortcuts + Phase strip + Backend |
| Undo (mana tapping) | MEDIUM | MEDIUM | P2 | Shortcuts + Backend |
| Deck simulation (AI vs AI) | HIGH | HIGH | P2 | Independent subsystem |
| Jumpstart pack builder | MEDIUM | HIGH | P3 | Independent subsystem |
| Jumpstart dual-pack setup | MEDIUM | MEDIUM | P3 | Pack builder |

## Competitor Feature Analysis

| Feature | Arena | MTGO | Forge Desktop | Moxfield | Our v2.0 Approach |
|---------|-------|------|---------------|----------|-------------------|
| Game log | No in-game log. File-based logs only. 3rd-party trackers (Arena Tutor, MTGA Tool) fill the gap. | Sidebar text log with chat window. Toggle via chat icon. Resize with Alt+PgUp/PgDn. | Full log panel with verbosity levels (`GameLogVerbosity`). | N/A | Collapsible sidebar streaming `GameLog` entries via WebSocket. Verbosity toggle (concise: turns+spells vs detailed: all phases+mana). |
| Keyboard shortcuts | Enter (pass), Z (undo mana), Ctrl (full control), Shift+Enter (toggle auto-pass), Q+click (multi-tap lands), L (toggle phases). | F2 (OK/pass), F4 (pass turn), F6 (yield all). | Partial hotkey support. | N/A | Enter/Space (OK), Escape (cancel), Z (undo mana), Ctrl (full control toggle). Start with 5 keys. Expand later if needed. |
| Priority indication | Glowing prompt box, phase bar segment highlight, rope timer when action on you. Blue rope animation. | Blue highlight on prompt box, chess clock timer. Prompt box is "main source of information." | Button highlighting, text prompt. | N/A | Pulsing border on ActionBar when buttons active. Active phase glow on PhaseStrip. "Your Turn" / "Waiting" text badge. No timer needed (vs AI). |
| Targeting | Arrows from source to target. Valid targets glow green. Dimmed invalid targets. Stack shows targeting arrows for spells. | Arrows in game area. Text-based targeting confirmation. | Highlighting and click-to-select. | N/A | SVG arrows (extend CombatOverlay pattern). Green glow border on valid targets via prompt payload. Dim non-targetable cards. |
| Auto-yield | Auto-pass when no playable cards. Shift+Enter toggles. Phase stops clickable on phase bar. Ctrl enables full control. | No auto-yield (always prompts). | Auto-yield per trigger, per ability. `shouldAutoYield` in IGuiGame. | N/A | Auto-pass on opponent's turn when no instant-speed plays. Full control toggle (Ctrl). Clickable phase stops on PhaseStrip. |
| Undo | Z: undo mana tapping before spell commitment. Strictly limited scope. | No undo. | Some undo via `ManaRefundService`. | N/A | Z key: undo mana tapping only. Uses engine's existing `ManaRefundService`. Match Arena's limited scope. |
| Goldfish/solitaire | Not available. | Solitaire mode available. | Goldfish mode available. | Basic playtest (draw hands only, no rules engine). | Lobby checkbox. Pass-only AI opponent. Full rules engine active (triggers, upkeep, etc. still fire). |
| Deck simulation | Not available. | Not available. | Not available as batch testing. | Not available. | **NEW differentiator.** Headless AI vs AI, configurable game count, gauntlet testing against multiple opponent decks. Stats: win rate, avg turns, mulligan rate. |
| Jumpstart format | Pack selection from curated pool. 3 choices per half. Color-weighted second pick based on first. Arena-specific pack contents. | Not available. | No native Jumpstart GameType (keyword only). | N/A | Custom 20-card pack builder. Dual-pack selection and merge. Play as Constructed with merged 40-card deck. |
| Advanced deck stats | Basic mana curve, card count. | Minimal stats. | Basic stats. | Mana curve, color dist, type breakdown, custom tags (manual categorization). | **Auto-categorize** removal/ramp/draw from oracle text patterns. No manual tagging. Density percentages alongside counts. Extends existing StatsPanel charts. |
| Oracle text | Hover shows card image (text embedded in image art). | Card text visible in card detail panel. | Oracle text in card detail view. | Full oracle text display alongside image. | Show oracle text panel alongside hover preview image in both deck builder and game board. Essential when images are small or fail to load. |
| Card images | Always English, default printing, high quality art. Proprietary image server. | English, specific printings per set. | Local image cache with Scryfall fallback. | Scryfall images with user-selectable printings per card. | Scryfall direct URLs with `setCode/collectorNumber` for speed and printing control. English by default (Scryfall default behavior). Fallback to name-based if set/CN unavailable. |

## Sources

- [MTG Arena Keyboard Shortcuts - Draftsim](https://draftsim.com/mtg-arena-keyboard-shortcuts/)
- [Arena Hot Keys and Interface Guide - MTG Arena Zone](https://mtgazone.com/arena-hot-keys-and-interface-guide-simplify-your-game-with-these-easy-tricks/)
- [Arena Hot Keys and Shortcuts - AetherHub](https://aetherhub.com/Article/MTG-Arena-Shortcuts-and-hidden-hotkeys)
- [Playing a Match Beginner Guide - MTG Arena Zone](https://mtgazone.com/playing-a-match/)
- [MTGO Getting Started Gameplay](https://www.mtgo.com/getting-started/getting-started-gameplay)
- [Forge MTG Beginner's Guide - Draftsim](https://draftsim.com/forge-mtg/)
- [5 Best Deck Testers for Magic - Draftsim](https://draftsim.com/mtg-deck-tester/)
- [Moxfield Features Wiki](https://github.com/moxfield/moxfield-public/wiki/Features)
- [Scryfall API Card Imagery](https://scryfall.com/docs/api/images)
- [Scryfall Search Reference (prefer:, lang:)](https://scryfall.com/docs/syntax)
- [Scryfall /cards/search endpoint](https://scryfall.com/docs/api/cards/search)
- [Jumpstart Format - MTG Wiki](https://mtg.fandom.com/wiki/Jumpstart_(format))
- [Foundations Jumpstart on Arena](https://magic.wizards.com/en/news/mtg-arena/foundations-jumpstart)
- [Picking Jumpstart Packs on Arena - Card Kingdom](https://blog.cardkingdom.com/picking-jumpstart-packs-on-arena/)
- Forge engine source: `GameLog.java`, `GameLogFormatter.java`, `GameType.java`, `ManaRefundService.java`, `ManaPool.java`, `PhaseHandler.java`, `IGuiGame.java`
- Forge web client source: `ActionBar.tsx`, `GameBoard.tsx`, `StatsPanel.tsx`, `GameCardImage.tsx`, `gameStore.ts`, `deck-stats.ts`

---
*Feature research for: Forge Web Client v2.0 -- gameplay UX polish, Jumpstart, simulation, card quality*
*Researched: 2026-03-20*
