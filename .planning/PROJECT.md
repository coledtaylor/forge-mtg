# Forge Web Client

## What This Is

A locally-run web client for the Forge MTG engine that lets you build decks and play Magic: The Gathering against Forge's AI in a modern browser interface. It wraps the existing Java game engine with a REST + WebSocket API and provides a React/TypeScript frontend with a full deck builder (search, editing, statistics, format validation, import/export) and an Arena-style game board with real-time play.

## Core Value

Build a deck in the browser and play a full game of Magic against the AI — the complete loop from deckbuilding to gameplay must work end-to-end.

## Requirements

### Validated

- ✓ Complete MTG rules engine with 20,000+ cards — existing (`forge-game`)
- ✓ AI opponents with multiple difficulty profiles — existing (`forge-ai`)
- ✓ Card database with editions, legality, and format support — existing (`forge-core`)
- ✓ Deck storage and format validation — existing (`forge-core`)
- ✓ GUI abstraction layer via `IGuiGame`/`IGuiBase` interfaces — existing (`forge-gui`)
- ✓ Serializable game state views (`GameView`, `CardView`, `PlayerView`) — existing
- ✓ REST API for card search, deck CRUD, and format validation — v1.0
- ✓ WebSocket API for real-time game state during matches — v1.0
- ✓ Web-based deck builder with visual card browser, list/grid editor, statistics — v1.0
- ✓ Card image display via Scryfall API with lazy loading and text fallback — v1.0
- ✓ Play games vs AI through the browser with all zones and prompts — v1.0
- ✓ Support for Commander, Standard, casual 60-card, and Jumpstart formats — v1.0
- ✓ Game board rendering with all zones (hand, battlefield, graveyard, exile, stack) — v1.0
- ✓ Player interaction for casting spells, declaring combat, responding to triggers — v1.0
- ✓ Deck import/export via text paste, file upload, clipboard copy (4 formats) — v1.0

### Active

- [ ] Gameplay UX overhaul — priority indicators, targeting feedback, OK/Pass clarity
- [ ] Game log — scrollable action history
- [ ] Keyboard shortcuts for common game actions
- [ ] Auto-yield/auto-pass for specific phases
- [ ] AI difficulty selection in game setup
- [ ] Goldfish/solitaire mode
- [ ] Undo support (where engine supports it)
- [ ] Card images from recent core set printings (English-only, mainstream art)
- [ ] Jumpstart format — 20-card pack builder, dual-pack game setup flow
- [ ] Deck simulation — AI vs AI headless games with configurable count and gauntlet
- [ ] Advanced deck stats (removal count, ramp density, draw density)
- [ ] Card oracle text alongside image
- [ ] Fix format validation for Casual 60-card/Jumpstart
- [ ] Consolidate duplicate GameStartConfig type
- [ ] Fix AI deck fallback for non-Commander formats

## Current Milestone: v2.0 Polish, Formats & Simulation

**Goal:** Polished gameplay UX, proper Jumpstart format support, and a new deck simulation feature for testing decks against AI gauntlets

**Target features:**
- Gameplay UX overhaul (priority, targeting, game log, keyboard shortcuts, auto-yield, undo)
- Card image quality (core set art, English-only)
- Jumpstart format (pack builder + dual-pack setup)
- Deck simulation engine (headless AI vs AI with stats)
- Advanced deck stats and oracle text
- v1.0 tech debt cleanup

### Out of Scope

- Multiplayer / networked play — local-only, single user
- User accounts or authentication — no need for local tool
- Mobile-optimized UI — desktop browser is the target
- Adventure mode — gameplay focus only

## Context

Shipped v1.0 with 11,344 LOC (4,643 Java + 6,701 TypeScript/TSX).

**Tech stack:**
- Backend: Java 17, Javalin 7, Jackson 2.21, Maven module (`forge-gui-web`)
- Frontend: React 19, TypeScript, Vite 8, Tailwind CSS 4, shadcn/ui, Zustand, TanStack Query
- Card images: Scryfall API (name-based and set/collector-number URLs)
- WebSocket: Custom protocol with 20 message types, CompletableFuture-based input bridge

**Architecture:** `forge-gui-web` module implements `IGuiGame`/`IGuiBase` interfaces with a pseudo-EDT thread ("Web-EDT"). REST handlers use Forge's `CardDb`, `DeckSerializer`, and `GameFormat` directly. Frontend uses Zustand for game state and TanStack Query for REST data.

**Known tech debt from v1.0:**
- Format validation returns 400 for "Casual 60-card" and "Jumpstart" (no matching Forge GameFormat)
- Duplicate `GameStartConfig` type definition (useGameWebSocket.ts and GameLobby.tsx)
- AI deck selection falls back to 60 Forests for non-Commander formats without matching decks
- All 6 phases have VALIDATION.md with nyquist_compliant: false

## Constraints

- **Tech stack (backend)**: Java 17, Maven module within existing Forge project — must integrate with existing module hierarchy
- **Tech stack (frontend)**: React + TypeScript — Vite for dev server and bundling
- **API protocol**: REST for CRUD operations, WebSocket for live game state
- **Card images**: Scryfall API — no local image storage needed
- **Deployment**: Local only — `localhost` access, no cloud hosting
- **Engine integration**: Must implement `IGuiGame` and `IGuiBase` interfaces, not fork the engine

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| React + TypeScript for frontend | Large ecosystem, strong typing, good for complex UI state | ✓ Good — 6,701 LOC, clean component architecture |
| WebSocket + REST hybrid API | REST for stateless ops (cards, decks), WebSocket for stateful game sessions | ✓ Good — clean separation of concerns |
| Scryfall for card images | High-quality images, well-documented API, no local storage burden | ✓ Good — lazy loading with text fallback works well |
| New `forge-gui-web` module (not `forge-web-starter`) | Lighter weight, follows existing GUI module pattern | ✓ Good — clean integration with Forge module hierarchy |
| Javalin 7 embedded server | Already a Forge-compatible server, simpler than Spring Boot | ✓ Good — 4,643 Java LOC total |
| Zustand + immer for game state | Zustand is lightweight, immer handles deep state updates cleanly | ✓ Good — Record<number,T> avoided Map serialization pitfalls |
| CompletableFuture input bridge | Maps Forge's blocking game thread to async WebSocket model | ✓ Good — handles dual input system (InputQueue + sendAndWait) |
| Scryfall name-based URL for game cards | CardDto lacks setCode/collectorNumber; name-based exact match works | ⚠️ Revisit — slower than direct URL, could add identifiers to DTO |
| Client-side commander color identity filter | Simpler than backend param, avoids round-trip for each keystroke | ✓ Good — instant filtering on search results |
| 1-second debounced auto-save for deck editor | Balance between responsiveness and server load | ✓ Good — with flushSave for explicit transitions |

---
*Last updated: 2026-03-20 after v2.0 milestone start*
