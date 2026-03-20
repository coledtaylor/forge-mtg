# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.0 — MVP

**Shipped:** 2026-03-20
**Phases:** 6 | **Plans:** 16 | **Commits:** 108

### What Was Built
- WebSocket engine bridge wrapping Forge's Java game engine (IGuiGame/IGuiBase with 48 overrides)
- REST API (card search, deck CRUD, format validation, import/export parsing)
- Full deck builder (search, list/grid views, stats charts, commander, sideboard, import/export)
- Arena-style game board (all zones, Zustand state, real-time WebSocket play, combat, prompts)
- Seamless build-then-play loop (lobby, format/deck selection, "Play This Deck", return-to-lobby)

### What Worked
- Phase ordering was correct — Engine Bridge first eliminated the highest-risk work (sync-to-async threading, IGuiGame complexity)
- REST/WebSocket split kept phases cleanly independent — deck builder and game board had zero cross-dependencies during development
- Scryfall API for card images avoided local image management entirely
- Debounced auto-save with flushSave pattern was clean and reliable
- Plan execution velocity improved dramatically: early phases ~20min/plan, later phases ~3-4min/plan

### What Was Inefficient
- ROADMAP.md Phase 3 checkbox never got updated to [x] (cosmetic but shows state tracking gap)
- Phase 6 (import/export) was originally out of scope but added mid-milestone — scope creep, though justified
- Duplicate `findDeckFile` pattern copied between DeckHandler and DeckImportExportHandler (private method barrier)
- GameStartConfig type defined in two files rather than a shared types module

### Patterns Established
- `forge-gui-web` module structure: Java handlers in `forge.web.api`, DTOs in `forge.web.dto`, frontend in `frontend/src`
- Component file organization: `components/deck-editor/`, `components/game/`, `components/lobby/`, `components/ui/`
- Hooks pattern: `useCardSearch`, `useDeckEditor`, `useGameWebSocket` wrap all API/state logic
- SVG-based charts (mana curve, color distribution, type breakdown) — no charting library dependency
- Zustand + immer for complex mutable state (game), TanStack Query for server state (decks, cards)

### Key Lessons
1. Forge's dual input system (InputQueue buttons + CompletableFuture choices) was the hardest integration challenge — document this pattern for future maintainers
2. Javalin route registration order matters — specific routes (e.g., `/api/decks/parse`) must come before parameterized routes (`/api/decks/{name}`)
3. Record<number,T> instead of Map<number,T> avoids Zustand/immer serialization issues
4. Scryfall name-based exact match URL works but is slower than direct set/collector URLs — worth adding identifiers to CardDto later
5. Format string mapping between frontend display names and Forge's internal GameFormat names needs explicit handling

### Cost Observations
- Model mix: ~70% opus, ~30% sonnet (sonnet for verification and integration checking)
- Sessions: ~6 across 3 days
- Notable: Later phases executed 5-6x faster than early phases due to established patterns and component reuse

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Commits | Phases | Key Change |
|-----------|---------|--------|------------|
| v1.0 | 108 | 6 | Initial process — established all patterns |

### Cumulative Quality

| Milestone | LOC | Verifications | Requirements |
|-----------|-----|---------------|--------------|
| v1.0 | 11,344 | 6/6 passed | 34/34 satisfied |

### Top Lessons (Verified Across Milestones)

1. (First milestone — lessons above will be verified in subsequent milestones)
