# Forge Web Client

## What This Is

A locally-run web client for the Forge MTG engine that lets you build decks and play Magic: The Gathering against Forge's AI in a modern browser interface. It wraps the existing Java game engine with a REST + WebSocket API and provides a React/TypeScript frontend for deck building and gameplay.

## Core Value

Build a deck in the browser and play a full game of Magic against the AI — the complete loop from deckbuilding to gameplay must work end-to-end.

## Requirements

### Validated

<!-- Inferred from existing Forge codebase -->

- ✓ Complete MTG rules engine with 20,000+ cards — existing (`forge-game`)
- ✓ AI opponents with multiple difficulty profiles — existing (`forge-ai`)
- ✓ Card database with editions, legality, and format support — existing (`forge-core`)
- ✓ Deck storage and format validation — existing (`forge-core`)
- ✓ GUI abstraction layer via `IGuiGame`/`IGuiBase` interfaces — existing (`forge-gui`)
- ✓ Serializable game state views (`GameView`, `CardView`, `PlayerView`) — existing

### Active

- [ ] REST API for card search, deck CRUD, and format management
- [ ] WebSocket API for real-time game state during matches
- [ ] Web-based deck builder with visual card browser and list editor
- [ ] Card image display via Scryfall API
- [ ] Play games vs AI through the browser
- [ ] Support for Commander, Standard, casual 60-card, and Jumpstart formats
- [ ] Game board rendering with zones (hand, battlefield, graveyard, exile, stack)
- [ ] Player interaction for casting spells, declaring attackers/blockers, and responding to triggers

### Out of Scope

- Multiplayer / networked play — local-only, single user
- User accounts or authentication — no need for local tool
- Mobile-optimized UI — desktop browser is the target
- Adventure mode — gameplay focus only
- Deck import from external sources (MTGO, Arena) — v2 consideration

## Context

Forge is a mature open-source MTG engine with a well-defined GUI abstraction layer (`IGuiGame`, `IGuiBase`). The architecture already supports multiple frontends (Swing desktop, libGDX mobile) through these interfaces. The web client will be a new implementation of these same interfaces, with the Java backend serializing game events to JSON over WebSocket and the React frontend rendering them.

The existing `TrackableObject` / `GameView` / `CardView` / `PlayerView` classes are designed for serialization, making them natural candidates for the WebSocket protocol.

A `forge-web-starter` Spring Boot module exists in early development but is not the target — we're building a new `forge-gui-web` module using a lighter embedded server approach.

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
| React + TypeScript for frontend | Large ecosystem, strong typing, good for complex UI state | — Pending |
| WebSocket + REST hybrid API | REST for stateless ops (cards, decks), WebSocket for stateful game sessions | — Pending |
| Scryfall for card images | High-quality images, well-documented API, no local storage burden | — Pending |
| New `forge-gui-web` module (not `forge-web-starter`) | Lighter weight, follows existing GUI module pattern | — Pending |
| Embedded server (Jetty or similar) | Already a Forge dependency, simpler than Spring Boot for local use | — Pending |

---
*Last updated: 2026-03-16 after initialization*
