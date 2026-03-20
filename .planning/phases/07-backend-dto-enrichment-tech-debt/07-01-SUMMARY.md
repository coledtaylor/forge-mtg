---
phase: 07-backend-dto-enrichment-tech-debt
plan: 01
subsystem: api
tags: [scryfall, card-images, dto, java, typescript]

# Dependency graph
requires:
  - phase: 03-game-engine-bridge
    provides: CardDto and GameStateDto serialization pipeline
provides:
  - CardDto enriched with setCode and collectorNumber via preferred-printing resolution
  - Scryfall set/collector-number image URLs with &lang=en enforcement
  - Name-based fallback for tokens and generated cards
affects: [game-board-rendering, card-hover-preview]

# Tech tracking
tech-stack:
  added: []
  patterns: [preferred-printing-resolution-via-CardArtPreference]

key-files:
  created: []
  modified:
    - forge-gui-web/src/main/java/forge/web/dto/CardDto.java
    - forge-gui-web/src/test/java/forge/web/DtoSerializationTest.java
    - forge-gui-web/frontend/src/lib/scryfall.ts
    - forge-gui-web/frontend/src/lib/gameTypes.ts
    - forge-gui-web/frontend/src/components/game/GameCardImage.tsx
    - forge-gui-web/frontend/src/components/game/GameCard.tsx
    - forge-gui-web/frontend/src/components/game/HandCard.tsx
    - forge-gui-web/frontend/src/components/game/StackPanel.tsx
    - forge-gui-web/frontend/src/components/game/ZoneOverlay.tsx
    - forge-gui-web/frontend/src/components/game/ZonePile.tsx

key-decisions:
  - "Used CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY for preferred printing -- Forge's built-in edition ranking"
  - "Wrapped preferred-printing lookup in try-catch so tokens/generated cards gracefully produce null setCode/collectorNumber"
  - "Added setCode/collectorNumber to CardDto TypeScript interface in gameTypes.ts to match backend enrichment"

patterns-established:
  - "Preferred-printing resolution: use CardDb.getCardFromEditions() with LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY in DTO from() methods"
  - "Scryfall URL strategy: set/collector when available, name-based fallback for tokens, always &lang=en"

requirements-completed: [CARD-01, CARD-02, CARD-03]

# Metrics
duration: 12min
completed: 2026-03-20
---

# Phase 7 Plan 1: CardDto Enrichment Summary

**CardDto enriched with setCode/collectorNumber via Forge's preferred-printing resolution; all Scryfall URLs use set/collector-number pattern with &lang=en**

## Performance

- **Duration:** 12 min
- **Started:** 2026-03-20T21:29:41Z
- **Completed:** 2026-03-20T21:41:48Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments
- CardDto.from() now resolves preferred printing using Forge's CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY and populates setCode/collectorNumber
- Frontend GameCardImage uses direct Scryfall set/collector-number URLs instead of name-based lookup
- All Scryfall image URLs include &lang=en for English-only art
- Tokens and generated cards gracefully fall back to name-based URLs (no crashes)

## Task Commits

Each task was committed atomically:

1. **Task 1: Enrich CardDto with setCode, collectorNumber, and preferred-printing resolution** - `d2190ff905` (feat)
2. **Task 2: Update frontend scryfall.ts and GameCardImage.tsx for set/collector URLs with lang=en** - `1d0cc43ec7` (feat)

## Files Created/Modified
- `forge-gui-web/src/main/java/forge/web/dto/CardDto.java` - Added setCode/collectorNumber fields and preferred-printing resolution in from() method
- `forge-gui-web/src/test/java/forge/web/DtoSerializationTest.java` - Added setCode/collectorNumber to round-trip test and new null-field token test
- `forge-gui-web/frontend/src/lib/scryfall.ts` - Added &lang=en to getScryfallImageUrl
- `forge-gui-web/frontend/src/lib/gameTypes.ts` - Added setCode/collectorNumber to CardDto TypeScript interface
- `forge-gui-web/frontend/src/components/game/GameCardImage.tsx` - Rewrote to accept setCode/collectorNumber props, use set/collector URL with name-based fallback
- `forge-gui-web/frontend/src/components/game/GameCard.tsx` - Pass setCode/collectorNumber to GameCardImage
- `forge-gui-web/frontend/src/components/game/HandCard.tsx` - Pass setCode/collectorNumber to GameCardImage
- `forge-gui-web/frontend/src/components/game/StackPanel.tsx` - Pass setCode/collectorNumber to GameCardImage
- `forge-gui-web/frontend/src/components/game/ZoneOverlay.tsx` - Pass setCode/collectorNumber to GameCardImage
- `forge-gui-web/frontend/src/components/game/ZonePile.tsx` - Pass setCode/collectorNumber to GameCardImage

## Decisions Made
- Used CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY for preferred printing -- Forge's built-in edition ranking handles all edge cases
- Wrapped preferred-printing lookup in try-catch so tokens/generated cards gracefully produce null setCode/collectorNumber
- Added setCode/collectorNumber to CardDto TypeScript interface in gameTypes.ts to match backend enrichment

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added setCode/collectorNumber to CardDto TypeScript interface**
- **Found during:** Task 2 (Frontend update)
- **Issue:** Plan specified updating GameCardImage.tsx and scryfall.ts but did not mention updating the CardDto TypeScript interface in gameTypes.ts to include the new fields
- **Fix:** Added `setCode: string | null` and `collectorNumber: string | null` to the CardDto interface in gameTypes.ts
- **Files modified:** forge-gui-web/frontend/src/lib/gameTypes.ts
- **Verification:** TypeScript compilation passes
- **Committed in:** 1d0cc43ec7 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** Essential for type safety -- without the TypeScript interface update, passing setCode/collectorNumber props would cause type errors.

## Issues Encountered
- Maven cached a failed parent POM resolution from central; resolved by building with `-am` flag to build dependencies from source
- Pre-existing checkstyle violations in WebGuiGame.java and WebServer.java (unused imports); skipped with `-Dcheckstyle.skip=true` since not caused by this plan's changes

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- CardDto enrichment complete, game board images will now use direct set/collector-number Scryfall URLs
- Plans 07-02 and 07-03 can proceed (format validation fix and AI deck bundling)

---
*Phase: 07-backend-dto-enrichment-tech-debt*
*Completed: 2026-03-20*
