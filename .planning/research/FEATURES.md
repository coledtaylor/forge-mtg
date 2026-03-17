# Feature Research

**Domain:** MTG deck builder + AI gameplay web client
**Researched:** 2026-03-16
**Confidence:** HIGH

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

#### Deck Builder

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Card search with filters | Every deck builder has this (Moxfield, Archidekt, Scryfall). Users expect instant search by name, type, color, CMC, format legality | MEDIUM | Forge backend already has card database and filter infrastructure. Frontend needs debounced search with multiple filter dimensions |
| Card image display | Users expect to see the card art, not just text. Moxfield/Archidekt show full card images inline | LOW | Scryfall API for images -- already a project constraint. Cache URLs client-side |
| Deck list view (text list) | Standard way to view/edit decks. Every tool has a text-based list showing quantities and card names | LOW | Simple table component with quantity controls |
| Visual card grid/gallery view | Moxfield and Archidekt both offer grid layouts. Users expect to toggle between list and visual views | MEDIUM | CSS grid of card images, needs lazy loading for performance |
| Mana curve chart | Universal in deck builders (Moxfield, Archidekt, Deckstats, ManaBox). Bar chart of cards by CMC | LOW | Simple bar chart component. Data derived from deck contents |
| Card type distribution | Creatures/Instants/Sorceries/etc breakdown. Standard in Moxfield, Archidekt, Deckstats | LOW | Pie or bar chart. Straightforward to compute from card data |
| Color distribution | Mana symbol breakdown across the deck. Expected in all serious builders | LOW | Count pips from mana costs. Visual display of color ratios |
| Deck save/load (CRUD) | Fundamental. Users need to create, name, save, and reopen decks | MEDIUM | REST API to Forge's existing deck storage. Need file-based persistence matching Forge's deck format |
| Format validation | Users expect to know if their deck is legal in a format. Forge already validates formats via DeckFormat | MEDIUM | Forge backend handles validation. Frontend needs to display errors/warnings clearly |
| Sideboard support | Standard 15-card sideboard is expected for 60-card formats. Forge already has DeckSection.Sideboard | LOW | Second list/panel for sideboard cards. Drag or button to move between main/side |
| Commander zone support | Commander is the most popular format. Forge already supports commander sections via DeckFormat.hasCommander() | MEDIUM | Dedicated commander slot with validation (color identity, legendary creature) |
| Add/remove card quantity controls | Click or type to adjust card counts. Universal UX pattern | LOW | +/- buttons or editable quantity field per card |
| Basic land adding | Every builder lets you add basics. Forge has land set selection built in | LOW | Quick-add panel for Plains/Island/Swamp/Mountain/Forest with quantity |

#### Gameplay

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Battlefield rendering with zones | Core gameplay display. Hand, battlefield, graveyard, exile, library, stack must all be visible. MTG Arena and Forge desktop both show these | HIGH | Most complex UI component. Needs spatial layout for two players' zones. Forge's IGuiGame already defines zone operations (updateZones, openZones, hideZones) |
| Card rendering on battlefield | Cards must display as recognizable images with tapped/untapped state, counters, attachments | HIGH | Card images from Scryfall. Need tap rotation, counter overlays, aura/equipment attachment visualization |
| Hand display with card interaction | Player's hand shown at bottom, clickable to cast. Arena shows cards extending from bottom edge | MEDIUM | Horizontal card fan. Click to select, show valid targets. IGuiGame.getAbilityToPlay() drives the interaction |
| Phase/turn indicator | Users need to know what phase they're in. Arena illuminates phase icons along a bar | LOW | Phase bar component. IGuiGame.updatePhase() already fires events |
| Life total display | Must show both players' life totals prominently | LOW | Simple numeric display. IGuiGame.updateLives() provides the data |
| Mana pool display | Show available mana. IGuiGame.showManaPool/hideManaPool/updateManaPool already exist | LOW | Colored mana symbols with counts |
| Stack visualization | Must show spells/abilities on the stack in order. Users need to read and respond. IGuiGame.updateStack() exists | MEDIUM | Vertical list of stack items with card images/text. Needs to support responses |
| Priority/prompt system | Tell the player what action is needed. "Choose attacker", "Choose target", etc. IGuiGame.showPromptMessage() exists | MEDIUM | Modal or inline prompt with OK/Cancel buttons. IGuiGame.updateButtons() provides button state |
| Combat phase UI | Declare attackers, declare blockers, assign damage. IGuiGame.showCombat() and assignCombatDamage() exist | HIGH | Visual attack/block assignment. Arrow lines connecting attackers to blockers/defenders |
| Card detail view (hover/click) | Zoom in on any card to read it. Essential for complex cards | LOW | Enlarged card image on hover or click. Scryfall provides high-res images |
| Choice/selection dialogs | Many cards require choices (pick a card, choose a mode, etc). IGuiGame has extensive choice methods (getChoices, one, many, order) | HIGH | Generic dialog system that maps IGuiGame's ~15 choice method signatures to UI components |
| Game setup (format + deck selection) | Users need to pick a format, select their deck, and configure AI opponent before starting | MEDIUM | Lobby screen with format picker, deck selector, AI difficulty. Maps to Forge's HostedMatch setup |
| AI opponent | The entire point. Forge's AI engine is the backend -- this is "free" from the engine side | LOW | Already exists in forge-ai. Web layer just needs to connect to it. Zero new AI work |

### Differentiators (Competitive Advantage)

Features that set the product apart from Moxfield/Archidekt (deck builders) and Arena (gameplay). These leverage Forge's unique strengths.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Integrated deck builder + gameplay | Build a deck then immediately play it against AI without switching apps. Moxfield can't play games. Arena can't easily brew. This is the core value loop | MEDIUM | Navigation from deck builder to game lobby with pre-selected deck. UX integration, not technical complexity |
| 20,000+ card pool (all of Magic) | Arena only has ~8,000 cards. Forge supports nearly every card ever printed. Play vintage, legacy, modern, pioneer, pauper, commander -- all formats | LOW | Already exists in Forge engine. Just expose it through the API |
| Goldfish/solitaire mode | Test draws and play patterns without AI opponent. Moxfield has basic "draw 7" but no gameplay. Forge can run solitaire | MEDIUM | Run a game with no opponent or a "do nothing" AI. Useful for combo testing |
| Keyboard shortcuts for gameplay | Arena has shortcuts (Z=undo, Space=pass). Power users expect this for fluid gameplay | LOW | Keyboard event handlers mapped to common actions (pass priority, confirm, undo) |
| Multiple AI difficulty levels | Forge already has multiple AI profiles. Let users pick easy/medium/hard | LOW | Already exists in forge-ai. Just expose in game setup UI |
| Card text/oracle text panel | Show oracle text alongside the card image. Useful for hard-to-read card art or foreign prints | LOW | Text panel pulling from Forge's card database, not just the image |
| Game log/history | Scrollable log of game actions. Forge desktop has this. Helpful for understanding what happened | MEDIUM | Append IGuiGame events to a log panel. Text-based action history |
| Deck statistics beyond basics | Color pip count, average CMC, card draw density, removal count, ramp count. Goes beyond Moxfield's stats | MEDIUM | Categorize cards by function (removal, ramp, draw) using Forge's card metadata or heuristics |
| Undo support | Forge supports undo for some actions. Expose in web UI | LOW | Map to existing Forge undo infrastructure if available |
| Auto-yield/auto-pass settings | Skip priority in phases where you have no plays. Arena does this. IGuiGame has autoPassUntilEndOfTurn, shouldAutoYield | MEDIUM | Settings UI + state management for auto-pass rules per phase |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems for this project specifically.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Multiplayer/networked play | "Play with friends" is natural desire | Massively increases complexity: networking, sync, auth, cheating prevention. Out of scope per PROJECT.md. Forge's web socket model is designed for local single-user | Keep it local-only. If multiplayer is ever wanted, it's a separate project phase |
| Collection tracking/inventory | Moxfield and ManaBox offer this. Users want to track what they own | Adds significant data management overhead, has nothing to do with core gameplay loop, and local-only tool doesn't need it | Defer entirely. Users who want collection tracking already use Moxfield/ManaBox |
| Price data integration | Deck builders show TCGPlayer/CardKingdom prices | Requires external API integration, pricing data is volatile, adds complexity for a free local tool where you're not buying cards | Skip entirely. This is a gameplay tool, not a purchasing tool |
| Deck import from Arena/MTGO | Archidekt and ManaBox support importing deck lists from Arena format | Adds parsing complexity for marginal value in v1. Users can rebuild decks. Forge already has its own deck format | Defer to v1.x. If added, support simple text paste (card name + quantity) not proprietary formats |
| Card scanning / camera input | ManaBox's killer feature for mobile | Desktop browser target, no camera workflow, massive complexity | Not applicable to desktop web client |
| Animated card effects | Arena has flashy animations for spells and combat | Enormous effort for visual polish that doesn't improve gameplay. Web rendering performance concerns | Simple transitions (fade, slide) for state changes. No particle effects |
| Social features (sharing, comments) | Moxfield has deck sharing and community | Local-only tool with no server infrastructure for social. Completely out of scope | No social features. Export deck list as text if users want to share elsewhere |
| Mobile-responsive UI | "Works on my phone" | PROJECT.md explicitly scopes to desktop browser. Responsive design for complex game board is extremely hard | Desktop-only. Minimum 1280px viewport assumption |
| Adventure/Quest mode | Forge desktop has Quest mode (RPG progression) | Significant additional UI: shop, inventory, progression, map. Not core to deck building + gameplay loop | Defer entirely. Focus on constructed play vs AI first |
| Draft/sealed/limited modes | Forge supports drafting against AI | Complex additional UI flow (pick screens, card pools, timer). Not part of core "build deck, play game" loop | Defer to v2. Requires its own research phase |
| Real-time card price during deckbuilding | Show dollar values next to cards | External API dependency, volatile data, irrelevant for a free gameplay tool | Not needed |

## Feature Dependencies

```
[Card Search + Filters]
    └──requires──> [Card Database API]
                       └──requires──> [REST API Server]

[Deck Builder UI]
    └──requires──> [Card Search + Filters]
    └──requires──> [Deck CRUD API]
    └──requires──> [Card Image Display]
                       └──requires──> [Scryfall Image Integration]

[Gameplay Board]
    └──requires──> [WebSocket API]
    └──requires──> [IGuiGame Web Implementation]
    └──requires──> [Card Image Display]

[Combat Phase UI]
    └──requires──> [Gameplay Board]
    └──requires──> [Choice/Selection Dialogs]

[Game Setup / Lobby]
    └──requires──> [Deck CRUD API] (to list/select decks)
    └──requires──> [WebSocket API] (to start game session)

[Integrated Build-then-Play Loop]
    └──requires──> [Deck Builder UI]
    └──requires──> [Game Setup / Lobby]
    └──requires──> [Gameplay Board]

[Stack Visualization]
    └──requires──> [Gameplay Board]
    └──requires──> [Choice/Selection Dialogs]

[Auto-yield/Auto-pass]
    └──enhances──> [Gameplay Board]

[Game Log]
    └──enhances──> [Gameplay Board]

[Deck Statistics]
    └──enhances──> [Deck Builder UI]
```

### Dependency Notes

- **Gameplay Board requires WebSocket API:** Game state is pushed from server in real-time. REST is insufficient for live gameplay.
- **Combat Phase UI requires Choice/Selection Dialogs:** Attacker/blocker declaration and damage assignment use IGuiGame's choice infrastructure.
- **Integrated Build-then-Play is the capstone:** It depends on both deck building and gameplay being functional. This is the "core value" from PROJECT.md.
- **Card Image Display is shared infrastructure:** Both deck builder and gameplay need it. Build once, early.
- **Auto-yield enhances Gameplay Board:** Not required for basic play, but significantly improves UX once gameplay works.

## MVP Definition

### Launch With (v1)

Minimum viable product -- the complete deck-building-to-gameplay loop.

- [ ] Card search with name/type/color/CMC filters -- core of deck building
- [ ] Card image display via Scryfall -- cards must be visually identifiable
- [ ] Deck CRUD (create, save, load, delete) -- users need persistent decks
- [ ] Deck list view with quantity controls -- basic editing
- [ ] Mana curve chart -- minimum viable statistics
- [ ] Format validation (at least "Casual" + Commander) -- users need to know deck is legal
- [ ] Sideboard + Commander zone support -- format-dependent deck sections
- [ ] Basic land quick-add -- every deck needs lands
- [ ] Battlefield with all zones rendered -- hand, field, graveyard, exile, library, stack
- [ ] Card rendering with tap state -- basic visual game state
- [ ] Phase/turn indicator -- know where you are in the turn
- [ ] Life totals -- know who's winning
- [ ] Priority/prompt system with OK/Cancel -- respond to game events
- [ ] Choice/selection dialogs -- cards that require decisions must work
- [ ] Combat phase (attackers/blockers) -- combat is core to Magic
- [ ] Game setup screen (pick deck, start game vs AI) -- entry point to gameplay
- [ ] Card detail on hover/click -- read card text

### Add After Validation (v1.x)

Features to add once core loop works end-to-end.

- [ ] Visual card grid view in deck builder -- once list view works, add gallery mode
- [ ] Color distribution + card type charts -- expand deck statistics
- [ ] Mana pool display -- helpful but not blocking for basic gameplay
- [ ] Stack visualization (expanded) -- show full stack with card images
- [ ] Game log/history panel -- debug and review plays
- [ ] Keyboard shortcuts -- power user efficiency
- [ ] Auto-yield/auto-pass -- smoother gameplay flow
- [ ] Multiple AI difficulty selection -- already exists in engine, just expose
- [ ] Goldfish/solitaire mode -- solo testing
- [ ] Card text/oracle text panel -- supplement images
- [ ] Deck import via text paste -- simple "4 Lightning Bolt" format

### Future Consideration (v2+)

Features to defer until the core product is solid.

- [ ] Draft/sealed modes -- requires separate research, complex UI flow
- [ ] Advanced deck statistics (removal count, ramp density) -- nice but not essential
- [ ] Undo support -- depends on engine capability, needs investigation
- [ ] Deck comparison tools -- comparing two deck lists side by side
- [ ] Adventure/Quest mode -- RPG layer is a different product

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Card search + filters | HIGH | MEDIUM | P1 |
| Card image display (Scryfall) | HIGH | LOW | P1 |
| Deck CRUD | HIGH | MEDIUM | P1 |
| Deck list view + quantity controls | HIGH | LOW | P1 |
| Mana curve chart | MEDIUM | LOW | P1 |
| Format validation | MEDIUM | LOW | P1 |
| Battlefield zone rendering | HIGH | HIGH | P1 |
| Card rendering (tap, counters) | HIGH | HIGH | P1 |
| Hand display + interaction | HIGH | MEDIUM | P1 |
| Phase/turn indicator | MEDIUM | LOW | P1 |
| Life totals | HIGH | LOW | P1 |
| Priority/prompt system | HIGH | MEDIUM | P1 |
| Choice/selection dialogs | HIGH | HIGH | P1 |
| Combat phase UI | HIGH | HIGH | P1 |
| Game setup screen | HIGH | MEDIUM | P1 |
| Visual card grid view | MEDIUM | MEDIUM | P2 |
| Color/type distribution charts | LOW | LOW | P2 |
| Stack visualization (expanded) | MEDIUM | MEDIUM | P2 |
| Game log | MEDIUM | LOW | P2 |
| Keyboard shortcuts | MEDIUM | LOW | P2 |
| Auto-yield/auto-pass | MEDIUM | MEDIUM | P2 |
| AI difficulty selection | MEDIUM | LOW | P2 |
| Goldfish mode | LOW | MEDIUM | P2 |
| Deck import (text paste) | LOW | LOW | P2 |
| Draft/sealed modes | MEDIUM | HIGH | P3 |
| Advanced deck statistics | LOW | MEDIUM | P3 |
| Undo support | LOW | MEDIUM | P3 |
| Quest/Adventure mode | LOW | HIGH | P3 |

## Competitor Feature Analysis

| Feature | Moxfield | MTG Arena | Forge Desktop | Our Web Client Approach |
|---------|----------|-----------|---------------|------------------------|
| Deck building | Best-in-class UX, instant search, packages | Basic, limited card pool | Functional but dated Swing UI | Modern React UI with Forge's full card database |
| Card pool | ~All printed cards (data only) | ~8,000 (Standard/Historic/Explorer) | 20,000+ with rules implementation | 20,000+ via Forge engine -- same as desktop |
| Gameplay | None (playtest draws only) | Full rules engine, polished | Full rules engine, functional UI | Full rules engine via Forge, modern web UI |
| AI play | None | Sparky (limited) | Multiple difficulty profiles | Multiple AI profiles via forge-ai |
| Format support | All (for building) | Standard/Historic/Explorer/Brawl | All including vintage/legacy/pauper/commander | All formats via Forge engine |
| Statistics | Mana curve, type, color, tags | Basic | Basic | Start basic, expand over time |
| Platform | Web (any browser) | Windows/Mac client | Java desktop + Android | Web (desktop browser) |
| Cost | Free | Free-to-play with purchases | Free, open source | Free, open source |
| Offline | No | No | Yes | Yes (localhost) |
| Card images | Scryfall | Proprietary | Downloaded/Scryfall | Scryfall API |

## Sources

- [Moxfield](https://moxfield.com/) -- leading web deck builder, UX benchmark
- [Archidekt](https://archidekt.com/) -- deck builder with playtest and collection features
- [Draftsim - Best MTG Deck Builder review](https://draftsim.com/best-mtg-deck-builder/) -- comparison of deck builder tools
- [GrimDeck - Best MTG Collection Tracker and Deck Builder Apps](https://grimdeck.com/blog/best-mtg-collection-tracker-deck-builder) -- 2026 comparison
- [MTG Arena Zone - Interface Guide](https://mtgazone.com/using-arena-interface-and-add-ons/) -- Arena UI/UX patterns
- [Forge GitHub](https://github.com/Card-Forge/forge) -- source of truth for engine capabilities
- [Draftsim - Forge Beginner's Guide](https://draftsim.com/forge-mtg/) -- Forge feature overview
- [Deckstats.net](https://deckstats.net/deckbuilder/en/) -- deck statistics feature reference
- Forge source code: `IGuiGame.java`, `IGuiBase.java`, `FDeckEditor.java` -- existing interface contracts

---
*Feature research for: MTG deck builder + AI gameplay web client*
*Researched: 2026-03-16*
