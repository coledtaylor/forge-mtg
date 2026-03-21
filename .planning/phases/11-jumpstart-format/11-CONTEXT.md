# Phase 11: Jumpstart Format - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Add full Jumpstart format support: 20-card pack creation in the deck builder, browsing Forge's built-in pack definitions, dual-pack game setup for both player and AI, and format-specific validation. This is a vertical slice touching deck builder, lobby, and backend.

</domain>

<decisions>
## Implementation Decisions

### Pack Builder
- Reuse the existing DeckEditor with Jumpstart-specific constraints: 20-card target instead of 60, format = "Jumpstart", no sideboard tab, validation enforces exactly 20 cards
- No new specialized view — same editor, different rules
- "Browse Packs" button in the deck list when Jumpstart format is selected — shows Forge's pre-built packs from `StaticData.getSpecialBoosters()` with theme names
- User can select a Forge pack to use directly or copy it to edit
- Packs saved as regular .dck files with format comment "Jumpstart"

### Game Setup Flow
- When Jumpstart is selected in the lobby: replace the single deck picker with two side-by-side pack pickers ("Pack 1" and "Pack 2")
- Each picker shows user's custom Jumpstart packs + Forge's built-in packs
- No merged deck preview — just show the two selected pack names and Start button (surprise factor, like paper Jumpstart)
- Start button enabled only when both packs are selected
- Validation: exactly 2 packs selected before game can start

### AI Pack Selection
- AI randomly picks 2 packs from Forge's built-in pack pool — different every game
- Matches the paper Jumpstart experience (random packs)

### Game Engine Integration
- No `GameType.Jumpstart` — use `GameType.Constructed` with merged 40-card deck
- Backend merges the two selected packs into a single 40-card deck at game start
- Same for AI: backend picks 2 random packs and merges them

### Claude's Discretion
- Pack browser UI layout (grid vs list of packs)
- How to present pack themes (icon, color indicator, card count)
- Exact API endpoints for listing available packs
- How to handle the 20-card validation message in the editor

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Deck Builder
- `forge-gui-web/frontend/src/components/deck-editor/DeckEditor.tsx` — Main editor to adapt for Jumpstart constraints
- `forge-gui-web/frontend/src/components/DeckList.tsx` — Deck list where "Browse Packs" button goes
- `forge-gui-web/frontend/src/hooks/useDeckEditor.ts` — Editor hook with format-aware validation

### Game Setup
- `forge-gui-web/frontend/src/components/lobby/GameLobby.tsx` — Lobby to replace deck picker with dual-pack picker for Jumpstart
- `forge-gui-web/frontend/src/components/lobby/DeckPicker.tsx` — Existing picker pattern to adapt
- `forge-gui-web/frontend/src/types/game.ts` — GameStartConfig needs pack fields

### Backend
- `forge-gui-web/src/main/java/forge/web/WebServer.java` — handleStartGame needs pack-merge logic
- `forge-gui-web/src/main/java/forge/web/api/FormatValidationHandler.java` — Jumpstart validation (20 cards, Phase 7 already returns 200)

### Research
- `.planning/research/STACK.md` — No GameType.Jumpstart, use Constructed
- `.planning/research/ARCHITECTURE.md` — `AdventureEventController.getJumpstartBoosters()` reference, `StaticData.getSpecialBoosters()`
- `.planning/research/PITFALLS.md` — No engine Jumpstart GameType, application-level approach

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DeckPicker.tsx` — Radio-style selection list. Can be reused/adapted for pack picker
- `DeckEditor.tsx` — Full editor with format-aware behavior. Jumpstart = same editor, different constraints
- `GameLobby.tsx` — Format selector already has "Jumpstart" option (from Phase 7)
- `FormatValidationHandler.java` — Already handles "Jumpstart" format (returns 200 with 20-card validation from Phase 7)

### Established Patterns
- Format-conditional UI in GameLobby (Commander shows commander-specific options)
- DeckPicker filters decks by format via `matchesFormat()`
- GameStartConfig carries deckName/format/aiDifficulty through to backend

### Integration Points
- `GameLobby.tsx` — Needs Jumpstart-specific UI branch (dual pack picker instead of deck picker)
- `WebServer.handleStartGame()` — Needs pack-merge logic when format is Jumpstart
- `GameStartConfig` — Needs optional `pack1` and `pack2` fields for Jumpstart
- New REST endpoint: `GET /api/jumpstart/packs` — list available Forge packs

</code_context>

<specifics>
## Specific Ideas

- Pack names only in lobby (no merged preview) — like paper Jumpstart where you open packs and play blind
- AI gets random packs every game — keeps it fresh
- Browse Packs should feel like browsing precons — themed names, easy to pick

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 11-jumpstart-format*
*Context gathered: 2026-03-21*
