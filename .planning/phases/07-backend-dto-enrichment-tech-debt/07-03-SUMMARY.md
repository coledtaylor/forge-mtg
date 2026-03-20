---
phase: 07-backend-dto-enrichment-tech-debt
plan: 03
subsystem: api
tags: [ai-decks, deck-loading, resource-bundling, game-startup]

requires:
  - phase: 04-game-play-loop
    provides: WebServer game startup and deck loading infrastructure
provides:
  - 9 curated AI deck files across Standard, Casual 60-card, and Commander formats
  - Startup-time deck installation from bundled resources
  - Any-deck fallback replacing 60-Forests garbage
affects: [game-play-loop, format-support]

tech-stack:
  added: []
  patterns: [resource-bundled-deck-installation, any-deck-fallback]

key-files:
  created:
    - forge-gui-web/src/main/resources/ai-decks/standard/mono-red-aggro.dck
    - forge-gui-web/src/main/resources/ai-decks/standard/azorius-control.dck
    - forge-gui-web/src/main/resources/ai-decks/standard/golgari-midrange.dck
    - forge-gui-web/src/main/resources/ai-decks/casual/mono-green-stompy.dck
    - forge-gui-web/src/main/resources/ai-decks/casual/burn.dck
    - forge-gui-web/src/main/resources/ai-decks/casual/white-weenie.dck
    - forge-gui-web/src/main/resources/ai-decks/commander/atraxa.dck
    - forge-gui-web/src/main/resources/ai-decks/commander/krenko.dck
    - forge-gui-web/src/main/resources/ai-decks/commander/tatyova.dck
  modified:
    - forge-gui-web/src/main/java/forge/web/WebServer.java

key-decisions:
  - "Used Logger (tinylog) instead of System.err for all warning/info messages"
  - "Decks use common tournament staples with approximate set codes (Forge resolves by name)"
  - "Commander decks use [Commander] section per .dck format convention"

patterns-established:
  - "Resource bundling: .dck files in src/main/resources/ai-decks/{format}/ copied to DECK_CONSTRUCTED_DIR on startup"
  - "No-overwrite install: skip if target file already exists, allowing user customization"

requirements-completed: [DEBT-03]

duration: 6min
completed: 2026-03-20
---

# Phase 7 Plan 3: AI Deck Bundling Summary

**9 curated AI decks bundled across 3 formats with startup installation and any-deck fallback replacing 60-Forests garbage**

## Performance

- **Duration:** 6 min
- **Started:** 2026-03-20T21:30:00Z
- **Completed:** 2026-03-20T21:36:17Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments
- Bundled 9 curated AI decks: 3 Standard (Mono-Red Aggro, Azorius Control, Golgari Midrange), 3 Casual (Mono-Green Stompy, Burn, White Weenie), 3 Commander (Atraxa Superfriends, Krenko Goblins, Tatyova Lands)
- Added installBundledAiDecks() to WebServer that copies deck resources to DECK_CONSTRUCTED_DIR on startup (no-overwrite)
- Removed getDefaultAiDeck() (60 Forests) entirely; pickRandomDeck() now falls back to any available deck
- Added null safety for AI deck resolution in handleStartGame()

## Task Commits

Each task was committed atomically:

1. **Task 1: Create bundled AI deck files** - `20a59227d8` (feat)
2. **Task 2: Update WebServer to install bundled decks and remove fallback** - `b954d3f744` (feat)

## Files Created/Modified
- `forge-gui-web/src/main/resources/ai-decks/standard/mono-red-aggro.dck` - Mono-Red Aggro Standard deck (60 cards)
- `forge-gui-web/src/main/resources/ai-decks/standard/azorius-control.dck` - Azorius Control Standard deck (60 cards)
- `forge-gui-web/src/main/resources/ai-decks/standard/golgari-midrange.dck` - Golgari Midrange Standard deck (60 cards)
- `forge-gui-web/src/main/resources/ai-decks/casual/mono-green-stompy.dck` - Mono-Green Stompy Casual deck (60 cards)
- `forge-gui-web/src/main/resources/ai-decks/casual/burn.dck` - Burn Casual deck (60 cards)
- `forge-gui-web/src/main/resources/ai-decks/casual/white-weenie.dck` - White Weenie Casual deck (60 cards)
- `forge-gui-web/src/main/resources/ai-decks/commander/atraxa.dck` - Atraxa Superfriends Commander deck (100 cards)
- `forge-gui-web/src/main/resources/ai-decks/commander/krenko.dck` - Krenko Goblins Commander deck (100 cards)
- `forge-gui-web/src/main/resources/ai-decks/commander/tatyova.dck` - Tatyova Lands Commander deck (100 cards)
- `forge-gui-web/src/main/java/forge/web/WebServer.java` - Added installBundledAiDecks(), removed getDefaultAiDeck(), updated pickRandomDeck() fallback

## Decisions Made
- Used Logger (tinylog) instead of System.err for all warning/info messages, consistent with existing codebase
- Commander decks use [Commander] section per Forge .dck format convention
- Decks use common tournament staples with approximate set codes; Forge resolves cards by name regardless of set code accuracy
- No-overwrite install pattern: existing files are not replaced, allowing users to customize decks

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Checkstyle plugin failure during `mvn compile` is pre-existing (726 errors across project, not caused by this plan's changes). Compilation succeeds with `-Dcheckstyle.skip=true`.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- AI opponents now play with realistic, synergistic decks across all supported formats
- Bundled decks automatically install on first startup
- Ready for downstream phases that depend on functional AI gameplay

## Self-Check: PASSED

All 10 created/modified files verified present. Both task commits (20a59227d8, b954d3f744) verified in git log.

---
*Phase: 07-backend-dto-enrichment-tech-debt*
*Completed: 2026-03-20*
