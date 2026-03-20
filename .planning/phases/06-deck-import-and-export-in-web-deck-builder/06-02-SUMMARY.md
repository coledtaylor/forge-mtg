---
phase: 06-deck-import-and-export-in-web-deck-builder
plan: 02
subsystem: ui
tags: [react, dialog, import, export, clipboard, toast, sonner, debounce]

# Dependency graph
requires:
  - phase: 06-deck-import-and-export-in-web-deck-builder
    provides: "Backend parse and export REST endpoints (Plan 01)"
  - phase: 03-deck-builder
    provides: "DeckPanel, DeckEditor, useDeckEditor hook"
provides:
  - "ImportDeckDialog component with paste/file upload and live preview"
  - "ExportDeckDialog component with format selector and copy-to-clipboard"
  - "parseDeckText and exportDeck API client functions"
  - "importCards method on useDeckEditor hook"
  - "sonner toast notification system"
affects: []

# Tech tracking
tech-stack:
  added: [sonner]
  patterns: [debounced-api-call, file-upload-drop-zone, clipboard-api-with-fallback]

key-files:
  created:
    - forge-gui-web/frontend/src/components/deck-editor/ImportDeckDialog.tsx
    - forge-gui-web/frontend/src/components/deck-editor/ExportDeckDialog.tsx
  modified:
    - forge-gui-web/frontend/src/types/deck.ts
    - forge-gui-web/frontend/src/api/decks.ts
    - forge-gui-web/frontend/src/hooks/useDeckEditor.ts
    - forge-gui-web/frontend/src/main.tsx
    - forge-gui-web/frontend/src/components/deck-editor/DeckPanel.tsx
    - forge-gui-web/frontend/src/components/deck-editor/DeckEditor.tsx

key-decisions:
  - "mergeCards helper defined inside importCards callback to avoid mutating shared state"
  - "Debounce implemented as standalone utility with cancel method for cleanup"

patterns-established:
  - "Debounced API preview: useMemo debounce + cancel on unmount for live preview patterns"
  - "Clipboard copy with execCommand fallback for older browsers"

requirements-completed: [DECK-14]

# Metrics
duration: 3min
completed: 2026-03-20
---

# Phase 6 Plan 2: Frontend Import/Export Dialogs Summary

**Import/export modal dialogs with live color-coded preview, file upload, format dropdown, and clipboard copy using sonner toasts**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-20T16:11:05Z
- **Completed:** 2026-03-20T16:14:25Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments
- ImportDeckDialog with two-column layout: textarea + file upload drop zone on left, live color-coded preview with recognized/warning/unknown indicators on right
- ExportDeckDialog with format dropdown (generic/mtgo/arena/forge), read-only preview, and copy-to-clipboard with toast notification
- Import/Export ghost icon buttons added to DeckPanel header bar
- importCards hook method supports both replace and add-to-deck modes
- sonner toast provider wired into application root

## Task Commits

Each task was committed atomically:

1. **Task 1: Add types, API functions, install sonner, and add importCards to useDeckEditor** - `80c207cdbf` (feat)
2. **Task 2: Build ImportDeckDialog, ExportDeckDialog, and wire into DeckPanel/DeckEditor** - `3abef84476` (feat)

## Files Created/Modified
- `forge-gui-web/frontend/src/types/deck.ts` - Added ParseToken interface
- `forge-gui-web/frontend/src/api/decks.ts` - Added parseDeckText and exportDeck API functions
- `forge-gui-web/frontend/src/hooks/useDeckEditor.ts` - Added importCards method with replace/add modes
- `forge-gui-web/frontend/src/main.tsx` - Added sonner Toaster provider
- `forge-gui-web/frontend/src/components/deck-editor/ImportDeckDialog.tsx` - Two-column import modal with textarea, file upload, live preview
- `forge-gui-web/frontend/src/components/deck-editor/ExportDeckDialog.tsx` - Export modal with format selector and clipboard copy
- `forge-gui-web/frontend/src/components/deck-editor/DeckPanel.tsx` - Added Import/Export icon buttons in header
- `forge-gui-web/frontend/src/components/deck-editor/DeckEditor.tsx` - Wired dialog state and importCards callback
- `forge-gui-web/frontend/package.json` - Added sonner dependency
- `forge-gui-web/frontend/package-lock.json` - Lock file updated

## Decisions Made
- mergeCards helper defined inside the importCards callback closure to avoid mutating the additions parameter passed from the caller
- Debounce implemented as standalone function with cancel method rather than using a library, keeps the dependency footprint minimal

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 6 (Deck Import and Export) is now complete -- both backend endpoints (Plan 01) and frontend UI (Plan 02) are done
- All 6 phases of the v1.0 milestone are complete

## Self-Check: PASSED

All 8 key files verified present. Both task commits (80c207cdbf, 3abef84476) confirmed in git log.

---
*Phase: 06-deck-import-and-export-in-web-deck-builder*
*Completed: 2026-03-20*
