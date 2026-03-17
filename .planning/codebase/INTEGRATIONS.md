# External Integrations

**Analysis Date:** 2026-03-16

## APIs & External Services

**Card Images & Data:**
- Scryfall API (`https://api.scryfall.com`)
  - Purpose: High-quality card images and metadata
  - SDK/Client: Native HttpURLConnection via `forge.util.HttpUtil`
  - Endpoints used:
    - `/cards/named?fuzzy={cardname}&set={set}&face={face}&format=image` - Card image download
    - Fuzzy search with optional set and face parameters
  - Auth: None required (public API)
  - Reference: `forge-gui/src/main/java/forge/gui/download/GuiDownloadPicturesHQ.java`

**Forge CDN & Downloads:**
- CardForge Downloads (`https://downloads.cardforge.org`)
  - Purpose: Cached card images, token images
  - Base URL: `URL_CARDFORGE` from `ForgeConstants.java`
  - Endpoints:
    - `/images/cards/` - Card image directory
    - `/images/tokens/` - Token image directory
  - Auth: None required
  - Reference: `forge-gui/src/main/java/forge/localinstance/properties/ForgeConstants.java` (lines 330-337)

**GitHub Assets:**
- GitHub Raw Content (`https://raw.githubusercontent.com/Card-Forge/forge-extras/refs/heads/main/`)
  - Purpose: Price data and card extras
  - File: `all-prices.txt` - Card price information
  - Auth: None required (public repository)
  - Reference: `ForgeConstants.java` line 335

**GitHub Release Feeds:**
- GitHub Releases (`https://github.com/Card-Forge/forge/releases`)
  - ATOM feeds for version/update checking:
    - Releases: `releases.atom`
    - Commits: `commits/master.atom`
  - Parser: `ApptasticSoftware rssreader` 3.8.2
  - Reference: `ForgeConstants.java` lines 30-33

**Forge Releases:**
- Releases Server (`https://releases.cardforge.org/`)
  - Purpose: Official release downloads and updates
  - Reference: `ForgeConstants.java` line 34

## Data Storage

**Databases:**
- Not detected - Forge uses file-based storage for game data
- XStream XML serialization for game state persistence
  - Used for: Deck files, quest saves, match state
  - Format: XML to Java object serialization

**File Storage:**
- Local filesystem only
  - Card data: `CARD_DATA_DIR` (cardsfolder/)
  - Deck files: `DECK_BASE_DIR` (constructed/, draft/, sealed/, etc.)
  - Cache: `CACHE_DIR` (user home/cache/pics/)
  - User preferences: `USER_PREFS_DIR` (user home/preferences/)

**Caching:**
- Local file-based caching
  - Card images: `CACHE_CARD_PICS_DIR`
  - Tokens: `CACHE_TOKEN_PICS_DIR`
  - Symbols: `CACHE_SYMBOLS_DIR`
  - Other images: Various cache subdirectories in `PICS_DIR`

## Authentication & Identity

**Auth Provider:**
- Custom/None - Single-player offline only
- No user authentication system implemented
- Adventure mode supports player profiles locally
- No external identity provider integration

## Monitoring & Observability

**Error Tracking:**
- Sentry 8.21.1
  - Purpose: Error reporting and exception monitoring
  - Used in modules: `forge-game`, `forge-ai`
  - Breadcrumb tracking for game state and ability execution
  - Configuration: Via `sentry.properties` files
  - Reference:
    - `forge-gui/src/main/java/forge/gui/download/sentry.properties`
    - `forge-gui-desktop/src/main/config/sentry.properties`
    - `forge-gui-mobile-dev/src/main/config/sentry.properties`

**Logs:**
- Tinylog 2.7.0 + SLF4J adapter
  - Log file: `USER_DIR/forge.log`
  - Configuration: `application.properties`, `application.yml`
  - Levels: Configured per module, standard SLF4J interface

## CI/CD & Deployment

**Hosting:**
- GitHub (source code)
  - Repository: `https://github.com/Card-Forge/forge/`
  - Releases: GitHub Releases with atom feed

**CI Pipeline:**
- Not detected in codebase analysis
- GitHub Actions (assumed, common for GitHub projects)

**Build Automation:**
- Maven-based build pipeline
- Checkstyle validation (enforced at validate phase)
- Maven enforcer plugin for Java 17 and Maven 3.8.1+ requirement

## Environment Configuration

**Required env vars:**
- `SENTRY_DSN` (if Sentry error reporting enabled)
- Java system properties configurable via forge.profile.properties

**Secrets location:**
- `.env` file in user profile directory (not committed)
- `forge.profile.properties.example` - Template with defaults
- `sentry.properties` - Sentry configuration

**Key configuration files:**
- `forge.profile.properties` - User preferences and paths
- `application.yml` / `application.properties` - Runtime configuration (in forge-web-starter)
- `application-*.yml` - Environment-specific config (web module)

## Webhooks & Callbacks

**Incoming:**
- None detected

**Outgoing:**
- None detected (error reporting to Sentry is uni-directional)

## Platform-Specific Integrations

**Android:**
- Android SDK (google maven repository)
- Min API: 26

**iOS:**
- RoboVM (robovm.properties for iOS compilation)

**Desktop:**
- Launch4j (Windows .exe wrapper generation)
- System clipboard integration via Java APIs

## Web Stack Integrations (In Development)

The forge-web-starter module indicates planned web client with:

**Backend:**
- Spring Boot framework (inferred from structure)
- WebSocket support for real-time game communication
- REST API endpoints for:
  - Card management (`/api/cards/`)
  - Game state (`/api/game/`)
  - Card import (`/api/import/`)

**Frontend:**
- React/TypeScript (planned, per project epic)
- Scryfall API integration for card images
- WebSocket client for game updates

**Database:**
- Not yet fully implemented
- Structure suggests Spring Data JPA repository pattern
- Tables: Card, CardFace (defined in web-starter)

---

*Integration audit: 2026-03-16*
