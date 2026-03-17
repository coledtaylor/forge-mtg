# Stack Research

**Domain:** React/TypeScript game client with Java backend (REST + WebSocket)
**Researched:** 2026-03-16
**Confidence:** HIGH

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended | Confidence |
|------------|---------|---------|-----------------|------------|
| React | 19.2.x | UI framework | Stable since Dec 2024, universal ecosystem, strong typing with TS. 19.2 adds Suspense improvements useful for async game state loading. | HIGH |
| TypeScript | 5.7.x | Type safety | Use 5.7 stable (not 6.0 RC). TS 6.0 is RC as of March 2026 -- too fresh for a greenfield project. 5.7 is battle-tested and fully supported. | HIGH |
| Vite | 8.x | Build tooling | Ships Rolldown (Rust bundler) for 10-30x faster builds. Native Vite+React template. HMR in <50ms. | HIGH |
| Tailwind CSS | 4.2.x | Styling | CSS-native config (no JS config file), 5x faster builds. Utility-first is ideal for game UIs with many conditional states. | HIGH |
| Zustand | 5.x | Client state management | Minimal boilerplate, middleware for devtools/persistence, optimized re-renders. Perfect for game state (board, hand, zones) where fine-grained subscriptions matter. | HIGH |
| TanStack Query | 5.x | Server state (REST) | Handles card search, deck CRUD caching, pagination. Separates server state from game state cleanly. | HIGH |

### Backend (Java Web Layer)

| Technology | Version | Purpose | Why Recommended | Confidence |
|------------|---------|---------|-----------------|------------|
| Javalin | 7.0.x | HTTP + WebSocket server | Lightweight, embeddable, first-class WebSocket support, built on Jetty 12. Requires Java 17+ (matches Forge). REST + WS in one framework. Far simpler than Spring Boot for a local-only tool. | HIGH |
| Jackson | 2.21.x | JSON serialization | LTS release (Jan 2026). Standard for Java JSON. Use 2.x line (not 3.x) for broader ecosystem compatibility. Serialize GameView/CardView/PlayerView to JSON for WebSocket protocol. | HIGH |
| Jetty | 12.x (via Javalin) | Embedded server | Bundled with Javalin 7. Do NOT use standalone -- Javalin wraps it cleanly. | HIGH |

### Supporting Libraries (Frontend)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| react-use-websocket | 4.x | WebSocket React hook | Core game connection. Provides useWebSocket hook with auto-reconnect, heartbeat, message queue. Avoids writing raw WS boilerplate. |
| @tanstack/react-table | 9.x | Table rendering | Deck list editor, card search results. Headless -- works with Tailwind. |
| clsx | 2.x | Conditional classnames | Everywhere Tailwind classes are conditional (tapped cards, highlighted zones, active phases). |
| lucide-react | latest | Icons | UI icons for zones, phases, actions. Tree-shakeable SVG icons. |
| zod | 3.x | Runtime validation | Validate WebSocket message shapes from server. Type-safe parsing of GameView JSON. |
| immer | 10.x | Immutable state updates | Complex nested game state updates (zones containing cards containing counters). Use with Zustand's immer middleware. |

### Supporting Libraries (Backend)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Jackson Databind | 2.21.x | Object-to-JSON mapping | Serializing GameView, CardView, PlayerView to JSON |
| Jackson Module Parameter Names | 2.21.x | Constructor detection | Avoid @JsonProperty on every field -- auto-detects from bytecode |
| SLF4J | 2.0.x (existing) | Logging facade | Already in Forge. Javalin uses SLF4J natively -- no adapter needed. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Vitest | 4.x | Unit/component testing. Native Vite integration, Jest-compatible API. |
| React Testing Library | latest | Component testing. Test game UI interactions (click to cast, drag to attack). |
| Playwright | latest | E2E testing. Full game flow tests (start match, play card, win). |
| ESLint | 9.x | Linting. Flat config format. Use `@typescript-eslint/eslint-plugin`. |
| Prettier | 3.x | Formatting. Integrate with ESLint via `eslint-config-prettier`. |
| MSW (Mock Service Worker) | 2.x | API mocking. Mock REST endpoints during frontend dev without Java backend running. |

## Installation

```bash
# Initialize frontend (from forge-gui-web/frontend/)
npm create vite@latest . -- --template react-ts

# Core
npm install react react-dom zustand @tanstack/react-query zod immer

# WebSocket
npm install react-use-websocket

# UI
npm install tailwindcss @tailwindcss/vite clsx lucide-react

# Tables (deck editor)
npm install @tanstack/react-table

# Dev dependencies
npm install -D typescript @types/react @types/react-dom
npm install -D vitest @testing-library/react @testing-library/jest-dom jsdom
npm install -D eslint @typescript-eslint/eslint-plugin @typescript-eslint/parser
npm install -D prettier eslint-config-prettier
npm install -D msw playwright
```

```xml
<!-- Maven (forge-gui-web/pom.xml) -->
<dependencies>
    <!-- Forge modules -->
    <dependency>
        <groupId>${project.groupId}</groupId>
        <artifactId>forge-gui</artifactId>
        <version>${project.version}</version>
        <exclusions>
            <!-- Exclude Jetty 9.4 from forge-gui; Javalin brings Jetty 12 -->
            <exclusion>
                <groupId>org.eclipse.jetty</groupId>
                <artifactId>*</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- Web framework (brings Jetty 12) -->
    <dependency>
        <groupId>io.javalin</groupId>
        <artifactId>javalin</artifactId>
        <version>7.0.1</version>
    </dependency>

    <!-- JSON serialization -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.21.0</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.module</groupId>
        <artifactId>jackson-module-parameter-names</artifactId>
        <version>2.21.0</version>
    </dependency>
</dependencies>
```

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Javalin 7 | Spring Boot 3.x | Only if you need dependency injection, database ORM, or enterprise middleware. Massive overkill for a local single-user tool. Forge already has a `forge-web-starter` with Spring Boot -- we're deliberately avoiding it for simplicity. |
| Javalin 7 | Raw Jetty 12 | Only if you need absolute control over servlet containers. Javalin wraps Jetty cleanly and adds routing, WS handlers, and static file serving with minimal overhead. |
| Zustand | Redux Toolkit | Only if you have a team already using Redux. RTK adds boilerplate (slices, reducers, actions) that Zustand avoids. For game state with many independent zones, Zustand's selector-based subscriptions are more natural. |
| Zustand | Jotai | If you prefer atomic state (each card as an atom). Viable but Zustand's store model maps better to "game state is one object with zones." |
| TanStack Query | SWR | If the project were simpler (just fetching). TanStack Query has better mutation support, query invalidation, and devtools -- all needed for deck CRUD. |
| Tailwind CSS 4 | CSS Modules | If the team strongly prefers scoped CSS. Tailwind's utility-first approach is faster for prototyping complex game UIs with many visual states. |
| react-use-websocket | Native WebSocket API | If you need custom reconnection logic or binary WebSocket frames. The hook handles reconnect, heartbeat, and message queuing out of the box. |
| Jackson 2.21 | Gson | Never for this project. Jackson is faster, has better streaming support, and handles polymorphic types (needed for GameEvent subclasses). |
| Vitest | Jest | Only if already invested in Jest config. Vitest is faster, natively supports ESM and Vite, and has a compatible API. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Spring Boot | Enormous dependency tree (~50MB+), slow startup, enterprise complexity for a localhost tool. Forge's existing `forge-web-starter` uses it but we're building lighter. | Javalin 7 |
| Socket.IO | Adds its own protocol layer on top of WebSocket. Requires Socket.IO server (Node.js) or a Java adapter. Forge is Java -- use raw WebSocket via Javalin. | Javalin WebSocket + react-use-websocket |
| Redux (classic) | Massive boilerplate, action types, switch statements. Overkill for a game client where Zustand does the same in 1/5 the code. | Zustand 5 |
| Create React App | Deprecated. No longer maintained. Slow builds. | Vite 8 |
| Webpack | Vite 8 with Rolldown is 10-30x faster. No reason to use Webpack for a new React project in 2026. | Vite 8 |
| styled-components / Emotion | Runtime CSS-in-JS adds overhead. Tailwind generates styles at build time with zero runtime cost. | Tailwind CSS 4 |
| XStream (for web JSON) | Forge uses XStream for XML serialization internally. DO NOT use it for the web API -- it serializes to XML, not JSON, and has known security issues with deserialization. | Jackson 2.21 |
| TypeScript 6.0 RC | Release candidate as of March 2026. Not stable yet. Use 5.7.x for production. Upgrade to 6.x after stable release. | TypeScript 5.7 |
| Axios | fetch() is built into all modern browsers and Node 18+. Axios adds unnecessary bundle size for REST calls that TanStack Query wraps anyway. | Native fetch + TanStack Query |

## Stack Patterns

**For game state (real-time, WebSocket-driven):**
- Use Zustand store with `immer` middleware
- WebSocket messages update the store directly
- React components subscribe to specific slices (hand, battlefield, stack)
- Because: Game state is one coherent object that changes frequently; Zustand's selector pattern prevents unnecessary re-renders when only one zone changes

**For server state (REST, request-response):**
- Use TanStack Query for card search, deck list, format info
- Because: These are cacheable, paginated, and follow standard request-response patterns. TanStack Query handles caching, deduplication, and stale-while-revalidate automatically.

**For WebSocket message typing:**
- Define message types with Zod schemas, derive TypeScript types with `z.infer`
- Because: WebSocket messages are untyped at runtime. Zod validates incoming JSON and provides type narrowing. Catches server-client protocol mismatches at the boundary.

**For the Java backend JSON protocol:**
- Use Jackson with `@JsonTypeInfo` for polymorphic GameEvent serialization
- Register custom serializers for TrackableObject views (GameView, CardView)
- Because: The engine's view objects have complex inheritance hierarchies. Jackson's polymorphic type handling lets the frontend discriminate event types cleanly.

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| React 19.2.x | react-use-websocket 4.x | Verified compatible; hooks API stable |
| React 19.2.x | TanStack Query 5.x | Officially supported |
| React 19.2.x | Zustand 5.x | Zustand 5 supports React 18+ and 19 |
| Vite 8.x | Vitest 4.x | Same ecosystem, shared config |
| Vite 8.x | Tailwind CSS 4.2.x | Use `@tailwindcss/vite` plugin |
| Javalin 7.0.x | Java 17+ | Java 17 is the minimum requirement |
| Javalin 7.0.x | Jetty 12.x | Bundled; do not add Jetty separately |
| Javalin 7.0.x | Jackson 2.21.x | Javalin has optional Jackson integration via `javalin-jackson` |
| Jackson 2.21.x | Java 17+ | Full support |
| forge-gui (Jetty 9.4) | Javalin 7 (Jetty 12) | MUST exclude Jetty 9.4 transitive deps from forge-gui in forge-gui-web's pom.xml. Verified: forge-gui source code does not import Jetty classes -- only declares them as deps for downstream modules. Safe to exclude. |

## Critical Integration Note: Jetty Version Conflict

The existing `forge-gui` module declares Jetty 9.4.57 as a dependency, but its source code does not import any Jetty classes (only a tinylog config references Jetty for log level suppression). This means `forge-gui-web` can safely:

1. Depend on `forge-gui` with Jetty exclusions
2. Bring in Javalin 7 which bundles Jetty 12
3. No runtime classpath conflicts

This was verified by searching forge-gui's source for Jetty imports -- zero results found. The Jetty 9.4 deps appear to exist for other downstream modules (desktop networking features) and are not used by the GUI abstraction layer that `forge-gui-web` needs.

## Sources

- [React 19.2 announcement](https://react.dev/blog/2025/10/01/react-19-2) -- React version confirmed (HIGH confidence)
- [Vite releases](https://vite.dev/releases) -- Vite 8 with Rolldown confirmed (HIGH confidence)
- [TypeScript 6.0 RC announcement](https://devblogs.microsoft.com/typescript/announcing-typescript-6-0-rc/) -- TS 6.0 is RC, not stable (HIGH confidence)
- [Tailwind CSS v4.0 blog](https://tailwindcss.com/blog/tailwindcss-v4) -- Tailwind 4 architecture changes confirmed (HIGH confidence)
- [Zustand GitHub](https://github.com/pmndrs/zustand) -- Version 5.x confirmed (HIGH confidence)
- [TanStack Query releases](https://github.com/tanstack/query/releases) -- v5.90.x confirmed (HIGH confidence)
- [Javalin 7.0 release](https://javalin.io/news/javalin-7.0.0-stable.html) -- Java 17+, Jetty 12, WebSocket support confirmed (HIGH confidence)
- [Javalin v6 to v7 migration](https://javalin.io/migration-guide-javalin-6-to-7) -- Breaking changes documented (HIGH confidence)
- [Jackson Release 2.21](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.21) -- LTS release Jan 2026 confirmed (HIGH confidence)
- [react-use-websocket npm](https://www.npmjs.com/package/react-use-websocket) -- Active maintenance, 77K weekly downloads (MEDIUM confidence)
- [Vitest GitHub](https://github.com/vitest-dev/vitest) -- v4.x confirmed (HIGH confidence)

---
*Stack research for: React/TypeScript game client with Java backend*
*Researched: 2026-03-16*
