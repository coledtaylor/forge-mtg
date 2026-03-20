# Milestones

## v1.0 MVP (Shipped: 2026-03-20)

**Phases:** 1-6 | **Plans:** 16 | **Commits:** 108
**LOC:** 4,643 Java + 6,701 TypeScript = 11,344 total
**Timeline:** 3 days (2026-03-18 → 2026-03-20)
**Git range:** `49c2844` (feat(01-01)) → `3abef84` (feat(06-02))

**Key accomplishments:**
1. WebSocket engine bridge wrapping Forge's game engine with async CompletableFuture input, full IGuiGame/IGuiBase implementation (48 override methods)
2. REST API with card search (5 filter dimensions, pagination), deck CRUD, format validation — React/Vite/shadcn/ui frontend with Scryfall card images
3. Full deck builder with two-column editor, grouped list/grid views, mana curve/color/type charts, format validation, commander support, sideboard, basic lands
4. Arena-style game board with all 6 zones, Zustand state management, real-time WebSocket play, SVG combat overlays, prompt system, game over screen
5. Seamless build-then-play loop with lobby (format/deck selection, AI settings), "Play This Deck" from editor, return-to-lobby after game
6. Deck import/export with text paste, file drag-and-drop, live color-coded preview, 4 export formats, clipboard copy with toast notifications

**Requirements:** 34/34 v1 requirements satisfied
**Audit:** tech_debt — minor issues (format validation 400 for Casual 60-card/Jumpstart, duplicate GameStartConfig type, AI deck fallback)

**Archive:** `milestones/v1.0-ROADMAP.md`, `milestones/v1.0-REQUIREMENTS.md`, `milestones/v1.0-MILESTONE-AUDIT.md`

---

