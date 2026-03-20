---
phase: 06-deck-import-and-export-in-web-deck-builder
plan: 01
subsystem: api
tags: [rest, deck-import, deck-export, DeckRecognizer, parse, mtgo, arena, forge-format]

requires:
  - phase: 01-engine-bridge
    provides: FModel headless init and Javalin server setup
  - phase: 03-deck-builder
    provides: DeckHandler CRUD routes and DTO patterns

provides:
  - POST /api/decks/parse endpoint wrapping DeckRecognizer.parseCardList
  - GET /api/decks/{name}/export endpoint with generic/mtgo/arena/forge formats
  - ParseTokenDto mapping DeckRecognizer.Token to JSON

affects: [06-02, deck-builder-frontend, import-export-ui]

tech-stack:
  added: []
  patterns: [DeckRecognizer wrapping for web, multi-format deck export]

key-files:
  created:
    - forge-gui-web/src/main/java/forge/web/api/DeckImportExportHandler.java
    - forge-gui-web/src/main/java/forge/web/dto/ParseTokenDto.java
  modified:
    - forge-gui-web/src/main/java/forge/web/WebServer.java

key-decisions:
  - "Replicated findDeckFile pattern from DeckHandler (private method cannot be shared)"
  - "Used switch expression for format dispatch in export endpoint"

patterns-established:
  - "Multi-format export: format parameter selects output formatter"

requirements-completed: [DECK-14]

duration: 3min
completed: 2026-03-20
---

# Phase 6 Plan 01: Deck Import Parse and Export REST Endpoints Summary

**REST endpoints for deck import parsing via DeckRecognizer and four-format deck export (generic, MTGO, Arena, Forge .dck)**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-20T16:05:56Z
- **Completed:** 2026-03-20T16:08:51Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- POST /api/decks/parse endpoint wraps DeckRecognizer.parseCardList to tokenize raw deck text into typed JSON tokens
- GET /api/decks/{name}/export?format=X endpoint formats saved decks in four output formats
- ParseTokenDto maps all DeckRecognizer.Token fields (type, quantity, text, cardName, setCode, collectorNumber, section) to JSON
- Routes registered before generic {name} routes to avoid Javalin path collision

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ParseTokenDto and DeckImportExportHandler** - `58a9b660cf` (feat)
2. **Task 2: Register parse and export routes in WebServer** - `7e817f78a7` (feat)

## Files Created/Modified
- `forge-gui-web/src/main/java/forge/web/dto/ParseTokenDto.java` - DTO mapping DeckRecognizer.Token to JSON fields
- `forge-gui-web/src/main/java/forge/web/api/DeckImportExportHandler.java` - Parse and export REST handlers with four format methods
- `forge-gui-web/src/main/java/forge/web/WebServer.java` - Route registration for parse and export endpoints

## Decisions Made
- Replicated findDeckFile pattern from DeckHandler since it is private and cannot be shared
- Used switch expression for clean format dispatch in export endpoint

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Parse and export endpoints ready for frontend consumption in Plan 02
- Frontend can call POST /api/decks/parse for live import preview
- Frontend can call GET /api/decks/{name}/export?format=X for deck export

---
*Phase: 06-deck-import-and-export-in-web-deck-builder*
*Completed: 2026-03-20*
