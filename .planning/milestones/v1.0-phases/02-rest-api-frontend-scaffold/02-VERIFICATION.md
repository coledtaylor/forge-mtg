---
phase: 02-rest-api-frontend-scaffold
verified: 2026-03-19T17:00:00Z
status: passed
score: 14/14 must-haves verified
re_verification: false
---

# Phase 02: REST API + Frontend Scaffold Verification Report

**Phase Goal:** REST API endpoints for card search and deck CRUD, plus React/TypeScript frontend scaffold with Scryfall card images, search UI, and deck list.
**Verified:** 2026-03-19T17:00:00Z
**Status:** PASSED
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths — Plan 01 (Backend REST API)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | GET /api/cards?q=lightning returns card results containing 'Lightning' (case-insensitive) | VERIFIED | `CardSearchHandler.search()` applies `PaperCardPredicates.searchableName(CONTAINS_IC, q)` at line 40–42 |
| 2 | GET /api/cards?color=R returns only cards with red in their color identity | VERIFIED | Switch on each char in `color` param builds `hasRed()` predicate at line 44–57 |
| 3 | GET /api/cards?page=2&limit=10 returns page 2 of 10 with correct total count | VERIFIED | `totalCount`, `totalPages`, `skip((page-1)*limit)`, `limit(limit)` at lines 91–105 |
| 4 | GET /api/cards returns cards with setCode and collectorNumber for Scryfall URL construction | VERIFIED | `CardSearchDto.from()` populates `setCode` (via `getScryfallCode()`) and `collectorNumber` at lines 54–56 |
| 5 | POST /api/decks creates a .dck file in the constructed deck directory | VERIFIED | `DeckSerializer.writeDeck(deck, new File(decksDir, ...))` at line 75 in `DeckHandler.create()` |
| 6 | GET /api/decks lists all saved decks with name, card count, and color identity | VERIFIED | `DeckHandler.list()` scans dir recursively, calls `DeckSummaryDto.from(deck, relativePath)` with name/cardCount/colors |
| 7 | GET /api/decks/{name} returns full deck contents grouped by section (main, sideboard) | VERIFIED | `DeckHandler.get()` calls `DeckDetailDto.from(deck)` which populates `main`, `sideboard`, `commander` lists |
| 8 | DELETE /api/decks/{name} removes the .dck file from disk | VERIFIED | `DeckHandler.delete()` calls `deckFile.delete()` and returns 204 at lines 153–154 |

### Observable Truths — Plan 02 (Frontend Scaffold)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 9 | React app builds and serves via Vite dev server | VERIFIED | `package.json` has `"dev": "vite"` script; all deps present; `node_modules` and `dist` exist |
| 10 | Vite dev server proxies /api/* requests to localhost:8080 | VERIFIED | `vite.config.ts` lines 14–17: `'/api': { target: 'http://localhost:8080', changeOrigin: true }` |
| 11 | Card images display using Scryfall API redirect URL from setCode + collectorNumber | VERIFIED | `CardImage.tsx` calls `getScryfallImageUrl(card.setCode, card.collectorNumber)` at line 30; `scryfall.ts` constructs `https://api.scryfall.com/cards/${setCode}/${collectorNumber}?format=image&version=${version}` |
| 12 | Failed card images show styled text fallback with card name, mana cost, and type line | VERIFIED | `CardImage.tsx` `onError` sets `imgError=true`; fallback renders `card.name`, `card.manaCost`, `card.typeLine` at lines 14–22 |
| 13 | Card search results display in a responsive grid layout | VERIFIED | `CardGrid.tsx` uses `gridTemplateColumns: 'repeat(auto-fill, minmax(244px, 1fr))'`; `App.tsx` renders `<CardGrid cards={data?.cards ?? []} />` |
| 14 | shadcn/ui components render with dark theme defaults | VERIFIED | `index.html` has `class="dark"` on `<html>`; `index.css` defines full `.dark` CSS variable block; skeleton, button, input, select, dialog components present in `src/components/ui/` |

**Score: 14/14 truths verified**

---

## Required Artifacts

### Plan 01 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `forge-gui-web/src/main/java/forge/web/dto/CardSearchDto.java` | Search-result DTO with setCode + collectorNumber | VERIFIED | 61 lines; `from(PaperCard)` factory fully implemented |
| `forge-gui-web/src/main/java/forge/web/api/CardSearchHandler.java` | GET /api/cards with predicate filtering + pagination | VERIFIED | 108 lines; 5 filter dimensions + full pagination logic |
| `forge-gui-web/src/main/java/forge/web/api/DeckHandler.java` | Deck CRUD using DeckSerializer | VERIFIED | 197 lines; list, create, get, update, delete all implemented |
| `forge-gui-web/src/main/java/forge/web/dto/DeckSummaryDto.java` | Deck list item DTO with name, card count, colors | VERIFIED | 56 lines; `from(Deck, relativePath)` populates all fields |
| `forge-gui-web/src/main/java/forge/web/dto/DeckDetailDto.java` | Full deck DTO with card lists by section | VERIFIED | 66 lines; main, sideboard, commander sections with Scryfall identifiers |

### Plan 02 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `forge-gui-web/frontend/package.json` | React + Vite + TanStack Query + Tailwind | VERIFIED | `@tanstack/react-query@^5.91.2`, `react@^19.2.4`, `vite@^8.0.1`, `tailwindcss@^4.2.2` |
| `forge-gui-web/frontend/vite.config.ts` | Vite config with proxy for /api and /ws | VERIFIED | Proxies `/api` to `http://localhost:8080` and `/ws` to `ws://localhost:8080` |
| `forge-gui-web/frontend/src/components/CardImage.tsx` | Scryfall image with lazy loading and styled text fallback | VERIFIED | `loading="lazy"`, `onError` handler, full text fallback with name/manaCost/typeLine |
| `forge-gui-web/frontend/src/lib/scryfall.ts` | Scryfall image URL construction helper | VERIFIED | `getScryfallImageUrl()` returns `https://api.scryfall.com/cards/${setCode}/${collectorNumber}?format=image&version=${version}` |
| `forge-gui-web/frontend/src/components/SearchBar.tsx` | Search input with filter controls | VERIFIED | Name input + color/type/format/CMC selects + search button; placeholder text "Search cards by name..." present |
| `forge-gui-web/frontend/src/hooks/useCardSearch.ts` | TanStack Query hook for card search | VERIFIED | `useQuery` with `queryKey: ['cards', params]`, `queryFn: () => searchCards(params)`, 5-min staleTime |

---

## Key Link Verification

### Plan 01 Key Links

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `CardSearchHandler.java` | `CardDb` (FModel) | `getUniqueCardsNoAlt()` | WIRED | Line 85–86: `FModel.getMagicDb().getCommonCards().getUniqueCardsNoAlt()` (uses bug-fixed variant) |
| `CardSearchHandler.java` | `PaperCard.java` | `PaperCardPredicates` | WIRED | Lines 40–41, 61–62, 74–75: predicates applied for name, type, cmc filters |
| `DeckHandler.java` | `DeckSerializer` | `writeDeck()` and `fromFile()` | WIRED | `DeckSerializer.writeDeck()` at line 75; `DeckSerializer.fromFile()` at lines 52, 89, 108, 190 |
| `WebServer.java` | `CardSearchHandler.java` | `config.routes.get("/api/cards", CardSearchHandler::search)` | WIRED | Line 99 of WebServer.java; also routes for all 5 deck endpoints at lines 100–104 |

### Plan 02 Key Links

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `CardImage.tsx` | `scryfall.ts` | `getScryfallImageUrl(card.setCode, card.collectorNumber)` | WIRED | Import at line 2; call at line 30 |
| `useCardSearch.ts` | `api/cards.ts` | `searchCards(params)` as queryFn | WIRED | Import at line 2; `queryFn: () => searchCards(params)` at line 8 |
| `api/cards.ts` | `vite.config.ts` proxy | `fetch('/api/cards?...')` | WIRED | `fetchApi('/api/cards?...')` at line 14; Vite proxies `/api` to localhost:8080 |

---

## Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| API-01 | 02-01-PLAN.md | Java backend exposes REST endpoints for card search with filters (name, type, color, CMC, format legality) | SATISFIED | `CardSearchHandler.search()` implements all 5 filter dimensions with pagination |
| API-02 | 02-01-PLAN.md | Java backend exposes REST endpoints for deck CRUD (create, save, load, delete) | SATISFIED | `DeckHandler` implements list, create, get, update, delete; all registered in `WebServer.createApp()` |
| DECK-02 | 02-02-PLAN.md | User can see card images fetched from Scryfall API | SATISFIED | `CardImage.tsx` displays Scryfall images via `getScryfallImageUrl(setCode, collectorNumber)` with skeleton loading and text fallback |

**All 3 requirement IDs accounted for. No orphaned requirements for Phase 02.**

---

## Anti-Patterns Found

No blockers or stubs found.

| File | Pattern | Severity | Notes |
|------|---------|----------|-------|
| `DeckHandler.java` lines 164, 180, 195 | `return null` | Info | These are sentinel returns in `findDeckFile()`/`scanForDeck()` helper methods — correct "not found" idiom, not stubs |
| `SearchBar.tsx` lines 85, 93, 106, 119, 147 | "placeholder" | Info | HTML `placeholder` attribute on `<Input>` and `<SelectValue>` — not code stubs |

---

## Human Verification Required

### 1. Scryfall image load against live API

**Test:** Start the backend (`mvn -pl forge-gui-web exec:java`) and frontend (`cd forge-gui-web/frontend && npm run dev`). Search for "Lightning Bolt" in the browser.
**Expected:** Card images load from Scryfall within a few seconds. Images display as actual card art, not the text fallback.
**Why human:** Scryfall API availability and correct setCode/collectorNumber values from Forge's card database cannot be verified statically.

### 2. Dark theme renders correctly

**Test:** Open the app in a browser at localhost:5173.
**Expected:** Background is dark (near-black), text is white/light gray, buttons and inputs use the blue accent color. The Forge app looks like the UI-SPEC dark theme.
**Why human:** CSS rendering and visual appearance require browser inspection.

### 3. Deck create/delete round-trip

**Test:** With the backend running, click "Create Deck" in the Deck List section. Confirm it appears in the list. Click Delete, confirm the dialog, confirm it disappears.
**Expected:** Deck appears immediately after creation, delete confirmation dialog appears, deck is removed from list after confirmation.
**Why human:** Mutation + cache invalidation + dialog UX flow requires a live browser test.

---

## Commits Verified

All 5 task commits from the summaries exist in the repository:

| Commit | Description |
|--------|-------------|
| `02a552d665` | feat(02-01): add CardSearchDto, DeckSummaryDto, DeckDetailDto |
| `a9b9d41805` | feat(02-01): add card search and deck CRUD REST endpoints with tests |
| `894be72766` | feat(02-02): initialize Vite + React + shadcn/ui frontend scaffold |
| `481560ecb4` | feat(02-02): add TypeScript types, API client, hooks, and all UI components |
| `41207aeb00` | fix(02-02): eliminate DFC/alt-art duplicates and stale search results |

---

## Summary

Phase 02 goal is fully achieved. All 14 observable truths are verified against actual codebase implementation — no stubs, no orphaned artifacts, no broken key links.

**Backend (Plan 01):** `CardSearchHandler` implements all 5 filter dimensions with pagination. `DeckHandler` implements full CRUD persisting via `DeckSerializer`. All 6 routes are registered in `WebServer.createApp()`. All 3 DTOs expose `setCode` and `collectorNumber` for Scryfall URL construction.

**Frontend (Plan 02):** Vite + React + shadcn/ui scaffold is complete with dark theme applied at the HTML root. `CardImage.tsx` loads from Scryfall with lazy loading, skeleton placeholder, and styled text fallback. TanStack Query hooks wrap all API calls. The `SearchBar` exposes all 5 filter dimensions matching the backend. `DeckList` provides create/delete with confirmation dialog. The `/api` proxy in `vite.config.ts` connects frontend to backend.

**Requirements:** API-01, API-02, and DECK-02 are all satisfied with direct implementation evidence.

Three items flagged for human verification (live Scryfall image loading, dark theme rendering, deck create/delete flow) — all automated code checks passed.

---

_Verified: 2026-03-19T17:00:00Z_
_Verifier: Claude (gsd-verifier)_
