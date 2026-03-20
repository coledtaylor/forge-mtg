---
phase: 07-backend-dto-enrichment-tech-debt
verified: 2026-03-20T22:00:00Z
status: passed
score: 12/12 must-haves verified
re_verification: false
---

# Phase 7: Backend DTO Enrichment & Tech Debt — Verification Report

**Phase Goal:** Backend data contracts are enriched so that downstream phases can build correct frontend features without rework, and v1.0 tech debt is resolved
**Verified:** 2026-03-20T22:00:00Z
**Status:** PASSED
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | CardDto includes setCode and collectorNumber fields populated from preferred printing | VERIFIED | `CardDto.java` lines 39-40 declare fields; `from()` method lines 78-92 resolve via `CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY` |
| 2 | Scryfall image URLs use set/collector-number pattern instead of name-based lookup | VERIFIED | `GameCardImage.tsx` line 18-19 branches on `setCode && collectorNumber` and calls `getScryfallImageUrl` from scryfall.ts |
| 3 | All Scryfall image URLs include `&lang=en` for English-only art | VERIFIED | `scryfall.ts` line 6 appends `&lang=en`; `GameCardImage.tsx` line 20 name-based fallback also includes `&lang=en` |
| 4 | Tokens and generated cards gracefully fall back to null setCode/collectorNumber | VERIFIED | `CardDto.java` lines 78-92 wrap resolution in try-catch; null fields route `GameCardImage` to name-based URL |
| 5 | Format validation returns 200 with legal=true for "Casual 60-card" format | VERIFIED | `FormatValidationHandler.java` lines 52-76 handle "Casual 60-card" with size/4-of checks; no 400 path |
| 6 | Format validation returns 200 with legal=true for "Jumpstart" format | VERIFIED | `FormatValidationHandler.java` lines 78-88 handle "Jumpstart" with 20-card check; no 400 path |
| 7 | Format validation returns 200 with legal=true for unrecognized formats | VERIFIED | `FormatValidationHandler.java` lines 91-95 return `legal=true` for null `GameFormat` |
| 8 | GameStartConfig interface exists in exactly one location | VERIFIED | Defined only in `src/types/game.ts`; no duplicate in `useGameWebSocket.ts` or `GameLobby.tsx` |
| 9 | AI deck selection finds curated decks for Standard format | VERIFIED | 3 `.dck` files in `ai-decks/standard/` with `Comment=Standard`; 60 cards each |
| 10 | AI deck selection finds curated decks for Casual 60-card format | VERIFIED | 3 `.dck` files in `ai-decks/casual/` with `Comment=Casual 60-card`; 60 cards each |
| 11 | AI deck selection finds curated decks for Commander format | VERIFIED | 3 `.dck` files in `ai-decks/commander/` with `Comment=Commander`; 100 cards each (99 main + 1 commander) |
| 12 | The 60-Forests getDefaultAiDeck() fallback is removed | VERIFIED | `grep "getDefaultAiDeck"` in `WebServer.java` returns no matches |

**Score:** 12/12 truths verified

---

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `forge-gui-web/src/main/java/forge/web/dto/CardDto.java` | VERIFIED | Contains `setCode`/`collectorNumber` fields and `getCardFromEditions` with `LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY` |
| `forge-gui-web/frontend/src/lib/scryfall.ts` | VERIFIED | `getScryfallImageUrl` returns URL with `&lang=en` appended |
| `forge-gui-web/frontend/src/components/game/GameCardImage.tsx` | VERIFIED | Imports `getScryfallImageUrl`; accepts `setCode?`/`collectorNumber?` props; branches on their presence |
| `forge-gui-web/src/main/java/forge/web/api/FormatValidationHandler.java` | VERIFIED | Contains "Casual 60-card" and "Jumpstart" intercept blocks before engine lookup; unknown format returns 200 |
| `forge-gui-web/frontend/src/types/game.ts` | VERIFIED | Single canonical `export interface GameStartConfig` with 4 fields |
| `forge-gui-web/src/main/java/forge/web/WebServer.java` | VERIFIED | Contains `installBundledAiDecks()` called at line 78 after FModel init; no `getDefaultAiDeck` anywhere |
| `forge-gui-web/src/main/resources/ai-decks/standard/mono-red-aggro.dck` | VERIFIED | 60-card deck, `Comment=Standard` |
| `forge-gui-web/src/main/resources/ai-decks/casual/mono-green-stompy.dck` | VERIFIED | 60-card deck, `Comment=Casual 60-card` |
| `forge-gui-web/src/main/resources/ai-decks/commander/atraxa.dck` | VERIFIED | 100-card deck (99 main + 1 commander), `Comment=Commander` |
| `forge-gui-web/src/test/java/forge/web/DtoSerializationTest.java` | VERIFIED | Contains `setCode`/`collectorNumber` assertions in `testCardDtoRoundTrip` and new `testCardDtoWithNullSetCodeRoundTrip` |
| `forge-gui-web/frontend/src/lib/gameTypes.ts` | VERIFIED | `CardDto` TypeScript interface includes `setCode: string | null` and `collectorNumber: string | null` (line 38-39) |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `CardDto.java` | `CardDb.getCardFromEditions` | Preferred printing resolution in `from()` | WIRED | Line 80: `getCardFromEditions(cv.getName(), CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY)` |
| `GameCardImage.tsx` | `scryfall.ts` | `import getScryfallImageUrl` | WIRED | Line 3 imports; line 19 calls `getScryfallImageUrl(setCode, collectorNumber)` |
| `useGameWebSocket.ts` | `types/game.ts` | `import type { GameStartConfig }` | WIRED | Line 4 imports; line 6 re-exports for backwards compatibility |
| `GameLobby.tsx` | `types/game.ts` | `import type { GameStartConfig }` | WIRED | Line 15 imports; line 17 re-exports for backwards compatibility |
| `App.tsx` | `types/game.ts` | `import type { GameStartConfig }` | WIRED | Line 7: `import type { GameStartConfig } from './types/game'` |
| `GameBoard.tsx` | `types/game.ts` | `import type { GameStartConfig }` | WIRED | Line 5: `import type { GameStartConfig } from '../../types/game'` |
| `WebServer.java` | `ai-decks/` resources | `installBundledAiDecks` on startup | WIRED | Line 78 calls method; method uses `WebServer.class.getResourceAsStream("/" + deckPath)` |
| `WebServer.pickRandomDeck` | any-deck fallback | `collectDecksByFormat(decksDir, null, allDecks)` | WIRED | Lines 434-438: null-format scan when format-specific scan is empty |
| `GameCard.tsx` | `GameCardImage` | `setCode`/`collectorNumber` props | WIRED | Lines 113, 178 pass `card.setCode` and `card.collectorNumber` |
| `HandCard.tsx` | `GameCardImage` | `setCode`/`collectorNumber` props | WIRED | Line 81 passes `card.setCode` and `card.collectorNumber` |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CARD-01 | 07-01 | Card images use direct Scryfall set/collector-number URLs instead of name-based lookups | SATISFIED | `GameCardImage.tsx` uses `getScryfallImageUrl(setCode, collectorNumber)` when fields are non-null; `CardDto.java` populates both fields |
| CARD-02 | 07-01 | Card images prefer recent core set or standard-legal printings for recognizable art | SATISFIED | `CardDto.java` uses `CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY` (line 81) — Forge's built-in preference for recent core/expansion printings |
| CARD-03 | 07-01 | Card images are English-only (no foreign-language art variants) | SATISFIED | `scryfall.ts` line 6 appends `&lang=en`; name-based fallback in `GameCardImage.tsx` line 20 also includes `&lang=en` |
| DEBT-01 | 07-02 | Format validation handles "Casual 60-card" and "Jumpstart" without returning 400 | SATISFIED | `FormatValidationHandler.java` intercepts both formats before engine lookup; neither path returns 400; unknown formats return 200 `legal=true` |
| DEBT-02 | 07-02 | GameStartConfig type consolidated into a single shared definition | SATISFIED | Defined only in `src/types/game.ts`; `useGameWebSocket.ts` and `GameLobby.tsx` import from canonical location with re-exports for compatibility |
| DEBT-03 | 07-03 | AI deck selection provides meaningful decks for all formats | SATISFIED | 9 bundled `.dck` files installed on startup; `getDefaultAiDeck()` (60 Forests) completely removed; fallback picks any available deck |

All 6 requirement IDs declared across the three PLANs are accounted for. No orphaned requirements found in REQUIREMENTS.md for Phase 7.

---

### Anti-Patterns Found

None detected. Scanned: `CardDto.java`, `FormatValidationHandler.java`, `WebServer.java` (installBundledAiDecks, pickRandomDeck), `scryfall.ts`, `GameCardImage.tsx`, `types/game.ts`.

- No TODO/FIXME/PLACEHOLDER comments in modified files
- No empty return stubs (`return null`, `return {}`, `return []` without logic)
- `pickRandomDeck` null returns are guarded at line 254 in `WebServer.java`
- Try-catch in `CardDto.from()` is intentional (token fallback), not a swallowed error

---

### Human Verification Required

The following items require runtime or visual confirmation and cannot be verified statically:

#### 1. Card Image URL Resolution End-to-End

**Test:** Start a game, observe cards on the board and in hand. Inspect network traffic.
**Expected:** Image requests go to `https://api.scryfall.com/cards/{setCode}/{collectorNumber}?format=image&version=normal&lang=en`. Images display recent, recognizable English printings rather than obscure foreign art.
**Why human:** Network calls and visual appearance cannot be verified statically.

#### 2. Token Cards Show Name-Based Fallback

**Test:** Start a game that generates tokens (e.g., a deck with token producers). Observe token card images.
**Expected:** Token images load via `https://api.scryfall.com/cards/named?exact=...&lang=en` without 404 errors or crashes.
**Why human:** Token generation requires a running game; null setCode/collectorNumber path needs runtime exercise.

#### 3. Format Validation Flow in Lobby

**Test:** Create a deck, open the lobby, select "Casual 60-card" and "Jumpstart" as formats.
**Expected:** No 400 errors in browser console; deck validation proceeds and returns a `legal` result.
**Why human:** Requires a running server and UI interaction.

#### 4. AI Deck Quality in-Game

**Test:** Start several games across Standard, Casual 60-card, and Commander formats with no AI deck name specified.
**Expected:** AI plays with a real deck from the bundled set (not 60 Forests). Deck names in logs should be identifiable (e.g., "Mono-Red Aggro").
**Why human:** Requires a running game to observe AI behavior; bundled deck content validity (card names resolve correctly in Forge's DB) can only be confirmed at runtime.

---

### Commits Verified

All 6 commits documented in SUMMARYs exist in git log:

| Commit | Plan | Description |
|--------|------|-------------|
| `d2190ff905` | 07-01 Task 1 | feat(07-01): enrich CardDto with setCode, collectorNumber via preferred-printing resolution |
| `1d0cc43ec7` | 07-01 Task 2 | feat(07-01): update frontend to use set/collector Scryfall URLs with lang=en |
| `9454119a51` | 07-02 Task 1 | fix(07-02): handle Casual 60-card, Jumpstart, and unknown format validation |
| `45dc13e9b5` | 07-02 Task 2 | refactor(07-02): consolidate GameStartConfig into shared types/game.ts |
| `20a59227d8` | 07-03 Task 1 | feat(07-03): bundle 9 curated AI decks for Standard, Casual, and Commander |
| `b954d3f744` | 07-03 Task 2 | feat(07-03): install bundled AI decks on startup and remove 60-Forests fallback |

---

### TypeScript Compilation

`npx tsc --noEmit` in `forge-gui-web/frontend` produces no output (zero errors). All type wiring for `GameStartConfig` re-exports and `setCode`/`collectorNumber` prop passing is type-safe.

---

_Verified: 2026-03-20T22:00:00Z_
_Verifier: Claude (gsd-verifier)_
