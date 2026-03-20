# Quick Task Summary: 260320-eyo — Fix Deck Import Bugs

## Status: COMPLETE

## Tasks Completed: 3/3

### Task 1 + 3: Commander Detection, Metadata Refresh, and Format Wiring

**Files modified:**
- `forge-gui-web/frontend/src/hooks/useDeckEditor.ts` (modified)
- `forge-gui-web/frontend/src/components/deck-editor/DeckEditor.tsx` (modified)

**Changes:**
- `useDeckEditor` now accepts an optional `format` parameter, passed from `DeckEditor`
- `importCards` detects Moxfield blank-line-delimited commander format: cards before the first blank line go to Commander zone when importing into a commander-format deck
- Falls back to using the first card as commander when no blank line separator exists
- Non-commander format imports are unaffected
- After import, saves immediately (bypasses debounce) and resets `localDeck` to `null` on success, triggering a re-fetch from server with full card metadata (setCode, typeLine, cmc, colors)

**Commit:** `fix(deck-import): add commander detection and metadata refresh`

### Task 2: Import Dialog Error Visibility, Unfound Toast, and Overflow

**Files modified:**
- `forge-gui-web/frontend/src/components/deck-editor/ImportDeckDialog.tsx` (modified)

**Changes:**
- Error tokens (UNKNOWN_CARD, UNSUPPORTED_CARD) now always render visible text, falling back to "(unknown card)" when both cardName and text are empty
- After import, a warning toast lists unfound card names (up to 5, with "and N more" overflow); success toast only shows when all cards were found
- Dialog uses flex layout with `shrink-0` on header/footer and `min-h-0 flex-1 overflow-hidden` on the grid, preventing viewport overflow for large deck lists

**Commit:** `fix(import-dialog): improve error visibility, unfound toast, and overflow`

## Validation

- TypeScript compilation passes with zero errors
- All three tasks' done criteria are met
