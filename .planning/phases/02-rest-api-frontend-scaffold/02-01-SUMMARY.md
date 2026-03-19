---
phase: 02-rest-api-frontend-scaffold
plan: 01
subsystem: api
tags: [javalin, rest, card-search, deck-crud, scryfall, dto]

# Dependency graph
requires:
  - phase: 01-engine-bridge
    provides: "WebServer with Javalin + FModel initialization, existing DTO pattern"
provides:
  - "GET /api/cards endpoint with name, color, type, cmc, format filters and pagination"
  - "Deck CRUD endpoints: GET/POST /api/decks, GET/PUT/DELETE /api/decks/{name}"
  - "CardSearchDto with setCode + collectorNumber for Scryfall image URLs"
  - "DeckSummaryDto and DeckDetailDto for deck browser and editor views"
affects: [02-rest-api-frontend-scaffold, 03-deck-builder, 05-polish-deploy]

# Tech tracking
tech-stack:
  added: []
  patterns: [predicate-based card filtering, recursive deck file scanning, static handler methods]

key-files:
  created:
    - forge-gui-web/src/main/java/forge/web/dto/CardSearchDto.java
    - forge-gui-web/src/main/java/forge/web/dto/DeckSummaryDto.java
    - forge-gui-web/src/main/java/forge/web/dto/DeckDetailDto.java
    - forge-gui-web/src/main/java/forge/web/api/CardSearchHandler.java
    - forge-gui-web/src/main/java/forge/web/api/DeckHandler.java
    - forge-gui-web/src/test/java/forge/web/CardSearchHandlerTest.java
    - forge-gui-web/src/test/java/forge/web/DeckHandlerTest.java
  modified:
    - forge-gui-web/src/main/java/forge/web/WebServer.java

key-decisions:
  - "Used CardRulesPredicates.coreType(String) variant instead of (boolean, CoreType) -- cleaner API, same result"
  - "ComparableOp uses GT_OR_EQUAL/LT_OR_EQUAL (not GREATER_OR_EQUAL/LESS_OR_EQUAL as plan suggested)"
  - "Tests placed in forge.web package (not forge.web.api) to access package-private WebServer.createApp()"

patterns-established:
  - "REST handler pattern: static methods in final utility class, one per HTTP method"
  - "DTO factory pattern: static from() methods with PaperCard or Deck input"
  - "Predicate composition for card search filtering (name, color, type, cmc, format)"

requirements-completed: [API-01, API-02]

# Metrics
duration: 12min
completed: 2026-03-19
---

# Phase 02 Plan 01: REST API Summary

**Card search with predicate-based filtering/pagination and deck CRUD persisting .dck files via DeckSerializer**

## Performance

- **Duration:** 12 min
- **Started:** 2026-03-19T15:18:56Z
- **Completed:** 2026-03-19T15:31:09Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments
- Card search endpoint supports 5 filter dimensions (name, color, type, cmc, format) with pagination
- Search results include setCode and collectorNumber enabling Scryfall image URL construction
- Full deck CRUD (create, list, get, update, delete) persisting to .dck format via DeckSerializer
- 8 new integration tests, all 32 tests pass (24 existing + 8 new)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create search and deck DTOs** - `02a552d665` (feat)
2. **Task 2: Create REST handlers, register routes, and add integration tests** - `a9b9d41805` (feat)

## Files Created/Modified
- `forge-gui-web/src/main/java/forge/web/dto/CardSearchDto.java` - Search result DTO with Scryfall identifiers
- `forge-gui-web/src/main/java/forge/web/dto/DeckSummaryDto.java` - Deck list item DTO with name, count, colors
- `forge-gui-web/src/main/java/forge/web/dto/DeckDetailDto.java` - Full deck DTO with card entries by section
- `forge-gui-web/src/main/java/forge/web/api/CardSearchHandler.java` - GET /api/cards with filtering and pagination
- `forge-gui-web/src/main/java/forge/web/api/DeckHandler.java` - Deck CRUD handler (list, create, get, update, delete)
- `forge-gui-web/src/main/java/forge/web/WebServer.java` - Route registration for 6 new endpoints
- `forge-gui-web/src/test/java/forge/web/CardSearchHandlerTest.java` - 4 card search integration tests
- `forge-gui-web/src/test/java/forge/web/DeckHandlerTest.java` - 4 deck CRUD integration tests

## Decisions Made
- Used `CardRulesPredicates.coreType(String)` which accepts type name directly, avoiding enum import
- ComparableOp enum values are GT_OR_EQUAL/LT_OR_EQUAL (plan referenced non-existent GREATER_OR_EQUAL names)
- Test classes placed in `forge.web` package to access package-private `WebServer.createApp()` and `getActiveSessions()`, matching the existing GameLoopIntegrationTest pattern

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed unused CardType import from CardSearchHandler**
- **Found during:** Task 2
- **Issue:** `CardRulesPredicates.coreType()` accepts a String directly, so the CardType import was unused and triggered checkstyle failure
- **Fix:** Removed the unused import
- **Files modified:** CardSearchHandler.java
- **Committed in:** a9b9d41805 (Task 2 commit)

**2. [Rule 3 - Blocking] Moved test files from forge.web.api to forge.web package**
- **Found during:** Task 2
- **Issue:** WebServer.createApp() and getActiveSessions() are package-private, inaccessible from forge.web.api test package
- **Fix:** Moved test files to forge.web package matching existing test pattern
- **Files modified:** CardSearchHandlerTest.java, DeckHandlerTest.java
- **Committed in:** a9b9d41805 (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (1 bug, 1 blocking)
**Impact on plan:** Both fixes necessary for compilation. No scope creep.

## Issues Encountered
None beyond the auto-fixed deviations.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- REST API ready for frontend scaffold (Plan 02) to consume
- Card search returns paginated results with Scryfall image data
- Deck CRUD enables save/load workflow for deck builder

---
*Phase: 02-rest-api-frontend-scaffold*
*Completed: 2026-03-19*
