package forge.web;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.websocket.WsContext;

import org.tinylog.Logger;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.GuiDisplayUtil;
import forge.util.ThreadUtil;
import forge.web.api.CardSearchHandler;
import forge.web.api.DeckHandler;
import forge.web.api.DeckImportExportHandler;
import forge.web.api.FormatValidationHandler;
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

        // Step 2.5: Install bundled AI decks (must happen after FModel init)
        installBundledAiDecks();

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
            config.routes.post("/api/decks/parse", DeckImportExportHandler::parse);
            config.routes.get("/api/decks/{name}/validate", FormatValidationHandler::validate);
            config.routes.get("/api/decks/{name}/export", DeckImportExportHandler::export);
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
                        case SELECT_CARD -> {
                            GameSession session = activeSessions.get(gameId);
                            if (session != null) {
                                int cardId = ((Number) msg.getPayload()).intValue();
                                handleSelectCard(session, cardId);
                            }
                        }
                        case UNDO -> {
                            GameSession session = activeSessions.get(gameId);
                            if (session != null) {
                                forge.interfaces.IGameController gc =
                                        session.webGuiGame.getGameController();
                                if (gc != null) {
                                    gc.undoLastAction();
                                }
                            }
                        }
                        case SET_AUTO_PASS -> {
                            GameSession session = activeSessions.get(gameId);
                            if (session != null) {
                                boolean enabled = Boolean.TRUE.equals(msg.getPayload());
                                session.webGuiGame.setAutoPassEnabled(enabled);
                            }
                        }
                        default -> Logger.warn("Unknown message type: {}", msg.getType());
                    }
                });

                ws.onClose(ctx -> {
                    String gameId = ctx.pathParam("gameId");
                    Logger.info("WebSocket closed for game: {}", gameId);
                    // Only remove if this session's wsContext matches the closing context
                    // (avoids race with StrictMode double-mount creating a new session)
                    GameSession session = activeSessions.get(gameId);
                    if (session != null && session.wsContext == ctx) {
                        activeSessions.remove(gameId);
                        session.close();
                    }
                });

                ws.onError(ctx -> {
                    String gameId = ctx.pathParam("gameId");
                    Logger.error(ctx.error(), "WebSocket error for game: {}", gameId);
                    GameSession session = activeSessions.get(gameId);
                    if (session != null && session.wsContext == ctx) {
                        activeSessions.remove(gameId);
                        session.close();
                    }
                });
            });
        });
    }

    @SuppressWarnings("unchecked")
    private static void handleStartGame(final WsContext ctx, final String gameId, final InboundMessage msg) {
        if (activeSessions.containsKey(gameId)) {
            Logger.warn("Game session already exists for gameId: {}", gameId);
            return;
        }

        // Parse config from payload (backward-compatible with null payload)
        String deckName = null;
        String aiDeckName = null;
        String format = "Constructed";
        String aiDifficulty = "Medium";

        if (msg.getPayload() instanceof Map) {
            Map<String, Object> config = (Map<String, Object>) msg.getPayload();
            deckName = (String) config.get("deckName");
            aiDeckName = (String) config.get("aiDeckName");
            format = (String) config.getOrDefault("format", "Constructed");
            aiDifficulty = (String) config.getOrDefault("aiDifficulty", "Medium");
        }

        // Map AI difficulty to profile name
        String profile = switch (aiDifficulty) {
            case "Easy" -> "Cautious";
            case "Hard" -> "Reckless";
            case "Goldfish" -> "Default"; // profile doesn't matter for DOES_NOTHING
            default -> "Default"; // Medium
        };

        // Map format to GameType
        GameType gameType = "Commander".equalsIgnoreCase(format)
                ? GameType.Commander : GameType.Constructed;

        // Resolve player deck
        Deck playerDeck = (deckName != null) ? loadDeckByName(deckName) : getDefaultDeck();
        if (playerDeck == null) {
            playerDeck = getDefaultDeck();
        }

        // Resolve AI deck
        Deck aiDeck;
        if (aiDeckName != null) {
            aiDeck = loadDeckByName(aiDeckName);
            if (aiDeck == null) {
                aiDeck = pickRandomDeck(format);
            }
        } else {
            aiDeck = pickRandomDeck(format);
        }
        if (aiDeck == null) {
            Logger.error("No AI deck available for format: {}", format);
            return;
        }

        Logger.info("Starting game {} with deck='{}', aiDeck='{}', format={}, difficulty={}",
                gameId, playerDeck.getName(), aiDeck.getName(), format, aiDifficulty);

        WebInputBridge inputBridge = new WebInputBridge();
        ViewRegistry viewRegistry = new ViewRegistry();
        WebGuiGame webGui = new WebGuiGame(ctx, objectMapper, inputBridge, viewRegistry);

        RegisteredPlayer humanPlayer;
        RegisteredPlayer aiPlayer;
        if (gameType == GameType.Commander) {
            humanPlayer = RegisteredPlayer.forCommander(playerDeck);
            aiPlayer = RegisteredPlayer.forCommander(aiDeck);
        } else {
            humanPlayer = new RegisteredPlayer(playerDeck);
            aiPlayer = new RegisteredPlayer(aiDeck);
        }
        humanPlayer.setPlayer(GamePlayerUtil.getGuiPlayer());
        if ("Goldfish".equals(aiDifficulty)) {
            java.util.Set<forge.ai.AIOption> options = java.util.EnumSet.of(forge.ai.AIOption.DOES_NOTHING);
            aiPlayer.setPlayer(GamePlayerUtil.createAiPlayer("Goldfish", 0, 0, options, profile));
        } else {
            aiPlayer.setPlayer(GamePlayerUtil.createAiPlayer(
                    GuiDisplayUtil.getRandomAiName(), profile));
        }

        List<RegisteredPlayer> players = List.of(humanPlayer, aiPlayer);
        Map<RegisteredPlayer, IGuiGame> guis = Map.of(humanPlayer, webGui);

        forge.gamemodes.match.HostedMatch hostedMatch = new forge.gamemodes.match.HostedMatch();
        webGui.setHostedMatch(hostedMatch);
        GameSession session = new GameSession(hostedMatch, webGui, inputBridge, viewRegistry, ctx);
        activeSessions.put(gameId, session);

        Logger.info("Starting game {} on game thread", gameId);
        // Start match on game thread (NOT WebSocket thread)
        final GameType matchType = gameType;
        ThreadUtil.invokeInGameThread(() -> {
            try {
                hostedMatch.startMatch(matchType, null, players, guis);
            } catch (final Exception e) {
                Logger.error(e, "Error starting game {}", gameId);
                // Only remove if this is still our session (avoids race with StrictMode re-mount)
                activeSessions.remove(gameId, session);
                session.close();
            }
        });
    }

    /**
     * Handle SELECT_CARD: find the CardView by ID and call selectCard on the game controller.
     */
    private static void handleSelectCard(final GameSession session, final int cardId) {
        final forge.interfaces.IGameController gc = session.webGuiGame.getGameController();
        if (gc == null) {
            Logger.warn("No game controller available for SELECT_CARD");
            return;
        }

        // Find CardView by iterating through all player zones
        final forge.game.GameView gv = session.webGuiGame.getGameView();
        if (gv == null) {
            Logger.warn("No game view available for SELECT_CARD");
            return;
        }

        forge.game.card.CardView foundCard = null;
        for (final forge.game.player.PlayerView pv : gv.getPlayers()) {
            for (final forge.game.zone.ZoneType zone : forge.game.zone.ZoneType.values()) {
                final forge.util.collect.FCollectionView<forge.game.card.CardView> cards = pv.getCards(zone);
                if (cards != null) {
                    for (final forge.game.card.CardView cv : cards) {
                        if (cv.getId() == cardId) {
                            foundCard = cv;
                            break;
                        }
                    }
                }
                if (foundCard != null) break;
            }
            if (foundCard != null) break;
        }

        if (foundCard != null) {
            Logger.info("SELECT_CARD: selecting card {} (id={})", foundCard.getName(), cardId);
            gc.selectCard(foundCard, null, null);
        } else {
            Logger.warn("SELECT_CARD: card with id={} not found", cardId);
        }
    }

    /**
     * Loads a deck by name from the constructed decks directory.
     * Tries exact filename match first, then scans recursively.
     */
    private static Deck loadDeckByName(final String name) {
        final File decksDir = new File(ForgeConstants.DECK_CONSTRUCTED_DIR);
        if (!decksDir.exists()) {
            return null;
        }
        final File direct = new File(decksDir, name + ".dck");
        if (direct.exists()) {
            return DeckSerializer.fromFile(direct);
        }
        // Scan directory for matching deck name
        return scanAndLoadDeck(decksDir, name);
    }

    private static Deck scanAndLoadDeck(final File dir, final String name) {
        final File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                final Deck found = scanAndLoadDeck(file, name);
                if (found != null) {
                    return found;
                }
            } else if (file.getName().endsWith(".dck")) {
                final Deck deck = DeckSerializer.fromFile(file);
                if (deck != null && name.equals(deck.getName())) {
                    return deck;
                }
            }
        }
        return null;
    }

    /**
     * Copies bundled AI deck files from resources to the constructed decks directory.
     * Only copies if the target file does not already exist (no overwrite).
     */
    private static void installBundledAiDecks() {
        final String[] deckPaths = {
            "ai-decks/standard/mono-red-aggro.dck",
            "ai-decks/standard/azorius-control.dck",
            "ai-decks/standard/golgari-midrange.dck",
            "ai-decks/casual/mono-green-stompy.dck",
            "ai-decks/casual/burn.dck",
            "ai-decks/casual/white-weenie.dck",
            "ai-decks/commander/atraxa.dck",
            "ai-decks/commander/krenko.dck",
            "ai-decks/commander/tatyova.dck"
        };

        final File decksDir = new File(ForgeConstants.DECK_CONSTRUCTED_DIR);
        for (final String deckPath : deckPaths) {
            final File target = new File(decksDir, deckPath);
            if (target.exists()) {
                continue;
            }
            try (final InputStream is = WebServer.class.getResourceAsStream("/" + deckPath)) {
                if (is == null) {
                    Logger.warn("Bundled deck not found in resources: {}", deckPath);
                    continue;
                }
                target.getParentFile().mkdirs();
                Files.copy(is, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Logger.info("Installed bundled AI deck: {}", deckPath);
            } catch (final Exception e) {
                Logger.warn("Failed to install bundled deck {}: {}", deckPath, e.getMessage());
            }
        }
    }

    /**
     * Picks a random deck from the constructed decks directory,
     * optionally filtering by format. Falls back to any available deck.
     */
    private static Deck pickRandomDeck(final String format) {
        final File decksDir = new File(ForgeConstants.DECK_CONSTRUCTED_DIR);
        if (!decksDir.exists()) {
            Logger.warn("No constructed decks directory found at {}", decksDir.getAbsolutePath());
            return null;
        }
        final List<Deck> candidates = new ArrayList<>();
        collectDecksByFormat(decksDir, format, candidates);
        if (!candidates.isEmpty()) {
            return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }
        // Fallback: pick any deck rather than returning null
        final List<Deck> allDecks = new ArrayList<>();
        collectDecksByFormat(decksDir, null, allDecks);
        if (!allDecks.isEmpty()) {
            Logger.warn("No decks found for format '{}', picking random deck from all available", format);
            return allDecks.get(ThreadLocalRandom.current().nextInt(allDecks.size()));
        }
        Logger.warn("No constructed decks found at all in {}", decksDir.getAbsolutePath());
        return null;
    }

    private static void collectDecksByFormat(final File dir, final String format,
                                              final List<Deck> result) {
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                collectDecksByFormat(file, format, result);
            } else if (file.getName().endsWith(".dck")) {
                final Deck deck = DeckSerializer.fromFile(file);
                if (deck != null && matchesFormat(deck, format)) {
                    result.add(deck);
                }
            }
        }
    }

    private static boolean matchesFormat(final Deck deck, final String format) {
        if (format == null || format.isEmpty()) {
            return true;
        }
        final String comment = deck.getComment();
        if (comment == null || comment.isEmpty()) {
            // Decks without format match "Constructed" and "Casual 60-card"
            return "Constructed".equalsIgnoreCase(format)
                    || "Casual 60-card".equalsIgnoreCase(format);
        }
        return comment.equalsIgnoreCase(format);
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
