# Phase 2: REST API + Frontend Scaffold - Context

**Gathered:** 2026-03-18
**Status:** Ready for planning

<domain>
## Phase Boundary

REST API endpoints for card search (with filters) and deck CRUD operations, plus a React + TypeScript frontend scaffold with Scryfall card image display. This phase delivers the backend API layer and the foundational frontend app — the deck builder UI itself is Phase 3.

</domain>

<decisions>
## Implementation Decisions

### Card Search API
- Case-insensitive substring (contains) matching for card name search
- Filters supported: color identity, card type, CMC/mana value (exact/lt/gt), format legality
- Offset-based pagination: `?page=1&limit=20` style
- Full card data in search results (name, manaCost, typeLine, oracleText, P/T, colors, CMC, setCode, collector number) — no separate detail endpoint needed
- Endpoint: `GET /api/cards?q=...&color=...&type=...&cmc=...&format=...&page=1&limit=20`

### Deck Persistence
- Store decks in Forge's native `.dck` format — compatible with desktop Forge
- Save to Forge's deck directory (`forge-gui/res/decks/`) so existing decks appear automatically
- Support folder/subdirectory structure for deck organization — matches how some users organize desktop Forge decks
- Deck API endpoints: CRUD operations (create, read, update, delete, list)
- Use Forge's existing `DeckManager` for serialization where possible

### Scryfall Card Images
- Frontend constructs Scryfall CDN URLs client-side from set code + collector number — zero Scryfall API calls, no backend proxy
- Default image size: normal (488×680) — good quality at reasonable file size
- Fallback for missing/failed images: styled text placeholder showing card name, mana cost, and type line
- Image URL pattern: `https://cards.scryfall.io/normal/front/{hash}/{hash}.jpg` (constructed from card metadata)

### Frontend Scaffold
- React + TypeScript + Vite for dev tooling and bundling
- shadcn/ui component library (Radix primitives + Tailwind CSS)
- Dark theme — matches MTG aesthetic, similar to MTGA/Moxfield dark mode
- TanStack Query for server state management (card search results, deck lists, API data)
- Vite dev server proxies `/api/*` and `/ws/*` to `localhost:8080` for development

### Claude's Discretion
- Exact project directory structure within `forge-gui-web/frontend/`
- Tailwind configuration and color palette for dark theme
- TanStack Query configuration (stale times, cache settings)
- REST error response format (can reuse Phase 1 patterns)
- How collector number is stored/exposed for Scryfall URL construction

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 1 Implementation (existing code to build on)
- `forge-gui-web/src/main/java/forge/web/WebServer.java` — Existing Javalin server with route registration pattern
- `forge-gui-web/src/main/java/forge/web/dto/CardDto.java` — Existing card DTO (may need extending for search-specific fields)
- `forge-gui-web/src/main/java/forge/web/dto/` — All existing DTOs establishing flat DTO conventions
- `forge-gui-web/pom.xml` — Current dependencies (Javalin 7, Jackson 2.21)

### Forge Card Database
- `forge-core/src/main/java/forge/StaticData.java` — Card database access point
- `forge-core/src/main/java/forge/item/PaperCard.java` — Card model with name, edition, rules
- `forge-core/src/main/java/forge/card/CardRules.java` — Card rules with type, color, mana cost
- `forge-core/src/main/java/forge/card/CardDb.java` — Card database with search/filter capabilities

### Forge Deck System
- `forge-core/src/main/java/forge/deck/Deck.java` — Deck model class
- `forge-core/src/main/java/forge/deck/DeckSection.java` — Deck sections (main, sideboard, commander)
- `forge-core/src/main/java/forge/deck/io/DeckSerializer.java` — .dck format serialization
- `forge-gui/res/decks/` — Default deck storage directory

### Format System
- `forge-game/src/main/java/forge/game/GameFormat.java` — Format definitions and legality checking

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WebServer.java`: Route registration pattern with `config.router.apiBuilder()` — add new REST routes here
- `CardDto.java`: Existing flat DTO for game state cards — may need a search-specific DTO with additional fields (collector number for Scryfall URL)
- `ObjectMapper` configuration in WebServer — reuse for REST JSON serialization
- `WebGuiBase.getAssetsDir()` — resolves path to `forge-gui/` for deck directory access

### Established Patterns
- Flat DTOs with public fields (no getters/setters) — established in Phase 1
- Javalin 7 route registration via `config.router.apiBuilder()` — established in WebServer
- Jackson serialization with `@JsonInclude(NON_NULL)` — established in WebServer ObjectMapper config
- tinylog Logger for all logging — project standard

### Integration Points
- `WebServer.java` — New REST routes registered alongside existing WebSocket and health endpoints
- `forge-gui-web/pom.xml` — May need frontend build plugin (frontend-maven-plugin) for npm integration
- `forge-gui/res/decks/` — Deck file I/O target directory
- `FModel.getMagicDb()` — Entry point for card database queries

</code_context>

<specifics>
## Specific Ideas

- Decks stored in Forge's native `.dck` format so existing desktop Forge decks work in the web client automatically
- Scryfall image fallback should be a styled text card (not a generic placeholder) showing name, mana cost, and type — always informative
- Dark theme inspired by MTGA/Moxfield aesthetic

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 02-rest-api-frontend-scaffold*
*Context gathered: 2026-03-18*
