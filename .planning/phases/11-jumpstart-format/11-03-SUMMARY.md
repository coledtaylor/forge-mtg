---
phase: 11-jumpstart-format
plan: 03
subsystem: ui, api
tags: [react, jumpstart, lobby, pack-picker, websocket, game-start]

requires:
  - phase: 11-jumpstart-format plan 01
    provides: JumpstartPack types, useJumpstartPacks hook, GameStartConfig pack fields, /api/jumpstart/packs endpoint
  - phase: 11-jumpstart-format plan 02
    provides: Jumpstart format option in lobby, deck builder Jumpstart awareness
provides:
  - PackPicker component for dual-pack selection in lobby
  - Jumpstart-conditional lobby UI with side-by-side pack pickers
  - Backend pack merge logic in handleStartGame for Jumpstart format
  - AI random pack selection from built-in JMP/J22 templates
affects: [12-simulation]

tech-stack:
  added: []
  patterns: [format-conditional lobby rendering, pack-merge game setup]

key-files:
  created:
    - forge-gui-web/frontend/src/components/lobby/PackPicker.tsx
  modified:
    - forge-gui-web/frontend/src/components/lobby/GameLobby.tsx
    - forge-gui-web/src/main/java/forge/web/WebServer.java

key-decisions:
  - "PackPicker reuses DeckPicker visual style (border-l highlight, color dots, badges) for consistency"
  - "Lobby widens to 720px for Jumpstart to accommodate dual side-by-side pickers"
  - "Inline AI difficulty selector for Jumpstart instead of AiSettings (no AI deck picker needed)"
  - "loadPackByName tries user deck files first, then built-in SealedTemplate packs"
  - "AI gets 2 random distinct packs from all JMP/J22 templates every game"

patterns-established:
  - "Format-conditional component swap: isJumpstart toggles between PackPicker pair and DeckPicker+AiSettings"
  - "Pack loading fallback: user deck -> built-in template via UnOpenedProduct"

requirements-completed: [JUMP-02, JUMP-03, JUMP-04, JUMP-05]

duration: 3min
completed: 2026-03-21
---

# Phase 11 Plan 03: Jumpstart Game Setup Summary

**Dual pack picker lobby UI with backend pack-merge into 40-card Constructed decks for both player and AI**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-21T04:00:12Z
- **Completed:** 2026-03-21T04:03:26Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- PackPicker component with user packs and built-in packs sections, matching DeckPicker visual style
- GameLobby conditionally renders dual side-by-side pack pickers when Jumpstart format selected
- Backend merges player's two selected packs into a single 40-card deck using GameType.Constructed
- AI randomly picks 2 distinct built-in JMP/J22 packs and merges them each game

## Task Commits

Each task was committed atomically:

1. **Task 1: PackPicker component and Jumpstart lobby UI** - `bc6e63a` (feat)
2. **Task 2: Backend pack merge in handleStartGame** - `f04cbc0` (feat)

## Files Created/Modified
- `forge-gui-web/frontend/src/components/lobby/PackPicker.tsx` - Pack selection list with user and built-in pack sections
- `forge-gui-web/frontend/src/components/lobby/GameLobby.tsx` - Jumpstart-conditional dual pack picker, inline difficulty selector, pack1/pack2 in config
- `forge-gui-web/src/main/java/forge/web/WebServer.java` - Jumpstart pack merge in handleStartGame, loadPackByName, mergeRandomJumpstartPacks

## Decisions Made
- PackPicker uses same visual patterns as DeckPicker (border highlight, color dots, badges) for UI consistency
- Lobby container widens from 480px to 720px for Jumpstart to fit two pack pickers side by side
- Inline AI difficulty selector replaces AiSettings entirely for Jumpstart (no AI deck picker needed since AI gets random packs)
- loadPackByName falls back from user deck files to built-in SealedTemplate packs
- Player deck named "Jumpstart - pack1 + pack2" for clear game log identification

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Full Jumpstart vertical slice complete: pack API, deck builder awareness, lobby UI, and game start
- Phase 11 (Jumpstart Format) is fully complete
- Ready for Phase 12 (Simulation) as the final v2.0 phase

---
*Phase: 11-jumpstart-format*
*Completed: 2026-03-21*
