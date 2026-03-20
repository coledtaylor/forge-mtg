# Phase 6: Deck Import and Export in Web Deck Builder - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Add deck import and export functionality to the web deck builder. Users can paste or upload deck lists in standard formats (MTGO, Arena, TappedOut, Deckstats, generic text, Forge .dck) and the system parses them using Forge's existing `DeckRecognizer`. Users can also export their deck in multiple formats via copy-to-clipboard. This phase does NOT add new deck editing capabilities — it adds I/O to the existing editor.

</domain>

<decisions>
## Implementation Decisions

### Import Input Method
- Support both paste textarea AND file upload (.dck, .dec, .txt files)
- Modal dialog with textarea on left, live preview on right
- File upload area above or below textarea (drag-and-drop or browse button)
- DeckRecognizer handles all format detection automatically — no format selector needed for import

### Import Behavior
- Ask "Replace current deck or add to it?" each import — user chooses per-import
- Live preview shows parsed results before committing — user reviews then clicks Import
- Preview updates as user types/pastes (debounced)
- Partial import allowed — recognized cards are imported, unknown cards are skipped with summary

### Import Preview Display
- Each line color-coded by DeckRecognizer token type:
  - Green check (✓) for LEGAL_CARD
  - Yellow warning (⚠) for LIMITED_CARD, CARD_FROM_NOT_ALLOWED_SET
  - Red X (✗) for UNKNOWN_CARD, UNSUPPORTED_CARD
  - Gray for COMMENT, DECK_SECTION_NAME
- Summary counts at bottom: "N recognized, N warnings, N not found"
- Import button always enabled — shows skip count if unknowns exist

### Export Formats
- Four export formats available via dropdown selector in export modal:
  1. Generic text list ("4 Lightning Bolt") — default, works everywhere
  2. MTGO format ("4 Lightning Bolt (2XM)") with Sideboard section
  3. Arena format ("4 Lightning Bolt (2XM) 123") with collector number
  4. Forge .dck format (full [metadata], [Main], [Sideboard] sections)

### Export Delivery
- Copy to clipboard — primary action button
- Export modal shows text preview (read-only textarea) with format dropdown
- Confirmation toast on successful copy
- No file download — clipboard only

### Editor Placement
- Import and Export icon buttons in DeckPanel header bar, next to existing Play button and view mode toggle
- Both open as centered modal dialogs (shadcn Dialog component)
- Available for both new and existing decks — can create a deck entirely from import

### Claude's Discretion
- Exact modal sizing and responsive behavior
- Debounce timing for live preview
- Icon choices for Import/Export buttons
- Toast styling and duration
- Whether to auto-detect deck name from import metadata

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Deck parsing (backend)
- `forge-core/src/main/java/forge/deck/DeckRecognizer.java` — Multi-format deck list parser. Handles MTGO, Arena, TappedOut, Deckstats, generic formats. Produces typed Token objects (LEGAL_CARD, UNKNOWN_CARD, LIMITED_CARD, etc.) with card resolution against Forge's database
- `forge-core/src/main/java/forge/deck/CardPool.java` §toCardList — Export serialization: "count CardName|SetCode" format
- `forge-core/src/main/java/forge/deck/io/DeckSerializer.java` — Forge .dck format serialization with metadata, sections

### Existing web deck editor (frontend)
- `forge-gui-web/frontend/src/components/deck-editor/DeckPanel.tsx` — Main deck panel with header bar (where Import/Export buttons go)
- `forge-gui-web/frontend/src/components/deck-editor/DeckEditor.tsx` — Deck editor orchestrator with save/load logic and prop wiring

### API layer
- `forge-gui-web/src/main/java/forge/web/WebServer.java` — Backend REST/WebSocket server. New import/export endpoints will be added here

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DeckRecognizer` (Java): Mature multi-format parser with token types — can be exposed via REST endpoint that returns typed parse results as JSON
- `CardPool.toCardList()`: Existing export serialization for Forge format
- `DeckSerializer.serializeDeck()`: Full .dck format with metadata sections
- shadcn `Dialog` component: Already used in the project for modals
- shadcn `Tabs` component: Used in DeckPanel for Cards/Stats/Sideboard tabs
- `useDecks` hook: Existing deck CRUD operations (save, load, create)

### Established Patterns
- REST endpoints in WebServer.java with Javalin handlers
- Frontend API calls via fetch with typed responses
- shadcn/ui + Tailwind for all UI components
- DeckPanel header already has icon buttons (Play, List/Grid toggle)

### Integration Points
- DeckPanel header: Add Import/Export buttons alongside existing controls
- WebServer.java: New POST endpoint for parsing deck text, new GET endpoint for exporting deck in specified format
- DeckEditor.tsx: Wire import results into existing deck state management (onImport callback that updates deck cards)

</code_context>

<specifics>
## Specific Ideas

- Import should feel like Moxfield's import — paste your list, see what's recognized, click import
- The desktop Forge version's DeckRecognizer is battle-tested across many formats — leverage it fully rather than reimplementing parsing on the frontend
- Export preview should show the actual text that will be copied, so users can verify before copying

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 06-deck-import-and-export-in-web-deck-builder*
*Context gathered: 2026-03-20*
