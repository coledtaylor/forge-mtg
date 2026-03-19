# Phase 4: Game Board - Context

**Gathered:** 2026-03-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Full gameplay UI for playing Magic against the AI through the browser. Renders all zones (hand, battlefield, graveyard, exile, library, stack) with real-time WebSocket updates. Handles all player prompts (choices, targeting, combat), combat declaration, and game info display. Connects to the engine bridge built in Phase 1.

</domain>

<decisions>
## Implementation Decisions

### Board Layout
- Arena-style layout: opponent at top, player at bottom, battlefield center
- Distinct lanes per player: lands row (bottom) and creatures/other permanents row (top)
- Subtle visual dividers between zones — polished, structured visual hierarchy
- Player info bars at top edge (opponent) and bottom edge (you) — thin horizontal bars with name, life total, hand count, mana pool
- Phase strip in center divider: horizontal bar showing Untap → Upkeep → Draw → Main 1 → Combat → Main 2 → End, current phase highlighted, turn ownership indicated
- Vertical stack display on the right side — spells in resolution order, top spell at top, each with card thumbnail + name

### Hand Display
- Fanned overlap with hover-to-raise — cards overlap slightly in an arc at the bottom
- Hovering raises a card above the others and shows enlarged preview (same CardHoverPreview pattern from Phase 3)
- Double-click a card in hand to cast/play it
- Single click selects/previews, double-click commits the action

### Battlefield Cards
- Tapped cards rotate 90° clockwise (standard Magic visual)
- Attachments (auras, equipment) stacked behind the card they're on, offset slightly up/right — each card in the stack supports hover-to-inspect
- Counters shown as small colored badge overlays on the card (e.g., "+1/+1 ×3")
- Separate rows: lands in bottom lane, creatures/other permanents in upper lane per player

### Graveyard & Exile
- Pile icon showing top card + count badge
- Click to expand into a scrollable overlay/panel listing all cards in that zone

### Card Detail
- Hover preview floating near cursor — reuses CardHoverPreview component and useCardHover hook from Phase 3
- Consistent hover preview across all zones (hand, battlefield, graveyard, exile, stack)

### Player Interaction & Prompts
- Bottom action bar for all prompts — fixed bar above the hand area
- Prompt text + action buttons (OK/Cancel/Pass) in the action bar — non-modal, board stays visible and interactive
- Multiple choices (modal spells, etc.) appear as buttons or a list within the action bar
- Arena-style priority: always pause when player has priority, explicitly pass with "Pass" button
- Auto-pass ONLY when player has zero playable cards and zero activatable abilities (no forced clicks when you literally can't act)
- Never auto-skip phases where the player could act — prevents the "accidentally clicked past my window" problem

### Targeting & Selection
- Valid targets highlight/glow on the board when the engine asks for a target
- Click a highlighted card to select it as the target
- Invalid cards dimmed during targeting mode
- Clear visual feedback loop: prompt in action bar → highlights on board → click to select → confirm

### Casting Spells
- Double-click a playable card in hand to cast/play it
- Engine handles mana payment automatically (auto-taps lands)
- If engine needs mana source choice (multiple sources), PROMPT_CHOICE appears in action bar
- Lands visually rotate as WebSocket sends zone updates after tapping

### Combat UI
- Declare attackers: eligible creatures highlight, click to toggle as attacker (creature shifts slightly forward + red glow), confirm with OK in action bar
- Declare blockers: eligible blockers highlight, click a blocker then click the attacker it blocks, line/arrow connects them, click blocker again to reassign, confirm with OK
- Subtle shift forward for attackers toward center line, blockers shift toward assigned attacker — signals engagement without breaking layout
- Combat damage: creatures flash red when taking damage, P/T updates in real-time, creatures that die fade out, life total changes animate (count up/down)

### Mana Pool Display
- In player info bar: colored mana symbols (W/U/B/R/G/C) with quantity next to each
- Only shows colors with available mana
- Uses mana-font CSS from Phase 3

### Claude's Discretion
- WebSocket reconnection/error handling
- Card sizing and responsive scaling within zones
- Animation timing and easing curves
- How to handle very large battlefields (many permanents)
- Game over screen design
- Sound effects (if any — likely skip for v1)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Engine Bridge (Phase 1)
- `forge-gui-web/src/main/java/forge/web/WebGuiGame.java` — IGuiGame implementation, all outbound message sending
- `forge-gui-web/src/main/java/forge/web/WebInputBridge.java` — Async input bridge for engine prompts (sendAndWait/complete pattern)
- `forge-gui-web/src/main/java/forge/web/protocol/MessageType.java` — All WebSocket message types (GAME_STATE, ZONE_UPDATE, PROMPT_CHOICE, COMBAT_UPDATE, etc.)
- `forge-gui-web/src/main/java/forge/web/protocol/InboundMessage.java` — Client-to-server message format
- `forge-gui-web/src/main/java/forge/web/protocol/OutboundMessage.java` — Server-to-client message format
- `forge-gui-web/src/main/java/forge/web/ViewRegistry.java` — Maps engine CardViews to stable IDs

### DTOs (Phase 1)
- `forge-gui-web/src/main/java/forge/web/dto/GameStateDto.java` — Full game state snapshot
- `forge-gui-web/src/main/java/forge/web/dto/PlayerDto.java` — Player state (life, mana pool, zones)
- `forge-gui-web/src/main/java/forge/web/dto/CardDto.java` — Card state (name, tapped, counters, attachments, zone)
- `forge-gui-web/src/main/java/forge/web/dto/CombatDto.java` — Combat state (attackers, blockers, assignments)
- `forge-gui-web/src/main/java/forge/web/dto/SpellAbilityDto.java` — Spell/ability on the stack
- `forge-gui-web/src/main/java/forge/web/dto/ZoneUpdateDto.java` — Incremental zone change

### Existing Frontend Assets (Phase 2/3)
- `forge-gui-web/frontend/src/components/CardImage.tsx` — Scryfall card image with lazy loading
- `forge-gui-web/frontend/src/components/deck-editor/CardHoverPreview.tsx` — Floating card preview near cursor
- `forge-gui-web/frontend/src/hooks/useCardHover.ts` — Hook for hover preview positioning
- `forge-gui-web/frontend/src/components/ManaCost.tsx` — Mana cost symbol rendering via mana-font
- `forge-gui-web/frontend/src/lib/mana.ts` — Mana string parsing utilities

### Forge Engine Interfaces
- `forge-gui/src/main/java/forge/gui/interfaces/IGuiGame.java` — All ~15 choice method signatures that need frontend handling

No external specs — requirements fully captured in decisions above and REQUIREMENTS.md (GAME-01 through GAME-11).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `CardImage` component: Scryfall images for any card, reuse for battlefield/hand/stack rendering
- `CardHoverPreview` + `useCardHover`: Floating preview pattern, reuse across all game zones
- `ManaCost` component + `mana-font`: Mana symbol rendering for mana pool display and card costs
- `mana.ts` utilities: Parse mana strings from engine DTOs
- WebSocket connection pattern: `WebServer.java` already handles `/ws/game/{gameId}` endpoint

### Established Patterns
- TanStack Query for REST data (deck list), but game board uses WebSocket — different data flow
- Tailwind CSS + shadcn/ui dark theme (CSS variables)
- Component composition pattern from Phase 3 (deck editor split layout)

### Integration Points
- `App.tsx` needs a game board view/route (currently has deck list + deck editor)
- WebSocket client needs to be built on the frontend (no existing WS client code)
- `WebServer.java` START_GAME message handler creates the game session — frontend needs to trigger this
- Engine sends `GAME_STATE` on connect, then incremental updates — frontend needs state management for the game model
- `BUTTON_OK` / `BUTTON_CANCEL` inbound messages for priority passing (already in MessageType)

</code_context>

<specifics>
## Specific Ideas

- Arena-style board with distinct land/creature lanes — not a simple tabletop layout
- Hand fans out with hover-to-raise interaction — cards should feel like they're in your hand
- Priority system always pauses when player can act, auto-passes only when nothing is possible — no more missed windows
- Combat has subtle physical shift of creatures toward center — attacking feels like attacking
- Damage causes brief red flash + life total animation — visceral feedback

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 04-game-board*
*Context gathered: 2026-03-19*
