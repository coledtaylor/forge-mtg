# Forge MTG UI Refactor Options

## Executive Summary

Based on the codebase analysis, Forge has a **well-separated architecture** that makes UI replacement feasible. The game engine (`forge-game` and `forge-ai`) is largely decoupled from the UI through key interfaces:

- **`IGameController`** - UI commands to the game engine
- **`IGuiGame`** - Game engine events to the UI
- **`PlayerController`** - Abstract player interaction layer

Both refactor options are viable. Here are detailed outlines for each approach.

---

## Option 1: Web-Based UI (React/Vue + WebSocket/REST API)

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Web Browser                          │
│  ┌───────────────────────────────────────────────────┐  │
│  │   React/Vue Frontend                              │  │
│  │   - Game board rendering                          │  │
│  │   - Player hand/battlefield UI                    │  │
│  │   - Stack visualization                           │  │
│  │   - Card image display                            │  │
│  └─────────────────┬─────────────────────────────────┘  │
│                    │ WebSocket/REST                     │
└────────────────────┼────────────────────────────────────┘
                     │
┌────────────────────┼─────────────────────────────────────┐
│   Java Backend     │                                     │
│  ┌─────────────────▼──────────────────────────────────┐  │
│  │  Web API Layer (Spring Boot / Jetty)               │  │
│  │  - WebSocket handler                               │  │
│  │  - REST endpoints                                  │  │
│  │  - Session management                              │  │
│  │  - GameView serialization                          │  │
│  └─────────────────┬──────────────────────────────────┘  │
│                    │                                     │
│  ┌─────────────────▼──────────────────────────────────┐  │
│  │  PlayerControllerWeb implements IGameController    │  │
│  │  - Converts UI actions to game commands            │  │
│  │  - Queues actions via WebSocket                    │  │
│  └─────────────────┬──────────────────────────────────┘  │
│                    │                                     │
│  ┌─────────────────▼──────────────────────────────────┐  │
│  │  WebGuiGame implements IGuiGame                    │  │
│  │  - Converts game events to JSON                    │  │
│  │  - Broadcasts state updates                        │  │
│  └─────────────────┬──────────────────────────────────┘  │
│                    │                                     │
│  ┌─────────────────▼──────────────────────────────────┐  │
│  │  Existing Game Engine (UNCHANGED)                  │  │
│  │  - forge-game (rules engine)                       │  │
│  │  - forge-ai (AI players)                           │  │
│  │  - forge-core (card database)                      │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### Implementation Plan

#### Phase 1: API Layer (4-6 weeks)
**Goal:** Create a REST/WebSocket API that exposes the game engine

1. **New Module: `forge-web-api`**
   ```
   forge-web-api/
   ├── pom.xml (depends on forge-game, forge-ai, spring-boot/jetty)
   ├── src/main/java/forge/web/
   │   ├── WebApplication.java (main entry point)
   │   ├── config/
   │   │   ├── WebSocketConfig.java
   │   │   └── CorsConfig.java
   │   ├── controller/
   │   │   ├── GameController.java (REST endpoints)
   │   │   ├── WebSocketGameHandler.java
   │   │   └── CardSearchController.java
   │   ├── service/
   │   │   ├── GameSessionManager.java (manages active games)
   │   │   └── PlayerSessionService.java
   │   ├── model/
   │   │   ├── dto/ (JSON DTOs for API)
   │   │   └── GameSessionState.java
   │   └── player/
   │       ├── PlayerControllerWeb.java (implements IGameController)
   │       └── WebGuiGame.java (implements IGuiGame)
   ```

2. **Key Components to Implement:**

   **PlayerControllerWeb.java**
   ```java
   public class PlayerControllerWeb extends PlayerController implements IGameController {
       private final WebSocketSession session;
       private final BlockingQueue<PlayerAction> actionQueue;
       
       // Implements IGameController methods
       @Override
       public void selectCard(CardView card, ...) {
           // Queue action, wait for response via WebSocket
       }
       
       // Implements PlayerController abstract methods
       @Override
       public SpellAbility getAbilityToPlay(...) {
           // Send options to frontend, wait for selection
       }
   }
   ```

   **WebGuiGame.java**
   ```java
   public class WebGuiGame implements IGuiGame {
       private final WebSocketSession session;
       
       @Override
       public void updateStack() {
           // Serialize stack state to JSON
           // Broadcast via WebSocket
       }
       
       @Override
       public void showPromptMessage(PlayerView player, String message) {
           // Send prompt event to frontend
       }
   }
   ```

   **WebSocketGameHandler.java**
   ```java
   @Component
   public class WebSocketGameHandler {
       // Handle incoming actions from frontend
       @MessageMapping("/game/action")
       public void handleGameAction(GameAction action) {
           // Route to appropriate PlayerControllerWeb
       }
       
       // Subscribe to game state updates
       @SubscribeMapping("/game/state/{gameId}")
       public GameStateDTO subscribeToGame(String gameId) {
           // Return current state, future updates via WebSocket
       }
   }
   ```

3. **API Endpoints:**
   - `POST /api/game/new` - Start a new game
   - `GET /api/game/{id}` - Get game state
   - `POST /api/game/{id}/action` - Submit player action
   - `WS /ws/game/{id}` - WebSocket for real-time updates
   - `GET /api/cards/search` - Card database search
   - `GET /api/decks` - List/load decks

#### Phase 2: Frontend (8-12 weeks)
**Goal:** Build a modern web UI using React/Vue

1. **Technology Stack:**
   - **Framework:** React with TypeScript (or Vue 3 if preferred)
   - **State Management:** Redux Toolkit / Zustand
   - **WebSocket:** Socket.io-client or native WebSocket API
   - **Styling:** Tailwind CSS + shadcn/ui components
   - **Card Images:** Canvas API for rendering, lazy loading
   - **Build Tool:** Vite

2. **Frontend Structure:**
   ```
   forge-web-ui/
   ├── package.json
   ├── src/
   │   ├── components/
   │   │   ├── game/
   │   │   │   ├── GameBoard.tsx
   │   │   │   ├── PlayerArea.tsx
   │   │   │   ├── Battlefield.tsx
   │   │   │   ├── Hand.tsx
   │   │   │   ├── Stack.tsx
   │   │   │   ├── PhaseIndicator.tsx
   │   │   │   └── ManaPool.tsx
   │   │   ├── cards/
   │   │   │   ├── Card.tsx
   │   │   │   ├── CardHover.tsx
   │   │   │   └── CardImage.tsx
   │   │   └── ui/ (buttons, dialogs, etc.)
   │   ├── services/
   │   │   ├── gameApi.ts (REST client)
   │   │   ├── gameWebSocket.ts (WebSocket handler)
   │   │   └── cardService.ts
   │   ├── store/
   │   │   ├── gameSlice.ts
   │   │   ├── uiSlice.ts
   │   │   └── store.ts
   │   ├── types/
   │   │   ├── GameView.ts
   │   │   ├── CardView.ts
   │   │   └── PlayerView.ts
   │   └── App.tsx
   ```

3. **Key Frontend Features:**
   - Drag-and-drop card interaction
   - Animated stack resolution
   - Priority passing controls
   - Card zoom on hover
   - Responsive layout (desktop + tablet)
   - Real-time opponent actions

#### Phase 3: Integration & Testing (4-6 weeks)
**Goal:** Ensure feature parity with existing UI

1. **Testing Strategy:**
   - Unit tests for API layer
   - Integration tests for WebSocket communication
   - E2E tests using Playwright/Cypress
   - Manual playthrough of complex interactions

2. **Migration Checklist:**
   - [ ] Basic game flow (draw, play land, cast spell)
   - [ ] Combat (declare attackers/blockers, damage assignment)
   - [ ] Stack interaction (responding to spells)
   - [ ] Targeting (valid target selection)
   - [ ] Mana payment (auto-tap, manual selection)
   - [ ] Deck building
   - [ ] Game modes (draft, sealed, etc.)
   - [ ] AI opponents
   - [ ] Multiplayer support

#### Phase 4: Deployment (2-3 weeks)
**Goal:** Deploy as cloud service

1. **Deployment Options:**
   - **Docker Compose:** Single server deployment
   - **Kubernetes:** Scalable cloud deployment
   - **Cloud Platform:** AWS/GCP/Azure

2. **Infrastructure:**
   ```
   ├── Docker/
   │   ├── Dockerfile.api (Java backend)
   │   ├── Dockerfile.web (Nginx + React build)
   │   └── docker-compose.yml
   ├── nginx/
   │   └── nginx.conf (reverse proxy, static files)
   └── k8s/ (if using Kubernetes)
       ├── deployment.yaml
       ├── service.yaml
       └── ingress.yaml
   ```

3. **Considerations:**
   - **Session Management:** Redis for shared sessions
   - **File Storage:** S3/Cloud Storage for card images
   - **Database:** PostgreSQL for user accounts, decks
   - **Load Balancing:** Sticky sessions for WebSocket
   - **CDN:** CloudFlare for static assets

### Advantages
- ✅ **Accessible anywhere** - Play from any device with a browser
- ✅ **Modern UX** - Smooth animations, responsive design
- ✅ **Easy updates** - No client installation required
- ✅ **Cloud multiplayer** - Natural fit for online play
- ✅ **Mobile friendly** - Can adapt for touch devices
- ✅ **Leverages your web dev skills**

### Challenges
- ⚠️ **Real-time sync** - WebSocket state synchronization complexity
- ⚠️ **Network latency** - May feel less responsive than desktop
- ⚠️ **Card images** - Large asset download, need CDN
- ⚠️ **State serialization** - GameView → JSON conversion overhead
- ⚠️ **Session management** - Handle disconnects, reconnects
- ⚠️ **Security** - Input validation, anti-cheat measures

### Estimated Timeline
- **Minimum Viable Product:** 4-5 months
- **Feature Parity:** 8-12 months
- **Polished Release:** 12-18 months

---

## Option 2: Modern Java UI (JavaFX)

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                  JavaFX Application                     │
│  ┌───────────────────────────────────────────────────┐  │
│  │   Modern JavaFX UI (forge-gui-javafx)             │  │
│  │   - FXML layouts                                  │  │
│  │   - CSS styling                                   │  │
│  │   - SceneBuilder designs                          │  │
│  │   - Animation API                                 │  │
│  └─────────────────┬─────────────────────────────────┘  │
│                    │ Direct method calls                │
│  ┌─────────────────▼──────────────────────────────────┐ │
│  │  JavaFxGuiGame implements IGuiGame                 │ │
│  │  - Updates JavaFX UI components                    │ │
│  │  - Uses Platform.runLater() for thread safety      │ │
│  └─────────────────┬──────────────────────────────────┘ │
│                    │                                    │
│  ┌─────────────────▼──────────────────────────────────┐ │
│  │  PlayerControllerJavaFx implements IGameController │ │
│  │  - Handles user input events                       │ │
│  │  - Manages dialog interactions                     │ │
│  └─────────────────┬──────────────────────────────────┘ │
│                    │                                    │
│  ┌─────────────────▼──────────────────────────────────┐ │
│  │  Existing Game Engine (UNCHANGED)                  │ │
│  │  - forge-game (rules engine)                       │ │
│  │  - forge-ai (AI players)                           │ │
│  │  - forge-core (card database)                      │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### Implementation Plan

#### Phase 1: Module Setup (2-3 weeks)
**Goal:** Create new JavaFX module alongside existing UIs

1. **New Module: `forge-gui-javafx`**
   ```
   forge-gui-javafx/
   ├── pom.xml
   ├── src/main/java/forge/gui/javafx/
   │   ├── ForgeJavaFxApp.java (main application)
   │   ├── controllers/ (FXML controllers)
   │   ├── views/ (custom components)
   │   ├── game/
   │   │   ├── JavaFxGuiGame.java (implements IGuiGame)
   │   │   └── PlayerControllerJavaFx.java
   │   ├── services/
   │   │   ├── ImageService.java (card image loading)
   │   │   └── AnimationService.java
   │   └── util/
   ├── src/main/resources/
   │   ├── fxml/ (SceneBuilder layouts)
   │   │   ├── MainGame.fxml
   │   │   ├── PlayerArea.fxml
   │   │   ├── Battlefield.fxml
   │   │   └── Dialogs.fxml
   │   ├── css/
   │   │   ├── game.css
   │   │   └── cards.css
   │   └── images/
   ```

2. **pom.xml Dependencies:**
   ```xml
   <dependencies>
       <dependency>
           <groupId>forge</groupId>
           <artifactId>forge-game</artifactId>
       </dependency>
       <dependency>
           <groupId>forge</groupId>
           <artifactId>forge-ai</artifactId>
       </dependency>
       <dependency>
           <groupId>org.openjfx</groupId>
           <artifactId>javafx-controls</artifactId>
           <version>21</version>
       </dependency>
       <dependency>
           <groupId>org.openjfx</groupId>
           <artifactId>javafx-fxml</artifactId>
           <version>21</version>
       </dependency>
       <dependency>
           <groupId>org.openjfx</groupId>
           <artifactId>javafx-web</artifactId>
           <version>21</version>
       </dependency>
       <!-- ControlsFX for enhanced controls -->
       <dependency>
           <groupId>org.controlsfx</groupId>
           <artifactId>controlsfx</artifactId>
           <version>11.2.1</version>
       </dependency>
   </dependencies>
   ```

#### Phase 2: Core UI Components (6-8 weeks)
**Goal:** Build modern, reusable JavaFX components

1. **Custom Controls:**

   **CardView.java** - Modern card display
   ```java
   public class CardView extends StackPane {
       private final ImageView cardImage;
       private final Label cardName;
       private final VBox infoOverlay;
       
       public CardView(CardView model) {
           // Modern card rendering with hover effects
           // Smooth animations using Transition API
       }
       
       public void playAnimation(AnimationType type) {
           // Play/cast/tap animations
       }
   }
   ```

   **BattlefieldPane.java** - Drag-and-drop battlefield
   ```java
   public class BattlefieldPane extends Pane {
       private final Map<CardView, CardNode> cardNodes;
       
       public BattlefieldPane() {
           // Implement drag-and-drop
           // Auto-layout cards with animations
           // Zone highlighting
       }
   }
   ```

   **StackView.java** - Visual stack representation
   ```java
   public class StackView extends VBox {
       public void addSpell(SpellAbilityView sa) {
           // Animated spell addition
           // Click to inspect
       }
       
       public void resolveTop() {
           // Animated removal
       }
   }
   ```

2. **FXML Layouts:**

   **MainGame.fxml** - Main game screen
   ```xml
   <BorderPane xmlns:fx="http://javafx.com/fxml">
       <top>
           <HBox styleClass="opponent-area">
               <fx:include source="PlayerArea.fxml"/>
           </HBox>
       </top>
       <center>
           <SplitPane>
               <fx:include source="Battlefield.fxml"/>
           </SplitPane>
       </center>
       <bottom>
           <HBox styleClass="player-area">
               <fx:include source="PlayerHand.fxml"/>
           </HBox>
       </bottom>
       <right>
           <VBox styleClass="sidebar">
               <fx:include source="Stack.fxml"/>
               <fx:include source="PhaseIndicator.fxml"/>
           </VBox>
       </right>
   </BorderPane>
   ```

3. **Modern CSS Styling:**

   **game.css**
   ```css
   .game-root {
       -fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);
   }
   
   .card-view {
       -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 2);
       -fx-background-radius: 8px;
   }
   
   .card-view:hover {
       -fx-scale-x: 1.1;
       -fx-scale-y: 1.1;
       -fx-cursor: hand;
   }
   
   .battlefield {
       -fx-background-color: rgba(255,255,255,0.05);
       -fx-background-radius: 12px;
       -fx-padding: 20px;
   }
   
   /* Modern animations */
   @keyframes cardDraw {
       from { -fx-translate-y: 100px; -fx-opacity: 0; }
       to { -fx-translate-y: 0; -fx-opacity: 1; }
   }
   ```

#### Phase 3: Game Integration (4-6 weeks)
**Goal:** Implement IGuiGame and IGameController

1. **JavaFxGuiGame.java**
   ```java
   public class JavaFxGuiGame extends AbstractGuiGame {
       private final MainGameController controller;
       
       @Override
       public void updateStack() {
           Platform.runLater(() -> {
               controller.getStackView().refresh(getGameView().getStack());
           });
       }
       
       @Override
       public void showPromptMessage(PlayerView player, String message) {
           Platform.runLater(() -> {
               controller.showPrompt(message);
           });
       }
       
       @Override
       public void updatePhase(boolean saveState) {
           Platform.runLater(() -> {
               controller.getPhaseIndicator().setPhase(
                   getGameView().getPhase()
               );
           });
       }
       
       // ... implement all IGuiGame methods
   }
   ```

2. **PlayerControllerJavaFx.java**
   ```java
   public class PlayerControllerJavaFx extends PlayerControllerHuman {
       private final JavaFxGuiGame gui;
       
       @Override
       public SpellAbility getAbilityToPlay(Card card, List<SpellAbility> abilities, ITriggerEvent event) {
           // Show ability selection dialog
           CountDownLatch latch = new CountDownLatch(1);
           AtomicReference<SpellAbility> selected = new AtomicReference<>();
           
           Platform.runLater(() -> {
               AbilityDialog dialog = new AbilityDialog(abilities);
               dialog.showAndWait().ifPresent(sa -> {
                   selected.set(sa);
                   latch.countDown();
               });
           });
           
           latch.await();
           return selected.get();
       }
       
       // ... implement other PlayerController methods
   }
   ```

#### Phase 4: Advanced Features (6-8 weeks)
**Goal:** Add modern UX enhancements

1. **Features to Implement:**
   - **Smooth Animations:**
     - Card draw animation
     - Spell cast effect
     - Damage indicators
     - Life total changes with particles
   
   - **Enhanced Interactions:**
     - Right-click context menus
     - Keyboard shortcuts
     - Auto-yield configuration
     - Undo/redo visualization
   
   - **Visual Feedback:**
     - Legal target highlighting
     - Valid action indicators
     - Turn/priority indicators
     - Mana availability preview
   
   - **Preferences:**
     - Theme selection (light/dark)
     - Card size adjustment
     - Sound effects
     - Animation speed

2. **Example: Animation Service**
   ```java
   public class AnimationService {
       public Timeline createCastAnimation(CardNode card) {
           return new Timeline(
               new KeyFrame(Duration.ZERO,
                   new KeyValue(card.translateYProperty(), 0),
                   new KeyValue(card.opacityProperty(), 1.0)
               ),
               new KeyFrame(Duration.millis(300),
                   new KeyValue(card.translateYProperty(), -50),
                   new KeyValue(card.opacityProperty(), 0.7,
                       Interpolator.EASE_OUT)
               ),
               new KeyFrame(Duration.millis(600),
                   new KeyValue(card.translateYProperty(), 0),
                   new KeyValue(card.opacityProperty(), 1.0,
                       Interpolator.EASE_IN)
               )
           );
       }
   }
   ```

#### Phase 5: Testing & Refinement (4-6 weeks)
**Goal:** Polish and ensure stability

1. **Testing:**
   - TestFX for automated UI testing
   - Manual playthrough of all game modes
   - Performance profiling
   - Memory leak detection

2. **Optimization:**
   - Image caching
   - Virtual scrolling for large lists
   - Lazy loading
   - Scene graph optimization

### Advantages
- ✅ **Native performance** - No network overhead
- ✅ **Offline play** - Works without internet
- ✅ **Rich desktop UX** - Better keyboard/mouse integration
- ✅ **Easier debugging** - Same JVM as game engine
- ✅ **Gradual migration** - Can coexist with current UI
- ✅ **Lower latency** - Direct method calls

### Challenges
- ⚠️ **JavaFX learning curve** - If you're not familiar with it
- ⚠️ **Desktop only** - No web/mobile without additional work
- ⚠️ **Distribution** - Users need to install/update
- ⚠️ **Cross-platform** - Need to test on Windows/Mac/Linux
- ⚠️ **Modern look** - Requires custom CSS/components (not as trendy as web)

### Estimated Timeline
- **Minimum Viable Product:** 3-4 months
- **Feature Parity:** 6-9 months
- **Polished Release:** 9-12 months

---

## Comparison Matrix

| Aspect | Web UI (React) | JavaFX |
|--------|----------------|---------|
| **Your Expertise** | ✅ High (web dev) | ❓ Low-Medium |
| **Development Speed** | Medium (API layer overhead) | Fast (direct integration) |
| **Accessibility** | ✅ Cloud, any device | Desktop only |
| **Performance** | Good (network dependent) | ✅ Excellent (native) |
| **Distribution** | ✅ Easy (URL) | Requires install |
| **Multiplayer** | ✅ Natural fit | Need to add networking |
| **Mobile Support** | ✅ Possible | Not easily |
| **Modern Look** | ✅ Very modern | Modern with effort |
| **Deployment Complexity** | ⚠️ High (cloud infra) | Low (desktop app) |
| **Maintenance** | Two codebases (frontend+backend) | Single codebase |

---

## Recommendation

### For You Specifically (Web Developer Background):

**Start with Web UI** because:
1. **Leverage your skills** - You'll be most productive with React/TypeScript
2. **Cloud potential** - Makes the game accessible anywhere
3. **Portfolio piece** - More impressive for web dev career
4. **Modern UX** - Easier to achieve trendy, animated UI
5. **Long-term value** - Web is where gaming is headed

### Suggested Hybrid Approach:

**Phase 1:** Build the web API layer (forge-web-api)
- This is valuable even if you later do JavaFX
- Enables future multiplayer/mobile apps
- Can test game engine integration without UI

**Phase 2:** Build a minimal web UI
- Focus on core gameplay first
- Get something playable quickly
- Iterate based on feedback

**Phase 3:** Decide on next steps
- If web UI feels good → continue polishing it
- If it's too slow/complex → consider JavaFX as fallback
- Or maintain both UIs!

---

## Next Steps

1. **Choose your path** based on your priorities
2. **Set up development environment:**
   - For Web: Install Node.js, choose React/Vue
   - For JavaFX: Install JavaFX SDK, Scene Builder
3. **Start with API layer** (recommended for both paths)
4. **Build a proof-of-concept** - Single game screen, basic interaction
5. **Evaluate** - Does it feel good? Is performance acceptable?
6. **Iterate** - Build incrementally, test frequently

Would you like me to create:
- A detailed starter template for the web API?
- A JavaFX proof-of-concept setup?
- Specific code examples for either approach?

Let me know which direction interests you most! 🚀

