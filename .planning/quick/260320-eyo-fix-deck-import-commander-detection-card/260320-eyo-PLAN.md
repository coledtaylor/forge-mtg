# Quick Plan: Fix Deck Import Bugs

## Context

The deck import workflow has five related bugs. The `importCards` function in `useDeckEditor.ts` (1) does not detect commander from Moxfield's blank-line-delimited format and (2) leaves imported cards with empty metadata (setCode, typeLine, cmc, colors all empty/zero) because the localDeck is never reset to pick up server-resolved data. The `ImportDeckDialog.tsx` needs (3) investigation of error card rendering (code looks correct but may have a data path issue), (4) a warning toast listing unfound cards, and (5) overflow containment for large deck lists.

The `useUpdateDeck` hook in `useDecks.ts` already invalidates the `['deck', name]` query on success (line 34), so resetting `localDeck` to `null` after import-save will cause the `useEffect` (line 35-39 of `useDeckEditor.ts`) to re-populate from `serverDeck` with full card metadata.

## Tasks

### Task 1: Fix importCards — Commander Detection and Metadata Refresh [x]

<files>
forge-gui-web/frontend/src/hooks/useDeckEditor.ts
</files>

<action>
Two changes to `importCards` and its surrounding context:

**Commander detection (Moxfield format):** When the parsed tokens have no `section === 'Commander'` assignments AND the deck format is commander, detect the Moxfield pattern: cards before the first blank line (a token with empty `text`/`cardName` that isn't a section header) become the commander, cards after become main deck. To enable this, add `format` as a parameter to `useDeckEditor` (threaded from `DeckEditor.tsx` which already has it), and pass it into `importCards` or make it available via closure. The detection logic: if `isCommanderFormat && no tokens have section === 'Commander'`, scan for the first non-card token (COMMENT with empty text, UNKNOWN_TEXT with blank, etc.) that acts as a blank-line separator. Cards before it go to commander, cards after go to main. If no blank-line separator exists, use the first card as commander. Only apply this heuristic when importing into a commander-format deck.

**Metadata refresh:** After `scheduleSave(updated)` completes (i.e., in the `onSuccess` callback of the mutation), reset `localDeck` to `null` so the `useEffect` on line 35-39 re-syncs from the server query (which will have been invalidated). The cleanest approach: have `importCards` call `scheduleSave` as it does now, but add a new mechanism — after the debounced save's `onSuccess` fires, set `localDeck` to `null`. This can be done by adding an optional `refetchAfterSave` flag to `scheduleSave`, or by having `importCards` directly call `updateMutation.mutate` (bypassing the debounce timer) with an `onSuccess` that sets `localDeck(null)`. The immediate-save approach is better for imports since there's no reason to debounce a bulk import.
</action>

<verify>
1. Import a Moxfield-format commander deck (commander card, blank line, 99 main deck cards) into a Commander-format deck. The first card(s) before the blank line should appear in the Commander zone, not main deck.
2. After import completes, hover over imported cards — they should show Scryfall preview images (proving metadata was populated from server refetch).
3. Mana curve, color identity, and card categorization should all reflect real card data, not zeros/empty.
</verify>

<done>
- Moxfield blank-line commander pattern is detected and cards are assigned to Commander section when importing into a commander-format deck
- When no blank line exists in a commander deck import, the first card becomes commander
- Non-commander format imports are unaffected (no commander detection applied)
- After import saves to backend, localDeck resets and re-fetches from server, populating full card metadata (setCode, typeLine, cmc, colors)
</done>

### Task 2: Import Dialog — Error Visibility, Unfound Toast, and Overflow Fix [x]

<files>
forge-gui-web/frontend/src/components/deck-editor/ImportDeckDialog.tsx
</files>

<action>
Three fixes in ImportDeckDialog:

**Error card rendering investigation:** The `renderTokenIcon` and `getTokenTextClass` functions look correct for UNKNOWN_CARD/UNSUPPORTED_CARD types. The likely issue is that unknown cards have `cardName: null`, so the render path shows `token.text` — but `token.text` may also be empty/null from the backend parser. Add a fallback: if both `cardName` and `text` are empty/null, show the raw line. Also verify that the `text` field is populated for unknown tokens by checking the backend parse response. If the display text is empty, the red-styled row renders as an invisible empty line. Ensure at minimum the quantity and some identifier text always renders for error tokens.

**Unfound card toast:** In `handleImport`, before calling `onImport`, collect the names of all UNKNOWN_CARD and UNSUPPORTED_CARD tokens from `parseResult`. After calling `onImport` and closing the dialog, if there are unfound cards, show a warning toast (using `toast.warning` from sonner) listing them: "X cards not found: CardA, CardB, CardC" (truncate the list if more than 5, e.g., "and N more"). Keep the existing success toast but change it to only show when there are zero unfound cards; otherwise the warning toast serves as the primary feedback.

**Dialog overflow:** On the `DialogContent` element (line 157), add `flex flex-col` to the className. On the grid container div (line 162), add `min-h-0 flex-1 overflow-hidden`. On the `DialogHeader` and `DialogFooter`, add `shrink-0`. This ensures the two-column grid can shrink and scroll internally without the dialog overflowing the viewport.
</action>

<verify>
1. Import a deck list containing intentionally misspelled card names — verify they appear in the preview with red text and XCircle icon, with visible text identifying which card failed.
2. After importing a list with unfound cards, verify a warning toast appears listing the unfound card names.
3. Import a 100-card deck list — verify the dialog does not overflow the viewport; the preview column scrolls internally.
</verify>

<done>
- Error tokens (UNKNOWN_CARD, UNSUPPORTED_CARD) always render visible text in red with XCircle icon, even when cardName is null
- After import, a warning toast lists unfound card names (up to 5, with "and N more" overflow)
- Success toast only shows when all cards were found
- Dialog content uses flex layout with overflow containment; large deck lists scroll within the preview without pushing the dialog beyond 80vh
</done>

### Task 3: Wire format into useDeckEditor for commander detection [x]

<files>
forge-gui-web/frontend/src/components/deck-editor/DeckEditor.tsx
</files>

<action>
Update `DeckEditor.tsx` to pass the deck `format` to `useDeckEditor`. Currently `useDeckEditor(deckName)` only receives the deck name. Change the call to pass format as well: `useDeckEditor(deckName, format)`. Update the `useDeckEditor` function signature accordingly (this connects to Task 1's changes). The `importCards` function inside the hook can then use the format to decide whether to apply commander detection logic.
</action>

<verify>
TypeScript compilation succeeds with no errors after both Task 1 and Task 3 changes are applied. The `format` parameter flows from DeckEditor through useDeckEditor to importCards logic.
</verify>

<done>
- `useDeckEditor` accepts an optional `format` parameter
- `DeckEditor` passes its `format` prop through to `useDeckEditor`
- No TypeScript compilation errors
</done>
