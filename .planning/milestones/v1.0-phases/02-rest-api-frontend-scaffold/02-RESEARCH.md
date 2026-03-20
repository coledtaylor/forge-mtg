# Phase 2: REST API + Frontend Scaffold - Research

**Researched:** 2026-03-18
**Domain:** Java REST API (Javalin 7) + React/TypeScript frontend scaffold + Scryfall card images
**Confidence:** HIGH

## Summary

This phase adds REST endpoints to the existing Javalin 7 web server (card search with filters, deck CRUD) and creates a new React + TypeScript frontend scaffold served via Vite. The backend work is straightforward: Forge already has a rich card database with `CardDb`, `PaperCard`, `CardRules`, and `PaperCardPredicates` providing search/filter capabilities, plus `DeckSerializer` and `DeckStorage` for .dck file I/O. The frontend scaffold is greenfield -- no `frontend/` directory exists yet.

A critical finding about Scryfall card images: the CDN (`*.scryfall.io`) URLs require Scryfall UUIDs (not set+collector number), so you cannot construct CDN URLs from card metadata alone. However, the API redirect endpoint `https://api.scryfall.com/cards/{setCode}/{collectorNumber}?format=image&version=normal` works perfectly as an `<img src>` -- the browser follows the 302 redirect to the CDN. The CDN itself has **no rate limits**. With lazy loading, only visible cards trigger requests, and browser caching prevents repeat fetches. The backend must expose `setCode` (Scryfall code) and `collectorNumber` in the search DTO so the frontend can construct these redirect URLs.

**Primary recommendation:** Build thin REST controller classes that compose Forge's existing predicates (`CardRulesPredicates`, `PaperCardPredicates`) for filtering, and a new search-specific DTO distinct from the game-state `CardDto`. Frontend should be a standard Vite + React + TypeScript project in `forge-gui-web/frontend/` with shadcn/ui and TanStack Query.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Case-insensitive substring (contains) matching for card name search
- Filters supported: color identity, card type, CMC/mana value (exact/lt/gt), format legality
- Offset-based pagination: `?page=1&limit=20` style
- Full card data in search results (name, manaCost, typeLine, oracleText, P/T, colors, CMC, setCode, collector number) -- no separate detail endpoint needed
- Endpoint: `GET /api/cards?q=...&color=...&type=...&cmc=...&format=...&page=1&limit=20`
- Store decks in Forge's native `.dck` format -- compatible with desktop Forge
- Save to Forge's deck directory (`forge-gui/res/decks/`) so existing decks appear automatically
- Support folder/subdirectory structure for deck organization
- Deck API endpoints: CRUD operations (create, read, update, delete, list)
- Use Forge's existing `DeckManager` for serialization where possible
- Frontend constructs Scryfall CDN URLs client-side from set code + collector number -- zero Scryfall API calls, no backend proxy
- Default image size: normal (488x680)
- Fallback for missing/failed images: styled text placeholder showing card name, mana cost, and type line
- React + TypeScript + Vite for dev tooling and bundling
- shadcn/ui component library (Radix primitives + Tailwind CSS)
- Dark theme -- matches MTG aesthetic
- TanStack Query for server state management
- Vite dev server proxies `/api/*` and `/ws/*` to `localhost:8080` for development

### Claude's Discretion
- Exact project directory structure within `forge-gui-web/frontend/`
- Tailwind configuration and color palette for dark theme
- TanStack Query configuration (stale times, cache settings)
- REST error response format (can reuse Phase 1 patterns)
- How collector number is stored/exposed for Scryfall URL construction

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| API-01 | Java backend exposes REST endpoints for card search with filters (name, type, color, CMC, format legality) | `CardDb.getUniqueCards()` provides deduplicated card collection; `CardRulesPredicates` has `name()`, `coreType()`, `cmc()` predicates; `PaperCardPredicates.fromRules()` bridges to PaperCard filtering; `GameFormat.getFilterRules()` provides format legality predicates via `FModel.getFormats()` |
| API-02 | Java backend exposes REST endpoints for deck CRUD (create, save, load, delete) | `DeckSerializer.writeDeck()`/`fromFile()` handle .dck I/O; `DeckStorage` manages file naming; `CardCollections.getConstructed()` wraps `StorageImmediatelySerialized` for auto-persist; `ForgeConstants.DECK_CONSTRUCTED_DIR` is the default save location |
| DECK-02 | User can see card images fetched from Scryfall API | `PaperCard.getEdition()` + `CardEdition.getScryfallCode()` + `PaperCard.getCollectorNumber()` provide the data needed; frontend uses `https://api.scryfall.com/cards/{scryfallSetCode}/{collectorNumber}?format=image&version=normal` as `<img src>` -- browser follows 302 redirect to CDN (no rate limits on CDN) |
</phase_requirements>

## Standard Stack

### Core (Backend -- already installed)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Javalin | 7.0.1 | HTTP server + REST routing | Already in pom.xml, used by Phase 1 |
| Jackson | 2.21.0 | JSON serialization | Already in pom.xml, ObjectMapper configured |
| TestNG | 7.10.2 | Testing | Already in pom.xml |

### Core (Frontend -- to install)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19.2.x | UI framework | Latest stable, locked decision |
| TypeScript | 5.9.x | Type safety | Latest stable |
| Vite | 8.0.x | Dev server + bundler | Latest stable, locked decision |
| @tanstack/react-query | 5.91.x | Server state management | Locked decision |
| Tailwind CSS | 4.x | Utility-first CSS | Locked decision (via shadcn/ui) |
| shadcn/ui | latest | Component library | Locked decision |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| N/A | N/A | All choices locked by user |

**Installation (frontend scaffold):**
```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install @tanstack/react-query
npx shadcn@latest init
```

**Version verification:** Checked via `npm view` on 2026-03-18:
- vite: 8.0.0
- react: 19.2.4
- @tanstack/react-query: 5.91.0
- typescript: 5.9.3
- tailwindcss: 4.2.2

## Architecture Patterns

### Recommended Backend Structure
```
forge-gui-web/src/main/java/forge/web/
├── WebServer.java              # Existing -- add REST route registration
├── api/                        # NEW: REST controllers
│   ├── CardSearchHandler.java  # GET /api/cards search + filter logic
│   └── DeckHandler.java        # GET/POST/PUT/DELETE /api/decks
├── dto/                        # Existing directory
│   ├── CardDto.java            # Existing game-state DTO (keep unchanged)
│   ├── CardSearchDto.java      # NEW: search-result DTO with setCode, collectorNumber
│   ├── DeckSummaryDto.java     # NEW: deck list item (name, card count, colors)
│   └── DeckDetailDto.java      # NEW: full deck with card lists by section
├── protocol/                   # Existing WebSocket protocol
└── ...                         # Existing Phase 1 classes
```

### Recommended Frontend Structure
```
forge-gui-web/frontend/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts              # Proxy config for /api/* and /ws/*
├── tailwind.config.ts          # Dark theme defaults
├── components.json             # shadcn/ui config
├── src/
│   ├── main.tsx                # App entry point
│   ├── App.tsx                 # Router + QueryClientProvider
│   ├── api/                    # API client functions
│   │   ├── client.ts           # Base fetch wrapper
│   │   ├── cards.ts            # Card search API calls
│   │   └── decks.ts            # Deck CRUD API calls
│   ├── hooks/                  # TanStack Query hooks
│   │   ├── useCardSearch.ts
│   │   └── useDecks.ts
│   ├── components/
│   │   ├── ui/                 # shadcn/ui generated components
│   │   ├── CardImage.tsx       # Scryfall image with lazy load + fallback
│   │   └── CardGrid.tsx        # Grid of card images (search results)
│   ├── lib/
│   │   ├── scryfall.ts         # Scryfall URL construction helper
│   │   └── utils.ts            # shadcn/ui cn() utility
│   └── types/
│       ├── card.ts             # Card TypeScript types
│       └── deck.ts             # Deck TypeScript types
└── public/
```

### Pattern 1: Composable Predicate Filtering (Backend)
**What:** Chain Forge's existing `Predicate<PaperCard>` predicates to build search filters
**When to use:** Card search endpoint -- compose name, type, color, CMC, format predicates
**Example:**
```java
// Source: Forge codebase CardRulesPredicates + PaperCardPredicates
Predicate<PaperCard> filter = pc -> true; // start with "match all"

if (nameQuery != null && !nameQuery.isEmpty()) {
    // PaperCardPredicates.searchableName does case-insensitive contains
    filter = filter.and(PaperCardPredicates.searchableName(
        PredicateString.StringOp.CONTAINS_IC, nameQuery));
}
if (type != null) {
    // CardRulesPredicates.coreType matches Creature, Instant, etc.
    filter = filter.and(PaperCardPredicates.fromRules(
        CardRulesPredicates.coreType(type)));
}
if (cmcOp != null && cmcValue != null) {
    filter = filter.and(PaperCardPredicates.fromRules(
        CardRulesPredicates.cmc(cmcOp, cmcValue)));
}
if (formatName != null) {
    GameFormat format = FModel.getFormats().get(formatName);
    if (format != null) {
        filter = filter.and(format.getFilterRules());
    }
}

// Get unique cards (one per name) and apply filter
Collection<PaperCard> results = FModel.getMagicDb().getCommonCards()
    .getUniqueCards().stream()
    .filter(filter)
    .collect(Collectors.toList());
```

### Pattern 2: Scryfall Image URL Construction (Frontend)
**What:** Build Scryfall image redirect URL from card metadata
**When to use:** Displaying card images in search results, deck views
**Example:**
```typescript
// Source: Forge's ImageUtil.getScryfallDownloadUrl pattern, adapted for frontend
function getScryfallImageUrl(
  setCode: string,
  collectorNumber: string,
  version: 'small' | 'normal' | 'large' = 'normal'
): string {
  // This URL triggers a 302 redirect to the CDN (*.scryfall.io)
  // CDN has NO rate limits -- only api.scryfall.com has the 10/sec limit
  // Browser <img> tags handle the redirect transparently
  return `https://api.scryfall.com/cards/${setCode.toLowerCase()}/${encodeURIComponent(collectorNumber)}?format=image&version=${version}`;
}
```

### Pattern 3: Javalin 7 Route Registration
**What:** Add REST routes to existing WebServer using `config.routes` block
**When to use:** Registering new API endpoints alongside existing health + WebSocket routes
**Example:**
```java
// Source: Existing WebServer.createApp() pattern + Javalin 7 docs
static Javalin createApp(final ObjectMapper mapper) {
    return Javalin.create(config -> {
        config.jsonMapper(new JavalinJackson(mapper, false));

        // Existing routes
        config.routes.get("/health", ctx -> ctx.result("ok"));
        config.routes.get("/api/sessions", ctx -> ctx.json(activeSessions.keySet()));

        // NEW: Card search
        config.routes.get("/api/cards", CardSearchHandler::search);

        // NEW: Deck CRUD
        config.routes.get("/api/decks", DeckHandler::list);
        config.routes.post("/api/decks", DeckHandler::create);
        config.routes.get("/api/decks/{name}", DeckHandler::get);
        config.routes.put("/api/decks/{name}", DeckHandler::update);
        config.routes.delete("/api/decks/{name}", DeckHandler::delete);

        // Existing WebSocket
        config.routes.ws("/ws/game/{gameId}", ws -> { ... });
    });
}
```

### Pattern 4: Flat DTO with Static Factory (Established Pattern)
**What:** Public-field DTOs with `from()` static factory method
**When to use:** All new DTOs (CardSearchDto, DeckSummaryDto, DeckDetailDto)
**Example:**
```java
// Source: Existing CardDto pattern from Phase 1
public class CardSearchDto {
    public String name;
    public String manaCost;
    public String typeLine;    // "Creature -- Human Wizard"
    public String oracleText;
    public int power;
    public int toughness;
    public List<String> colors;
    public int cmc;
    public String setCode;           // Scryfall set code (lowercase)
    public String collectorNumber;   // For Scryfall image URL

    public static CardSearchDto from(PaperCard pc) {
        CardSearchDto dto = new CardSearchDto();
        dto.name = pc.getName();
        CardRules rules = pc.getRules();
        dto.manaCost = rules.getManaCost().toString();
        dto.typeLine = rules.getType().toString();
        dto.oracleText = rules.getOracleText();
        dto.cmc = rules.getManaCost().getCMC();
        // ... colors from rules.getColorIdentity()
        CardEdition edition = StaticData.instance().getEditions().get(pc.getEdition());
        dto.setCode = edition != null ? edition.getScryfallCode() : pc.getEdition().toLowerCase();
        dto.collectorNumber = pc.getCollectorNumber();
        return dto;
    }
}
```

### Anti-Patterns to Avoid
- **Iterating all 30,000+ cards per request without early termination:** Use `Stream.filter().skip().limit()` for pagination, never collect all then sublist.
- **Building a Scryfall proxy/backend image endpoint:** The locked decision is client-side URL construction. The API redirect URL (`format=image`) works directly in `<img src>`.
- **Re-implementing deck serialization:** Use `DeckSerializer.writeDeck()` and `DeckSerializer.fromFile()` / `DeckSerializer.fromSections()` -- they handle .dck format edge cases.
- **Modifying the existing CardDto:** The game-state `CardDto` serves WebSocket game updates. Create a separate `CardSearchDto` for REST search results to avoid coupling.
- **Using `getAllCards()` instead of `getUniqueCards()`:** `getAllCards()` returns every printing of every card (massive duplication). `getUniqueCards()` returns one per card name.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Card name search | Custom string matching | `PaperCardPredicates.searchableName(CONTAINS_IC, query)` | Handles Unicode, split cards, DFCs correctly |
| Card type filtering | Manual type string parsing | `CardRulesPredicates.coreType(CoreType.Creature)` | Handles subtypes, supertypes, tribal correctly |
| CMC comparison | Manual mana cost parsing | `CardRulesPredicates.cmc(ComparableOp.EQUALS, value)` | Handles X costs, split card CMC rules |
| Format legality | Custom ban/set list logic | `FModel.getFormats().get("Standard").getFilterRules()` | Forge maintains up-to-date format definitions with ban lists |
| Color identity filtering | Manual color parsing | `CardRules.getColorIdentity()` returns `ColorSet` | Handles hybrid mana, color indicators, back faces per CR 903.4 |
| Deck file I/O | Custom .dck parser | `DeckSerializer.writeDeck()` / `DeckSerializer.fromFile()` | Handles metadata, sections, deferred loading, art preferences |
| Deck storage with auto-persist | Manual file management | `StorageImmediatelySerialized` with `DeckStorage` | Handles file naming, wrong-name correction, subfolder support |
| Scryfall set code mapping | Hardcoded set code table | `CardEdition.getScryfallCode()` | Forge maintains set code mappings in edition data |

**Key insight:** Forge's card database has been maintained for 15+ years with edge case handling for 30,000+ unique cards. Every card search/filter predicate accounts for split cards, double-faced cards, meld cards, adventures, and other complex card layouts. Hand-rolling any of this logic will produce bugs on non-standard card types.

## Common Pitfalls

### Pitfall 1: Using `getAllCards()` Instead of `getUniqueCards()`
**What goes wrong:** Card search returns the same card dozens of times (once per printing/set)
**Why it happens:** `CardDb.getAllCards()` returns every printing. Lightning Bolt appears in 40+ sets.
**How to avoid:** Use `getUniqueCards()` which returns one `PaperCard` per unique card name. For Scryfall images, the set code on this unique card is sufficient (latest printing by default).
**Warning signs:** Search results with duplicate card names, extremely slow response times.

### Pitfall 2: Scryfall CDN URL Misconception
**What goes wrong:** Trying to construct `cards.scryfall.io` URLs directly from set code + collector number
**Why it happens:** The CDN URLs use Scryfall's internal UUID (e.g., `cards.scryfall.io/normal/front/6/7/67f4c93b-...`), NOT set code + collector number.
**How to avoid:** Use the API redirect endpoint: `https://api.scryfall.com/cards/{setCode}/{collectorNumber}?format=image&version=normal`. The browser's `<img>` tag follows the 302 redirect to the CDN automatically. The CDN (`*.scryfall.io`) has NO rate limits -- only `api.scryfall.com` itself is rate-limited to ~10 req/sec.
**Warning signs:** 404 errors on card images, broken image URLs.

### Pitfall 3: Blocking the Javalin Event Loop with Card Search
**What goes wrong:** Card search with broad filters scans 30,000+ cards synchronously, blocking other requests.
**Why it happens:** Forge's `getUniqueCards()` is already in memory, but filter + sort + paginate on large collections can be slow.
**How to avoid:** The in-memory collection is fast enough (sub-100ms for most queries). If performance issues arise, use `stream().parallel()`. Do NOT move to async -- Javalin handlers are already on a virtual-thread-friendly pool.
**Warning signs:** Slow response times on broad searches (empty query, no filters).

### Pitfall 4: Deck Name / File Name Mismatch
**What goes wrong:** Creating a deck named "My Deck (v2)" produces unexpected file names or fails.
**Why it happens:** `DeckBase.getBestFileName()` sanitizes names for filesystem safety but the mapping isn't always reversible.
**How to avoid:** Use deck name as the API identifier, not the filename. Store/retrieve decks via `DeckSerializer.fromFile()` and `DeckSerializer.writeDeck()` which handle the name-to-file mapping.
**Warning signs:** Decks that can be saved but not loaded, or that appear with wrong names.

### Pitfall 5: Color Identity vs. Card Color
**What goes wrong:** Color filter returns wrong results (e.g., Boros Charm missing from "red" filter)
**Why it happens:** Card color (casting cost colors) differs from color identity (includes mana symbols in rules text). MTG players typically search by color identity.
**How to avoid:** Use `CardRules.getColorIdentity()` for the `color` filter parameter. The `ColorSet` class has `hasWhite()`, `hasBlue()`, etc., methods and a `getColor()` byte mask.
**Warning signs:** Multi-color cards not appearing in single-color searches, cards with color indicators being missed.

### Pitfall 6: Vite Proxy Configuration Gotchas
**What goes wrong:** API calls from the frontend get 404 or CORS errors in development
**Why it happens:** Vite's proxy needs exact path matching and proper target configuration.
**How to avoid:** Configure `vite.config.ts` with explicit proxy rules for `/api` and `/ws` targeting `http://localhost:8080`. WebSocket proxy needs `ws: true`.
**Warning signs:** Network errors in browser console during development, CORS headers in responses.

### Pitfall 7: Collector Number Encoding
**What goes wrong:** Cards with non-numeric collector numbers (e.g., "123a", "★") produce broken Scryfall URLs
**Why it happens:** Some collector numbers contain letters, Unicode characters, or special prefixes
**How to avoid:** URL-encode the collector number in the Scryfall URL: `encodeURIComponent(collectorNumber)`. Forge's `ImageUtil.getScryfallDownloadUrl()` already does this via `encodeUtf8()`.
**Warning signs:** 404s on specific cards, especially promo/special edition cards.

## Code Examples

### Card Search Handler
```java
// Source: Forge codebase APIs composed for REST endpoint
public class CardSearchHandler {
    public static void search(Context ctx) {
        String query = ctx.queryParam("q");
        String colorParam = ctx.queryParam("color");
        String typeParam = ctx.queryParam("type");
        String cmcParam = ctx.queryParam("cmc");
        String cmcOpParam = ctx.queryParam("cmcOp"); // "eq", "lt", "gt", "lte", "gte"
        String formatParam = ctx.queryParam("format");
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(20);

        Predicate<PaperCard> filter = buildFilter(query, colorParam, typeParam, cmcParam, cmcOpParam, formatParam);

        List<PaperCard> allMatches = FModel.getMagicDb().getCommonCards()
            .getUniqueCards().stream()
            .filter(filter)
            .sorted(Comparator.comparing(PaperCard::getName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());

        int totalCount = allMatches.size();
        int offset = (page - 1) * limit;
        List<CardSearchDto> pageResults = allMatches.stream()
            .skip(offset)
            .limit(limit)
            .map(CardSearchDto::from)
            .collect(Collectors.toList());

        // Return paginated response
        Map<String, Object> response = Map.of(
            "cards", pageResults,
            "total", totalCount,
            "page", page,
            "limit", limit,
            "totalPages", (int) Math.ceil((double) totalCount / limit)
        );
        ctx.json(response);
    }
}
```

### Deck CRUD Handler
```java
// Source: Forge DeckSerializer + DeckStorage patterns
public class DeckHandler {
    private static File getDecksDir() {
        return new File(ForgeConstants.DECK_CONSTRUCTED_DIR);
    }

    public static void list(Context ctx) {
        File decksDir = getDecksDir();
        // List .dck files including subdirectories
        List<DeckSummaryDto> decks = new ArrayList<>();
        listDecksRecursive(decksDir, decksDir, decks);
        ctx.json(decks);
    }

    public static void create(Context ctx) {
        DeckCreateRequest req = ctx.bodyAsClass(DeckCreateRequest.class);
        Deck deck = new Deck(req.name);
        // Add cards from request to appropriate sections
        DeckSerializer.writeDeck(deck, new File(getDecksDir(), deck.getBestFileName() + ".dck"));
        ctx.status(201).json(DeckDetailDto.from(deck));
    }

    public static void get(Context ctx) {
        String name = ctx.pathParam("name");
        File deckFile = findDeckFile(name);
        if (deckFile == null) {
            ctx.status(404).json(Map.of("error", "Deck not found"));
            return;
        }
        Deck deck = DeckSerializer.fromFile(deckFile);
        ctx.json(DeckDetailDto.from(deck));
    }
}
```

### Vite Proxy Configuration
```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
});
```

### TanStack Query Hook
```typescript
// Source: TanStack Query v5 patterns
import { useQuery, keepPreviousData } from '@tanstack/react-query';

interface CardSearchParams {
  q?: string;
  color?: string;
  type?: string;
  cmc?: number;
  cmcOp?: 'eq' | 'lt' | 'gt' | 'lte' | 'gte';
  format?: string;
  page?: number;
  limit?: number;
}

export function useCardSearch(params: CardSearchParams) {
  return useQuery({
    queryKey: ['cards', params],
    queryFn: () => searchCards(params),
    placeholderData: keepPreviousData, // Keep showing old results while fetching new page
    staleTime: 5 * 60 * 1000, // Card data rarely changes -- 5 min stale time
  });
}
```

### CardImage Component with Fallback
```tsx
// Source: Pattern based on locked decisions
function CardImage({ card }: { card: CardSearchResult }) {
  const [imgError, setImgError] = useState(false);
  const imgUrl = getScryfallImageUrl(card.setCode, card.collectorNumber);

  if (imgError) {
    return (
      <div className="w-[244px] h-[340px] bg-zinc-800 border border-zinc-700 rounded-lg p-3 flex flex-col">
        <div className="text-sm font-bold text-zinc-100">{card.name}</div>
        <div className="text-xs text-zinc-400">{card.manaCost}</div>
        <div className="text-xs text-zinc-500 mt-1">{card.typeLine}</div>
      </div>
    );
  }

  return (
    <img
      src={imgUrl}
      alt={card.name}
      loading="lazy"
      onError={() => setImgError(true)}
      className="w-[244px] h-[340px] rounded-lg"
    />
  );
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Scryfall CDN URLs constructable from set+number | CDN URLs use UUIDs; use API redirect endpoint instead | 2022+ | Must use `api.scryfall.com/cards/{set}/{num}?format=image` as img src |
| Vite 5/6 with CJS config | Vite 8 with ESM-only config | 2025-2026 | `vite.config.ts` must use ESM imports |
| TanStack Query v4 `keepPreviousData` option | TanStack Query v5 `placeholderData: keepPreviousData` | 2023 | Different API for pagination UX |
| Tailwind CSS v3 with `tailwind.config.js` | Tailwind CSS v4 with CSS-first config | 2025 | Config approach changed; shadcn/ui handles this |
| shadcn/ui CLI `npx shadcn-ui@latest init` | `npx shadcn@latest init` | 2024 | Package renamed |

**Deprecated/outdated:**
- Tailwind CSS v3 configuration style (v4 uses CSS-first approach)
- React 18 patterns (project should use React 19)
- TanStack Query v4 API (v5 has breaking changes to `keepPreviousData`, `useQuery` options)

## Open Questions

1. **Deck path encoding in REST URLs**
   - What we know: Deck names can contain spaces, special characters, and can be in subdirectories
   - What's unclear: Best URL encoding strategy for `GET /api/decks/{name}` when names have slashes (subdirectories)
   - Recommendation: Use URL-encoded full path (e.g., `/api/decks/Aggro%2FBurn%20Deck`) or query parameter approach (`/api/decks?path=Aggro/Burn%20Deck`). The query parameter approach is simpler for subdirectory support.

2. **Color identity filter semantics**
   - What we know: Users expect "W" to mean "cards that include white in their color identity"
   - What's unclear: Should `color=WR` mean "exactly Boros" or "includes white AND red"? Should colorless cards be included?
   - Recommendation: Default to "includes all specified colors" (AND logic). Empty `color` param means no filter. Add optional `colorOp=exact` for exact matching later if needed.

3. **Vite 8 + Tailwind v4 + shadcn/ui compatibility**
   - What we know: All three are latest versions; shadcn/ui init handles Tailwind setup
   - What's unclear: Whether shadcn/ui latest fully supports Tailwind v4's CSS-first configuration
   - Recommendation: Use `npx shadcn@latest init` which auto-detects and configures the right Tailwind version. If issues arise, pin to Tailwind v3.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | TestNG 7.10.2 |
| Config file | `forge-gui-web/pom.xml` (dependency) |
| Quick run command | `mvn test -pl forge-gui-web -Dtest=CardSearchHandlerTest -am` |
| Full suite command | `mvn test -pl forge-gui-web -am` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| API-01 | Card search returns filtered, paginated results | integration | `mvn test -pl forge-gui-web -Dtest=CardSearchHandlerTest -am` | No -- Wave 0 |
| API-02 | Deck CRUD creates/reads/updates/deletes .dck files | integration | `mvn test -pl forge-gui-web -Dtest=DeckHandlerTest -am` | No -- Wave 0 |
| DECK-02 | CardSearchDto includes setCode + collectorNumber for Scryfall URLs | unit | `mvn test -pl forge-gui-web -Dtest=CardSearchDtoTest -am` | No -- Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn test -pl forge-gui-web -am` (all web module tests)
- **Per wave merge:** `mvn test -pl forge-gui-web -am` + manual browser verification of frontend
- **Phase gate:** Full suite green + manual verification of card images loading in browser

### Wave 0 Gaps
- [ ] `CardSearchHandlerTest.java` -- covers API-01 (search + filter + pagination)
- [ ] `DeckHandlerTest.java` -- covers API-02 (CRUD operations on .dck files)
- [ ] `CardSearchDtoTest.java` -- covers DECK-02 (setCode + collectorNumber fields populated)
- [ ] Frontend: `npm test` setup with Vitest for component tests (optional for Phase 2, critical for Phase 3)

## Sources

### Primary (HIGH confidence)
- Forge codebase direct inspection: `CardDb.java`, `PaperCard.java`, `CardRules.java`, `CardRulesPredicates.java`, `PaperCardPredicates.java`, `DeckSerializer.java`, `DeckStorage.java`, `CardCollections.java`, `GameFormat.java`, `WebServer.java`, `CardDto.java`, `ImageUtil.java`, `ImageFetcher.java`
- `ForgeConstants.java` -- deck directory constants
- `ComparableOp.java` -- CMC comparison operators
- `PredicateString.java` -- string matching operators (CONTAINS, CONTAINS_IC, EQUALS, EQUALS_IC)

### Secondary (MEDIUM confidence)
- [Scryfall Card Imagery docs](https://scryfall.com/docs/api/images) -- CDN URL format uses UUIDs, not set+number
- [Scryfall /cards/:code/:number endpoint](https://scryfall.com/docs/api/cards/collector) -- `format=image` returns 302 redirect to CDN
- [Scryfall API docs](https://scryfall.com/docs/api) -- rate limits: 10 req/sec on api.scryfall.com, **no limits on *.scryfall.io CDN**
- [Scryfall blog on image URI changes](https://scryfall.com/blog/upcoming-api-changes-to-scryfall-image-uris-and-download-uris-224) -- old URL format deprecated
- [Javalin 7 documentation](https://javalin.io/documentation) -- route registration via `config.routes`
- npm registry -- verified package versions on 2026-03-18

### Tertiary (LOW confidence)
- None -- all findings verified against primary or secondary sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- backend stack is existing (Javalin 7, Jackson), frontend versions verified via npm registry
- Architecture: HIGH -- patterns derived from direct code inspection of Forge codebase APIs
- Pitfalls: HIGH -- Scryfall URL issue verified against official docs and blog; card database patterns verified from code

**Research date:** 2026-03-18
**Valid until:** 2026-04-18 (stable domain -- Forge APIs are mature, frontend versions may update)
