# Phase 7: Backend DTO Enrichment & Tech Debt - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Enrich backend data contracts (CardDto, GameStateDto) with fields that unlock card quality, priority display, and undo support for downstream phases. Fix v1.0 tech debt: format validation 400 errors, duplicate GameStartConfig type, AI deck fallback. This is pure backend + data plumbing — no new UI components.

</domain>

<decisions>
## Implementation Decisions

### Card Image Selection Strategy
- Prefer the most recent core set or standard-legal printing for every card
- If no core/standard printing exists, fall back to the most recent printing of any kind
- Add `setCode` and `collectorNumber` fields to `CardDto.java` (game state DTO), following the pattern already established in `CardSearchDto.java` and `DeckDetailDto.DeckCardEntry`
- Apply the preferred-printing lookup everywhere: game board, deck builder, hover previews, search results — one consistent image strategy
- Add `&lang=en` to all Scryfall image URLs to explicitly force English art (prevents edge cases like Japanese alternate art WAR planeswalkers)
- Update `scryfall.ts` helper and `GameCardImage.tsx` to use set/collector-number URLs instead of name-based lookups

### AI Deck Fallback
- Ship 3-5 curated sample AI decks per format as bundled .dck files in the web module's resources
- Source decklists from public competitive deck databases (MTGGoldfish, Moxfield, etc.) to seed realistic opponents
- Formats to cover: Commander (3-5 different commanders), Standard (3-5 archetypes), Casual 60-card (3-5 varied builds), Jumpstart (packs)
- Remove the 60-Forests `getDefaultAiDeck()` fallback entirely — if somehow no deck matches, log a warning and pick any available deck rather than generating garbage

### Format Validation Fix
- Create a mapping layer between frontend format names and Forge engine validation behavior
- "Casual 60-card" → no card legality check (any card is legal), only validate deck size (minimum 60 cards, 4-of limit)
- "Jumpstart" → no card legality check, validate pack size (20 cards)
- "Commander" and "Standard" → continue using `FModel.getFormats().get()` as today (these work correctly)
- Return 200 with `{legal: true}` for unrecognized formats rather than 400

### GameStartConfig Consolidation
- Move `GameStartConfig` interface to a single canonical location (`src/types/game.ts`)
- Remove duplicate definitions from `useGameWebSocket.ts` and `GameLobby.tsx`
- Re-export from both locations if needed for backwards compatibility

### Claude's Discretion
- Exact preferred-set resolution algorithm (how to rank editions by recency)
- Where to place the edition preference logic (backend CardDto.from() vs a shared utility)
- How to structure the bundled AI deck files within the Maven resource directory
- Error handling for missing setCode/collectorNumber on generated tokens

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Card DTO Pattern
- `forge-gui-web/src/main/java/forge/web/dto/CardSearchDto.java` — Existing pattern for setCode/collectorNumber population from PaperCard
- `forge-gui-web/src/main/java/forge/web/dto/CardDto.java` — Game state DTO that needs enrichment
- `forge-gui-web/src/main/java/forge/web/dto/DeckDetailDto.java` — Another existing pattern with setCode/collectorNumber

### Scryfall URL Construction
- `forge-gui-web/frontend/src/lib/scryfall.ts` — URL helper (already has set/collector function)
- `forge-gui-web/frontend/src/components/game/GameCardImage.tsx` — Game card image (needs migration from name-based to set-based)
- `forge-gui-web/frontend/src/components/CardImage.tsx` — Deck builder card image (already correct pattern)

### Format Validation
- `forge-gui-web/src/main/java/forge/web/api/FormatValidationHandler.java` — Where format validation happens
- `forge-gui-web/src/main/java/forge/web/WebServer.java` lines 406-436 — matchesFormat(), getDefaultAiDeck()

### Research
- `.planning/research/STACK.md` — Stack additions research (confirms no new deps needed)
- `.planning/research/ARCHITECTURE.md` — Architecture integration points
- `.planning/research/PITFALLS.md` — Scryfall cache invalidation risk, rate limiting

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `CardSearchDto.from(PaperCard)` — Already implements setCode/collectorNumber extraction via `CardEdition.getScryfallCode()` and `PaperCard.getCollectorNumber()`. Pattern to follow for CardDto.
- `getScryfallImageUrl()` in `scryfall.ts` — Already correct for set/collector URLs. Just needs to be used by GameCardImage.
- `DeckDetailDto.toEntries()` — Another working pattern for card edition lookup.

### Established Patterns
- DTOs use public fields (no getters/setters) per checkstyle config
- CardDto.from(CardView) is the factory method — CardView provides access to card state
- CardSearchDto uses `CardEdition edition = StaticData.instance().getEditions().get(pc.getEdition())` for set lookup
- Javalin route registration order matters — specific routes before parameterized `{name}` routes

### Integration Points
- `CardDto.from(CardView cv)` — Where setCode/collectorNumber need to be added. CardView has `getSetCode()` already (confirmed by architecture research).
- `WebServer.handleStartGame()` — Where AI deck resolution happens (pickRandomDeck, loadDeckByName)
- `FormatValidationHandler.validate()` — Where format mapping needs to be inserted before `FModel.getFormats().get()`
- `GameCardImage.tsx` — Frontend consumer of new CardDto fields

</code_context>

<specifics>
## Specific Ideas

- Source AI sample decks from MTGGoldfish or similar public competitive deck databases
- Algorithmic deck generation noted as a valuable future feature (deferred — not in this phase)
- English-only enforcement via `&lang=en` Scryfall parameter is belt-and-suspenders with set selection

</specifics>

<deferred>
## Deferred Ideas

- Algorithmic deck generation for AI opponents — valuable but separate feature, potentially its own phase
- User-configurable preferred set list — if the automatic "latest core set" isn't enough, could add settings later
- Dynamic format list from Forge's format directory instead of hardcoded FORMAT_OPTIONS array

</deferred>

---

*Phase: 07-backend-dto-enrichment-tech-debt*
*Context gathered: 2026-03-20*
