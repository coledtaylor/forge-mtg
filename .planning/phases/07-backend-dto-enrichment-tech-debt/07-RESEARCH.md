# Phase 7: Backend DTO Enrichment & Tech Debt - Research

**Researched:** 2026-03-20
**Domain:** Java backend DTO enrichment, Forge engine card model, Scryfall image URLs, format validation
**Confidence:** HIGH

## Summary

This phase enriches `CardDto` with `setCode` and `collectorNumber` fields for direct Scryfall image URLs, fixes format validation 400 errors for "Casual 60-card" and "Jumpstart", consolidates the duplicate `GameStartConfig` TypeScript interface, and replaces the 60-Forests AI fallback with bundled curated decks.

The critical discovery is that Forge already has a `CardArtPreference` system in `CardDb` that handles edition ranking by recency and filtering to core/expansion sets. The `LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY` preference does exactly what the user wants: pick the most recent core set or expansion printing, falling back to any printing if none qualify. This means no custom edition-ranking algorithm is needed.

The main technical challenge is that `CardView` (the game-state view object) exposes `getSetCode()` but NOT `getCollectorNumber()`. The collector number must be resolved by looking up the PaperCard via `StaticData.instance().getCommonCards()` using the card name and set code from CardView.

**Primary recommendation:** Use Forge's built-in `CardDb.getCardFromEditions(name, LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY)` for preferred-printing resolution, and look up collector number from the resolved PaperCard.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Prefer the most recent core set or standard-legal printing for every card
- If no core/standard printing exists, fall back to the most recent printing of any kind
- Add `setCode` and `collectorNumber` fields to `CardDto.java` (game state DTO), following the pattern already established in `CardSearchDto.java` and `DeckDetailDto.DeckCardEntry`
- Apply the preferred-printing lookup everywhere: game board, deck builder, hover previews, search results -- one consistent image strategy
- Add `&lang=en` to all Scryfall image URLs to explicitly force English art (prevents edge cases like Japanese alternate art WAR planeswalkers)
- Update `scryfall.ts` helper and `GameCardImage.tsx` to use set/collector-number URLs instead of name-based lookups
- Ship 3-5 curated sample AI decks per format as bundled .dck files in the web module's resources
- Source decklists from public competitive deck databases (MTGGoldfish, Moxfield, etc.) to seed realistic opponents
- Formats to cover: Commander (3-5 different commanders), Standard (3-5 archetypes), Casual 60-card (3-5 varied builds), Jumpstart (packs)
- Remove the 60-Forests `getDefaultAiDeck()` fallback entirely -- if somehow no deck matches, log a warning and pick any available deck rather than generating garbage
- Create a mapping layer between frontend format names and Forge engine validation behavior
- "Casual 60-card" -> no card legality check (any card is legal), only validate deck size (minimum 60 cards, 4-of limit)
- "Jumpstart" -> no card legality check, validate pack size (20 cards)
- "Commander" and "Standard" -> continue using `FModel.getFormats().get()` as today (these work correctly)
- Return 200 with `{legal: true}` for unrecognized formats rather than 400
- Move `GameStartConfig` interface to a single canonical location (`src/types/game.ts`)
- Remove duplicate definitions from `useGameWebSocket.ts` and `GameLobby.tsx`
- Re-export from both locations if needed for backwards compatibility

### Claude's Discretion
- Exact preferred-set resolution algorithm (how to rank editions by recency)
- Where to place the edition preference logic (backend CardDto.from() vs a shared utility)
- How to structure the bundled AI deck files within the Maven resource directory
- Error handling for missing setCode/collectorNumber on generated tokens

### Deferred Ideas (OUT OF SCOPE)
- Algorithmic deck generation for AI opponents -- valuable but separate feature, potentially its own phase
- User-configurable preferred set list -- if the automatic "latest core set" isn't enough, could add settings later
- Dynamic format list from Forge's format directory instead of hardcoded FORMAT_OPTIONS array
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| CARD-01 | Card images use direct Scryfall set/collector-number URLs instead of name-based lookups | CardView has `getSetCode()`, collector number resolved via PaperCard lookup; `getScryfallImageUrl()` already exists in scryfall.ts |
| CARD-02 | Card images prefer recent core set or standard-legal printings | Forge's `CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY` handles this natively |
| CARD-03 | Card images are English-only | Scryfall API supports `&lang=en` parameter on image URLs |
| DEBT-01 | Format validation handles "Casual 60-card" and "Jumpstart" without returning 400 | Current code does `FModel.getFormats().get()` which returns null for these; needs pre-check mapping |
| DEBT-02 | GameStartConfig type consolidated into a single shared definition | Duplicate interfaces found in `useGameWebSocket.ts` (line 5) and `GameLobby.tsx` (line 16) |
| DEBT-03 | AI deck selection provides meaningful decks for all formats | Current `pickRandomDeck()` falls back to `getDefaultAiDeck()` (60 Forests); needs bundled deck resources |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Forge engine (CardDb, CardEdition, CardArtPreference) | In-tree | Edition ranking, preferred-printing resolution | Built-in; handles all edge cases for set ordering |
| Javalin | In-tree | REST endpoint for format validation | Already used for all web API routes |
| Jackson | In-tree | JSON serialization of enriched CardDto | Already configured with ObjectMapper |

### Supporting
No new dependencies required. All work uses existing Forge engine APIs and the current web stack.

## Architecture Patterns

### CardDto Enrichment Pattern

**What:** Add `setCode` and `collectorNumber` to `CardDto.from(CardView)` using the same approach as `CardSearchDto.from(PaperCard)`.

**Key challenge:** `CardView` is a thread-safe view object used across game/UI boundaries. It has `getSetCode()` (via `CardStateView`, line 1554 of CardView.java) but does NOT have `getCollectorNumber()`. The collector number lives only on `PaperCard`.

**Resolution approach:**
1. Get set code from `cv.getCurrentState().getSetCode()`
2. Get the card's edition: `StaticData.instance().getEditions().get(setCodeFromView)`
3. Get the Scryfall code: `edition.getScryfallCode()`
4. Look up PaperCard to get collector number: `StaticData.instance().getCommonCards().getCardFromSet(name, edition, false)`
5. Get collector number: `paperCard.getCollectorNumber()`

**Example (from CardSearchDto.java, lines 54-56 -- proven pattern):**
```java
final CardEdition edition = StaticData.instance().getEditions().get(pc.getEdition());
dto.setCode = edition != null ? edition.getScryfallCode() : pc.getEdition().toLowerCase();
dto.collectorNumber = pc.getCollectorNumber();
```

### Preferred-Printing Resolution via CardArtPreference

**What:** Use Forge's built-in `CardDb.getCardFromEditions()` with `LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY` to find the best printing.

**How the algorithm works (from CardDb.java lines 891-941):**
1. Collect all printings of the card across all editions
2. Filter to `ALLOWED_SET_TYPES`: `CORE`, `EXPANSION`, `REPRINT` (line 78)
3. If filter leaves zero results, fall back to all editions (line 922)
4. Sort by edition release date, newest first (`latestFirst = true`, lines 925-927)
5. Iterate and pick first candidate with an image (lines 934-938)

**Where to place this logic:** Create a utility method `PreferredPrintingUtil.resolve(String cardName)` that returns a PaperCard with the preferred printing. Use this in `CardDto.from()` to override the in-game set code with the preferred one.

**Why override in-game set code:** The in-game `CardView.getSetCode()` returns whatever edition the card was loaded from (often obscure), not necessarily the most recognizable printing. For display purposes, we want the "best" printing.

### Format Validation Mapping Pattern

**What:** Intercept format names before `FModel.getFormats().get()` and handle custom formats with manual validation.

**Current flow (FormatValidationHandler.java):**
```java
final GameFormat format = FModel.getFormats().get(formatName);  // line 51
if (format == null) {
    ctx.status(400).json(Map.of("error", "Unknown format: " + formatName));  // line 53
    return;
}
```

**Problem:** `FModel.getFormats().get("Casual 60-card")` returns null (not a real Forge format). Same for "Jumpstart".

**Fix pattern:** Before the `FModel.getFormats().get()` call, check if formatName matches a custom-handled format. For those, do manual deck-size validation only (no card legality). For unknown formats, return 200 with `{legal: true}` instead of 400.

### GameStartConfig Consolidation Pattern

**What:** Move interface to a shared types file, import everywhere.

**Current state:**
- `useGameWebSocket.ts` line 5: `export interface GameStartConfig { deckName: string; aiDeckName: string | null; format: string; aiDifficulty: string; }`
- `GameLobby.tsx` line 16: identical interface definition

**Target:** Single definition in `src/types/game.ts` (new file), imported by both.

### AI Deck Bundling Pattern

**What:** Ship curated `.dck` files as Maven resources, copy to `DECK_CONSTRUCTED_DIR` on first run.

**Key details:**
- `forge-gui-web/src/main/resources/` does not exist yet -- needs to be created
- Deck files use Forge's `.dck` format (parsed by `DeckSerializer.fromFile()`)
- The `matchesFormat()` method (WebServer.java line 406) checks `deck.getComment()` against the format string
- Bundled decks should set their comment field to match the format (e.g., "Commander", "Standard", "Casual 60-card")
- `pickRandomDeck()` scans `DECK_CONSTRUCTED_DIR` recursively, so bundled decks placed in a subdirectory (e.g., `ai-decks/`) will be found automatically

### Anti-Patterns to Avoid
- **Building a custom edition-ranking algorithm:** Forge's `CardArtPreference` already does this correctly with date-based sorting and set type filtering. Do not replicate this logic.
- **Looking up PaperCard by name only without set:** `StaticData.instance().getCommonCards().getCard(name)` returns the default printing, which may not match the preferred one. Always use `getCardFromEditions()` with explicit art preference.
- **Modifying CardView to add collector number:** CardView uses `TrackableProperty` for thread-safe serialization. Adding a new TrackableProperty is invasive and unnecessary -- look up collector number at DTO creation time instead.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Edition recency ranking | Custom date comparison / sorting | `CardDb.getCardFromEditions(name, LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY)` | Handles all edge cases: unknown editions, missing images, fallback logic |
| Set type filtering (core vs promo) | Manual set type checks | `CardArtPreference.accept(CardEdition)` with `ALLOWED_SET_TYPES` | Already defines CORE + EXPANSION + REPRINT as preferred |
| Scryfall set code mapping | Forge code to Scryfall code converter | `CardEdition.getScryfallCode()` | Already lowercase, already handles aliases |
| Deck file parsing | Custom .dck parser | `DeckSerializer.fromFile()` | Handles all Forge deck format variants |

**Key insight:** The Forge engine already solved preferred-printing resolution. The entire CARD-01/CARD-02 requirement chain is: call one existing method, extract two fields, add them to CardDto.

## Common Pitfalls

### Pitfall 1: Tokens and Generated Cards Have No PaperCard
**What goes wrong:** `CardDto.from()` tries to look up a PaperCard for a token (e.g., "Soldier Token") and gets null.
**Why it happens:** Tokens are created by the engine at runtime with no PaperCard backing. They have a set code but no collector number in any card database.
**How to avoid:** In `CardDto.from()`, if the PaperCard lookup returns null, set `setCode` and `collectorNumber` to null. The frontend `GameCardImage.tsx` already has a fallback rendering for when the image URL fails.
**Warning signs:** NullPointerException in `CardDto.from()` during games with token-producing cards.

### Pitfall 2: CardView.getSetCode() Returns Forge Internal Code, Not Scryfall Code
**What goes wrong:** Using `cv.getCurrentState().getSetCode()` directly as the Scryfall set code produces broken image URLs.
**Why it happens:** Forge uses its own edition codes (e.g., "MH3") which may differ from Scryfall codes. The mapping is done by `CardEdition.getScryfallCode()`.
**How to avoid:** Always resolve through `StaticData.instance().getEditions().get(setCode)` then call `.getScryfallCode()`.
**Warning signs:** 404s from Scryfall for certain sets.

### Pitfall 3: matchesFormat() Logic for AI Deck Selection
**What goes wrong:** Bundled AI decks for "Casual 60-card" are never found because `matchesFormat()` has special handling.
**Why it happens:** Line 413 of WebServer.java: decks without a comment match "Constructed" and "Casual 60-card". But decks WITH a "Casual 60-card" comment match via `comment.equalsIgnoreCase(format)` on line 416. The logic is correct but the comment field must be set in the .dck files.
**How to avoid:** Ensure all bundled .dck files have the `Comment=` header matching the format string exactly.
**Warning signs:** `pickRandomDeck("Casual 60-card")` returns 0 candidates despite decks being present.

### Pitfall 4: Thread Safety of StaticData Lookups in CardDto.from()
**What goes wrong:** Performance regression or rare ConcurrentModificationException.
**Why it happens:** `CardDto.from(CardView)` is called from the game thread via `GameStateDto.from()`. `StaticData.instance()` is read-only after initialization, so this is safe. But repeated lookups for every card every game state update could be slow.
**How to avoid:** The PaperCard lookup (getCardFromEditions or getCardFromSet) is lightweight (HashMap-based). No caching needed for the DTO conversion. The heavy sorting logic in `tryToGetCardFromEditions` happens once per unique card name and is still fast (typically < 10 editions per card).
**Warning signs:** Game state serialization taking > 50ms in profiling.

### Pitfall 5: Scryfall lang Parameter Behavior
**What goes wrong:** `&lang=en` appended to set/collector-number URLs doesn't work or returns different card.
**Why it happens:** The Scryfall API endpoint `GET /cards/{set}/{number}` already returns the English printing by default. The `lang` parameter is only needed for the `named` endpoint.
**How to avoid:** For set/collector-number URLs, `&lang=en` is harmless but redundant. Keep it for belt-and-suspenders safety as the user decided.
**Warning signs:** None expected -- the parameter is accepted silently even if redundant.

## Code Examples

### Adding setCode and collectorNumber to CardDto.from()
```java
// In CardDto.from(CardView cv), after existing field population:
final CardStateView state = cv.getCurrentState();
if (state != null) {
    String inGameSetCode = state.getSetCode();
    if (inGameSetCode != null) {
        // Use preferred printing for best Scryfall image
        PaperCard preferred = StaticData.instance().getCommonCards()
            .getCardFromEditions(cv.getName(),
                CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);
        if (preferred != null) {
            CardEdition edition = StaticData.instance().getEditions().get(preferred.getEdition());
            dto.setCode = edition != null ? edition.getScryfallCode() : preferred.getEdition().toLowerCase();
            dto.collectorNumber = preferred.getCollectorNumber();
        }
    }
}
// Tokens / generated cards: setCode and collectorNumber remain null
```

### Updated scryfall.ts with &lang=en
```typescript
export function getScryfallImageUrl(
  setCode: string,
  collectorNumber: string,
  version: 'small' | 'normal' | 'large' = 'normal'
): string {
  return `https://api.scryfall.com/cards/${setCode.toLowerCase()}/${encodeURIComponent(collectorNumber)}?format=image&version=${version}&lang=en`
}
```

### Updated GameCardImage.tsx (set/collector-number URLs)
```typescript
import { getScryfallImageUrl } from '../../lib/scryfall'

interface GameCardImageProps {
  name: string
  setCode?: string | null
  collectorNumber?: string | null
  width?: number
  className?: string
}

export function GameCardImage({ name, setCode, collectorNumber, width = 100, className }: GameCardImageProps) {
  // Use set/collector URL if available, fall back to name-based for tokens
  const imageUrl = setCode && collectorNumber
    ? getScryfallImageUrl(setCode, collectorNumber)
    : `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(name)}&format=image&version=normal`
  // ... rest of component
}
```

### Format Validation Mapping
```java
// In FormatValidationHandler.validate(), before FModel.getFormats().get():
if ("Casual 60-card".equalsIgnoreCase(formatName)) {
    // Manual validation: 60+ cards, 4-of limit, no legality check
    int mainCount = countCards(deck.getOrCreate(DeckSection.Main));
    if (mainCount < 60) {
        ctx.json(Map.of("legal", false, "illegalCards", List.of(),
            "conformanceProblem", "Deck must have at least 60 cards (has " + mainCount + ")"));
        return;
    }
    // Check 4-of limit
    List<Map<String, String>> violations = checkFourOfLimit(deck);
    ctx.json(Map.of("legal", violations.isEmpty(), "illegalCards", violations, "conformanceProblem", ""));
    return;
}
if ("Jumpstart".equalsIgnoreCase(formatName)) {
    // Manual validation: exactly 20 cards
    int mainCount = countCards(deck.getOrCreate(DeckSection.Main));
    boolean legal = mainCount == 20;
    ctx.json(Map.of("legal", legal, "illegalCards", List.of(),
        "conformanceProblem", legal ? "" : "Jumpstart pack must have exactly 20 cards (has " + mainCount + ")"));
    return;
}

final GameFormat format = FModel.getFormats().get(formatName);
if (format == null) {
    // Unknown format: return legal=true rather than 400
    ctx.json(Map.of("legal", true, "illegalCards", List.of(), "conformanceProblem", ""));
    return;
}
```

### GameStartConfig Consolidation
```typescript
// src/types/game.ts (new file)
export interface GameStartConfig {
  deckName: string
  aiDeckName: string | null
  format: string
  aiDifficulty: string
}

// In useGameWebSocket.ts: remove interface, add import
import type { GameStartConfig } from '../types/game'

// In GameLobby.tsx: remove interface, add import
import type { GameStartConfig } from '../../types/game'
```

### Bundled AI Deck .dck Format
```
[metadata]
Name=Mono-Red Aggro
Comment=Standard
[Main]
4 Monastery Swiftspear|FDN
4 Slickshot Show-Off|OTJ
4 Heartfire Hero|BLB
4 Screaming Nemesis|DSK
4 Lightning Strike|DMU
4 Play with Fire|MID
4 Kumano Faces Kakkazan|NEO
4 Feldon, Ronom Excavator|BRO
4 Searing Blood|BNG
4 Squee, Dubious Monarch|DMU
20 Mountain|MH3
[Sideboard]
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Name-based Scryfall lookup (`/cards/named?exact=`) | Set/collector-number lookup (`/cards/{set}/{number}`) | This phase | Faster, deterministic, no ambiguity |
| Whatever edition the engine loaded | Preferred-printing via `CardArtPreference` | This phase | Consistent recognizable art |
| 400 error for unknown formats | Graceful handling with manual validation | This phase | "Casual 60-card" and "Jumpstart" work |
| 60-Forests AI fallback | Curated bundled decks | This phase | Real games against real decks |

## Open Questions

1. **Token image strategy for set/collector-number URLs**
   - What we know: Tokens have no PaperCard and no collector number. GameCardImage will fall back to name-based lookup.
   - What's unclear: Whether Scryfall name-based lookup works reliably for tokens (e.g., "Soldier Token" vs "Soldier")
   - Recommendation: Accept name-based fallback for tokens; this matches current behavior (no regression). Can improve in a future phase if needed.

2. **Bundled deck sourcing and licensing**
   - What we know: User wants 3-5 decks per format sourced from public databases
   - What's unclear: Exact decklists to use -- MTGGoldfish/Moxfield lists change weekly
   - Recommendation: Pick reasonable representative archetypes at implementation time. The decks don't need to be tournament-optimal, just realistic (actual card synergies, proper mana bases, no 60 Forests).

3. **Preferred-printing resolution for split/DFC/adventure cards**
   - What we know: `getCardFromEditions()` handles these by card name; DFCs store the front face name
   - What's unclear: Whether `CardView.getName()` returns the full DFC name or just the front face
   - Recommendation: Test with a few DFC examples during implementation; the name matching should work since `CardView.getName()` returns the card name Forge uses internally.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | TestNG (Java), Vitest (TypeScript -- not yet configured) |
| Config file | `forge-gui-web/pom.xml` (TestNG via Maven Surefire) |
| Quick run command | `mvn test -pl forge-gui-web -Dtest=DtoSerializationTest -q` |
| Full suite command | `mvn test -pl forge-gui-web -q` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CARD-01 | CardDto includes setCode and collectorNumber fields | unit | `mvn test -pl forge-gui-web -Dtest=DtoSerializationTest -q` | Needs update |
| CARD-02 | Preferred printing resolves to core/expansion set | unit | `mvn test -pl forge-gui-web -Dtest=CardDtoPreferredPrintingTest -q` | Wave 0 |
| CARD-03 | Scryfall URLs include &lang=en | unit (frontend) | Manual verification of scryfall.ts | Manual-only (string template) |
| DEBT-01 | Format validation returns 200 for Casual 60-card and Jumpstart | unit | `mvn test -pl forge-gui-web -Dtest=FormatValidationTest -q` | Wave 0 |
| DEBT-02 | GameStartConfig in single file, no duplicates | unit (frontend) | TypeScript compilation check | Manual-only (type check) |
| DEBT-03 | AI deck selection finds bundled decks | integration | `mvn test -pl forge-gui-web -Dtest=AiDeckSelectionTest -q` | Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn test -pl forge-gui-web -Dtest=DtoSerializationTest -q`
- **Per wave merge:** `mvn test -pl forge-gui-web -q`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `forge-gui-web/src/test/java/forge/web/FormatValidationTest.java` -- covers DEBT-01
- [ ] Update `DtoSerializationTest.java` -- add setCode/collectorNumber to CardDto round-trip test (covers CARD-01)
- [ ] Note: CARD-02, DEBT-03 require Forge engine initialization (`StaticData.instance()`) which may not be available in unit tests without the full data directory. These may need to be integration tests or manual validation.

## Sources

### Primary (HIGH confidence)
- `CardDb.java` lines 64-84 -- CardArtPreference enum, ALLOWED_SET_TYPES, accept() method
- `CardDb.java` lines 847-941 -- tryToGetCardFromEditions() full algorithm
- `CardView.java` lines 1554-1558 -- getSetCode() on CardStateView
- `CardSearchDto.java` lines 54-56 -- existing setCode/collectorNumber population pattern
- `FormatValidationHandler.java` lines 51-55 -- current format lookup causing 400
- `WebServer.java` lines 375-386 -- pickRandomDeck() and getDefaultAiDeck()
- `GameCardImage.tsx` -- current name-based Scryfall URL construction
- `scryfall.ts` -- existing getScryfallImageUrl() with set/collector pattern

### Secondary (MEDIUM confidence)
- Scryfall API documentation -- `&lang=en` parameter on card image endpoints
- `.dck` file format -- inferred from DeckSerializer usage and existing handler code

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all components are in-tree, verified by reading source
- Architecture: HIGH -- patterns verified by reading CardDb, CardView, and existing DTO code
- Pitfalls: HIGH -- identified from direct code reading (token edge case, set code mapping, thread safety)
- Format validation: HIGH -- root cause (FModel.getFormats().get() returning null) confirmed in source
- AI deck bundling: MEDIUM -- .dck format inferred from existing code, directory structure needs creation

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable -- Forge engine APIs rarely change)
