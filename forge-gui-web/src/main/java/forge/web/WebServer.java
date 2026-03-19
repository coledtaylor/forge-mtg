package forge.web;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.websocket.WsContext;

import org.tinylog.Logger;

import forge.deck.Deck;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.ThreadUtil;
import forge.web.api.CardSearchHandler;
import forge.web.api.DeckHandler;
import forge.web.protocol.InboundMessage;

/**
 * Entry point for the Forge web server.
 * Initializes the engine in correct order: GuiBase -> FModel -> Javalin.
 * Provides WebSocket endpoint for real-time game communication.
 */
public class WebServer {

    private static final ConcurrentHashMap<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private static ObjectMapper objectMapper;

    private WebServer() { } // no instances

    /**
     * Returns a snapshot of active session IDs (for debugging/testing).
     */
    static ConcurrentHashMap<String, GameSession> getActiveSessions() {
        return activeSessions;
    }

    public static void main(String[] args) {
        // Step 1 (MUST BE FIRST): Set up the GUI interface before any Forge class loading
        String assetsDir = determineAssetsDir(args);
        Logger.info("Setting up WebGuiBase with assets dir: {}", assetsDir);
        GuiBase.setInterface(new WebGuiBase(assetsDir));

        // Step 2: Initialize FModel with headless preferences
        Logger.info("Initializing FModel...");
        FModel.initialize(null, preferences -> {
            preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
            preferences.setPref(FPref.UI_LANGUAGE, "en-US");
            preferences.setPref(FPref.UI_ENABLE_SOUNDS, false);
            return null;
        });
        Logger.info("FModel initialized successfully");

        // Step 3: Create and start Javalin server
        Logger.info("Starting Javalin on port 8080...");
        objectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new ParameterNamesModule());

        Javalin app = createApp(objectMapper);
        app.start(8080);

        // Shutdown hook for clean teardown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info("Shutting down...");
            app.stop();
            cleanupAllSessions();
            ((WebGuiBase) GuiBase.getInterface()).shutdown();
        }));

        Logger.info("Forge web server started on port 8080");
    }

    /**
     * Creates and configures a Javalin app with all routes and WebSocket endpoints.
     * Extracted for testability -- integration tests can call this with a test ObjectMapper.
     */
    static Javalin createApp(final ObjectMapper mapper) {
        objectMapper = mapper;
        return Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, false));

            // REST endpoints
            config.routes.get("/health", ctx -> ctx.result("ok"));
            config.routes.get("/api/sessions", ctx -> ctx.json(activeSessions.keySet()));

            // Card search and deck CRUD endpoints
            config.routes.get("/api/cards", CardSearchHandler::search);
            config.routes.get("/api/decks", DeckHandler::list);
            config.routes.post("/api/decks", DeckHandler::create);
            config.routes.get("/api/decks/{name}", DeckHandler::get);
            config.routes.put("/api/decks/{name}", DeckHandler::update);
            config.routes.delete("/api/decks/{name}", DeckHandler::delete);

            // WebSocket game endpoint
            config.routes.ws("/ws/game/{gameId}", ws -> {
                ws.onConnect(ctx -> {
                    String gameId = ctx.pathParam("gameId");
                    Logger.info("WebSocket connected for game: {}", gameId);
                    ctx.enableAutomaticPings();
                    // Store the context for later use when START_GAME arrives
                    ctx.attribute("gameId", gameId);
                });

                ws.onMessage(ctx -> {
                    String gameId = ctx.pathParam("gameId");
                    InboundMessage msg = mapper.readValue(ctx.message(), InboundMessage.class);

                    switch (msg.getType()) {
                        case START_GAME -> handleStartGame(ctx, gameId, msg);
                        case CHOICE_RESPONSE, CONFIRM_RESPONSE, AMOUNT_RESPONSE -> {
                            GameSession session = activeSessions.get(gameId);
                            if (session != null) {
                                String responseJson = mapper.writeValueAsString(msg.getPayload());
                                boolean completed = session.inputBridge.complete(msg.getInputId(), responseJson);
                                if (!completed) {
                                    Logger.warn("No pending input for inputId: {}", msg.getInputId());
                                }
                            }
                        }
                        case BUTTON_OK -> {
                            GameSession session = activeSessions.get(gameId);
                            if (session != null) {
                                forge.interfaces.IGameController gc =
                                        session.webGuiGame.getGameController();
                                if (gc != null) {
                                    gc.selectButtonOk();
                                }
                            }
                        }
                        case BUTTON_CANCEL -> {
                            GameSession session = activeSessions.get(gameId);
                            if (session != null) {
                                forge.interfaces.IGameController gc =
                                        session.webGuiGame.getGameController();
                                if (gc != null) {
                                    gc.selectButtonCancel();
                                }
                            }
                        }
                        default -> Logger.warn("Unknown message type: {}", msg.getType());
                    }
                });

                ws.onClose(ctx -> {
                    String gameId = ctx.pathParam("gameId");
                    Logger.info("WebSocket closed for game: {}", gameId);
                    GameSession session = activeSessions.remove(gameId);
                    if (session != null) {
                        session.close();
                    }
                });

                ws.onError(ctx -> {
                    String gameId = ctx.pathParam("gameId");
                    Logger.error(ctx.error(), "WebSocket error for game: {}", gameId);
                    GameSession session = activeSessions.remove(gameId);
                    if (session != null) {
                        session.close();
                    }
                });
            });
        });
    }

    private static void handleStartGame(final WsContext ctx, final String gameId, final InboundMessage msg) {
        if (activeSessions.containsKey(gameId)) {
            Logger.warn("Game session already exists for gameId: {}", gameId);
            return;
        }

        WebInputBridge inputBridge = new WebInputBridge();
        ViewRegistry viewRegistry = new ViewRegistry();
        WebGuiGame webGui = new WebGuiGame(ctx, objectMapper, inputBridge, viewRegistry);

        // Build human and AI players with default decks
        RegisteredPlayer humanPlayer = new RegisteredPlayer(getDefaultDeck())
                .setPlayer(GamePlayerUtil.getGuiPlayer());
        RegisteredPlayer aiPlayer = new RegisteredPlayer(getDefaultAiDeck())
                .setPlayer(GamePlayerUtil.createAiPlayer());

        List<RegisteredPlayer> players = List.of(humanPlayer, aiPlayer);
        Map<RegisteredPlayer, IGuiGame> guis = Map.of(humanPlayer, webGui);

        forge.gamemodes.match.HostedMatch hostedMatch = new forge.gamemodes.match.HostedMatch();
        GameSession session = new GameSession(hostedMatch, webGui, inputBridge, viewRegistry, ctx);
        activeSessions.put(gameId, session);

        Logger.info("Starting game {} on game thread", gameId);
        // Start match on game thread (NOT WebSocket thread)
        ThreadUtil.invokeInGameThread(() -> {
            try {
                hostedMatch.startMatch(GameType.Constructed, null, players, guis);
            } catch (final Exception e) {
                Logger.error(e, "Error starting game {}", gameId);
                activeSessions.remove(gameId);
                session.close();
            }
        });
    }

    /**
     * Creates a minimal 60-card deck of basic Mountains.
     * Placeholder until Phase 5 adds proper deck selection.
     */
    static Deck getDefaultDeck() {
        Deck deck = new Deck("Web Default Deck");
        deck.getMain().add("Mountain", 60);
        return deck;
    }

    /**
     * Creates a minimal 60-card AI deck of basic Forests.
     */
    static Deck getDefaultAiDeck() {
        Deck deck = new Deck("AI Default Deck");
        deck.getMain().add("Forest", 60);
        return deck;
    }

    private static void cleanupAllSessions() {
        for (GameSession session : activeSessions.values()) {
            session.close();
        }
        activeSessions.clear();
    }

    private static String determineAssetsDir(String[] args) {
        if (args.length > 0) {
            return args[0];
        }
        String sysProp = System.getProperty("forge.assets.dir");
        if (sysProp != null) {
            return sysProp;
        }
        // Auto-detect: check common relative paths from typical working directories
        for (String candidate : new String[]{"forge-gui/", "../forge-gui/"}) {
            if (new java.io.File(candidate + "res/languages/en-US.properties").exists()) {
                return candidate;
            }
        }
        return "../forge-gui/";
    }

    // ========================================================================
    // Inner class: Game Session
    // ========================================================================

    /**
     * Holds all state for an active game session.
     */
    static class GameSession {
        final forge.gamemodes.match.HostedMatch hostedMatch;
        final WebGuiGame webGuiGame;
        final WebInputBridge inputBridge;
        final ViewRegistry viewRegistry;
        final WsContext wsContext;

        GameSession(final forge.gamemodes.match.HostedMatch hostedMatch,
                    final WebGuiGame webGuiGame,
                    final WebInputBridge inputBridge,
                    final ViewRegistry viewRegistry,
                    final WsContext wsContext) {
            this.hostedMatch = hostedMatch;
            this.webGuiGame = webGuiGame;
            this.inputBridge = inputBridge;
            this.viewRegistry = viewRegistry;
            this.wsContext = wsContext;
        }

        void close() {
            Logger.info("Closing game session");
            inputBridge.cancelAll();
            viewRegistry.clear();
        }
    }
}
