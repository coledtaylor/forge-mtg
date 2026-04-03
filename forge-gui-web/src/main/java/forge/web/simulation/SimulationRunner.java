package forge.web.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.tinylog.Logger;

import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gui.interfaces.IGuiGame;
import forge.player.GamePlayerUtil;
import forge.web.api.GameLogPersistence;

/**
 * Orchestrates AI-vs-AI simulation games on a dedicated thread.
 * Games run sequentially to avoid concurrent state corruption in the engine.
 */
public final class SimulationRunner {

    private static final ExecutorService simulationExecutor = Executors.newSingleThreadExecutor(r -> {
        final Thread t = new Thread(r, "Simulation-Orchestrator");
        t.setDaemon(true);
        return t;
    });

    private static final ConcurrentHashMap<String, SimulationJob> activeJobs = new ConcurrentHashMap<>();

    private static final int MAX_TURNS_CONSTRUCTED = 25;
    private static final int MAX_TURNS_COMMANDER = 40;

    private SimulationRunner() { }

    /**
     * Resolves the AI profile to use for the test deck.
     *
     * <p>If {@code aiProfile} is non-null, non-empty, and not {@code "auto"}, it is returned
     * as-is (explicit override). Otherwise the deck is classified and the archetype is mapped
     * to the corresponding profile name:</p>
     * <ul>
     *   <li>{@link DeckArchetype#AGGRO}    -> {@code "Aggro"}</li>
     *   <li>{@link DeckArchetype#BURN}     -> {@code "Burn"}</li>
     *   <li>{@link DeckArchetype#MIDRANGE} -> {@code "Midrange"}</li>
     *   <li>{@link DeckArchetype#CONTROL}  -> {@code "Control"}</li>
     *   <li>{@link DeckArchetype#COMBO}    -> {@code "Combo"}</li>
     *   <li>{@link DeckArchetype#UNKNOWN}  -> {@code "Default"}</li>
     * </ul>
     *
     * @param testDeck  the deck being tested
     * @param aiProfile the profile requested by the caller, or {@code null} / {@code "auto"}
     * @return a non-null, non-empty AI profile string
     */
    private static String resolveAiProfile(final Deck testDeck, final String aiProfile) {
        if (aiProfile != null && !aiProfile.trim().isEmpty() && !"auto".equalsIgnoreCase(aiProfile)) {
            return aiProfile;
        }
        final DeckArchetype archetype = DeckArchetypeClassifier.classify(testDeck);
        final String resolved;
        switch (archetype) {
            case AGGRO:
                resolved = "Aggro";
                break;
            case BURN:
                resolved = "Burn";
                break;
            case MIDRANGE:
                resolved = "Midrange";
                break;
            case CONTROL:
                resolved = "Control";
                break;
            case COMBO:
                resolved = "Combo";
                break;
            default:
                resolved = "Default";
                break;
        }
        Logger.info("Deck '{}' classified as {} -> using AI profile '{}'",
                testDeck.getName(), archetype, resolved);
        return resolved;
    }

    /**
     * Returns system info useful for choosing parallelism level.
     *
     * @return map with {@code availableProcessors}, {@code safeMax}, and {@code defaultParallelGames}
     */
    public static Map<String, Integer> getSystemInfo() {
        final int processors = Runtime.getRuntime().availableProcessors();
        final int safeMax = Math.max(1, processors - 2);
        final int defaultParallelGames = Math.max(1, processors / 2);
        final Map<String, Integer> info = new HashMap<>();
        info.put("availableProcessors", processors);
        info.put("safeMax", safeMax);
        info.put("defaultParallelGames", defaultParallelGames);
        return info;
    }

    /**
     * Start a new simulation run.
     *
     * @param testDeckName     display name of the test deck
     * @param testDeck         the test deck
     * @param opponentDecks    map of opponent name -> deck
     * @param totalGames       total number of games to play
     * @param aiProfile        AI profile to use, or {@code null}/{@code "auto"} for auto-detection
     * @param parallelGames    number of games to run concurrently
     * @return the SimulationJob tracking this run
     */
    public static SimulationJob startSimulation(final String testDeckName,
                                                 final Deck testDeck,
                                                 final Map<String, Deck> opponentDecks,
                                                 final int totalGames,
                                                 final String aiProfile,
                                                 final int parallelGames) {
        final String resolvedProfile = resolveAiProfile(testDeck, aiProfile);
        final java.util.Map<String, Double> cardScores = DeckArchetypeClassifier.getPlaystyleScores(testDeck);
        final ManaProfile manaProfile = ManaCurveAnalyzer.analyze(testDeck);
        final String jobId = UUID.randomUUID().toString();
        final SimulationJob job = new SimulationJob(
                jobId, testDeckName,
                new ArrayList<>(opponentDecks.keySet()),
                totalGames,
                cardScores,
                manaProfile
        );
        activeJobs.put(jobId, job);
        simulationExecutor.submit(() -> runSimulation(job, testDeck, opponentDecks, resolvedProfile, parallelGames));
        return job;
    }

    /**
     * Run the simulation: distribute games across opponents and execute them in parallel
     * using a fixed thread pool of size {@code parallelGames}.
     */
    private static void runSimulation(final SimulationJob job,
                                      final Deck testDeck,
                                      final Map<String, Deck> opponentDecks,
                                      final String aiProfile,
                                      final int parallelGames) {
        job.setRunning(true);
        final long startTime = System.currentTimeMillis();
        final int opponentCount = opponentDecks.size();
        if (opponentCount == 0) {
            Logger.warn("Simulation {} has no opponents, completing immediately", job.getId());
            job.setComplete();
            return;
        }

        // Distribute games evenly across opponents
        final List<Map.Entry<String, Deck>> opponents = new ArrayList<>(opponentDecks.entrySet());
        final int gamesPerOpponent = job.getTotalGames() / opponentCount;
        final int remainder = job.getTotalGames() % opponentCount;

        // AtomicInteger so tasks submitted to the thread pool can each grab a unique index
        final AtomicInteger gameIndex = new AtomicInteger(0);

        // Don't create more threads than games
        final int effectiveParallel = Math.min(parallelGames, job.getTotalGames());
        final ExecutorService gamePool = Executors.newFixedThreadPool(effectiveParallel, r -> {
            final Thread t = new Thread(r, "Sim-Game-" + job.getId().substring(0, 8));
            t.setDaemon(true);
            return t;
        });
        final ExecutorCompletionService<SimulationResult> completionService =
                new ExecutorCompletionService<>(gamePool);

        int tasksSubmitted = 0;
        try {
            for (int oppIdx = 0; oppIdx < opponents.size(); oppIdx++) {
                final Map.Entry<String, Deck> opponent = opponents.get(oppIdx);
                final int gamesForThisOpponent = gamesPerOpponent + (oppIdx < remainder ? 1 : 0);

                for (int g = 0; g < gamesForThisOpponent; g++) {
                    if (job.isCancelled()) {
                        Logger.info("Simulation {} cancelled before submitting game {}",
                                job.getId(), tasksSubmitted);
                        break;
                    }

                    final int idx = gameIndex.getAndIncrement();
                    final boolean onPlay = (idx % 2 == 0);
                    final String oppName = opponent.getKey();
                    final Deck oppDeck = opponent.getValue();

                    completionService.submit(() -> {
                        if (job.isCancelled()) {
                            return null;
                        }
                        return runSingleGame(
                                testDeck, job.getTestDeckName(), oppDeck,
                                oppName, onPlay, aiProfile, job.getId());
                    });
                    tasksSubmitted++;
                }

                if (job.isCancelled()) {
                    break;
                }
            }

            // Collect results as they complete
            for (int i = 0; i < tasksSubmitted; i++) {
                try {
                    final Future<SimulationResult> future = completionService.take();
                    final SimulationResult result = future.get();
                    if (result != null) {
                        job.addResult(result);
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Logger.warn("Simulation {} interrupted while collecting results", job.getId());
                    break;
                } catch (final Exception e) {
                    Logger.error(e, "Simulation {} a game task failed", job.getId());
                    // Continue collecting remaining results
                }
            }
        } finally {
            gamePool.shutdown();
        }

        final long elapsed = System.currentTimeMillis() - startTime;
        Logger.info("Simulation {} completed: {}/{} games in {}ms",
                job.getId(), job.getCompletedGames(), job.getTotalGames(), elapsed);
        job.setComplete();
    }

    /**
     * Run a single AI-vs-AI game and extract the result.
     */
    private static SimulationResult runSingleGame(final Deck testDeck,
                                                   final String testDeckName,
                                                   final Deck opponentDeck,
                                                   final String opponentName,
                                                   final boolean testDeckPlaysFirst,
                                                   final String aiProfile,
                                                   final String simulationId) {
        // Detect commander format from deck structure
        final boolean isCommander = !testDeck.getCommanders().isEmpty();
        final GameType gameType = isCommander ? GameType.Commander : GameType.Constructed;

        // Build explicit GameRules
        final GameRules rules = new GameRules(gameType);
        rules.setPlayForAnte(false);
        rules.setManaBurn(false);
        rules.setGamesPerMatch(1);

        // Create AI players — commander decks get 40 life and command zone setup
        final RegisteredPlayer testPlayer = isCommander
                ? RegisteredPlayer.forCommander(testDeck)
                : new RegisteredPlayer(testDeck);
        testPlayer.setPlayer(GamePlayerUtil.createAiPlayer(testDeckName, aiProfile));

        // Opponents always use Default profile (Medium difficulty) for consistent benchmarking
        final boolean oppIsCommander = !opponentDeck.getCommanders().isEmpty();
        final RegisteredPlayer oppPlayer = oppIsCommander
                ? RegisteredPlayer.forCommander(opponentDeck)
                : new RegisteredPlayer(opponentDeck);
        oppPlayer.setPlayer(GamePlayerUtil.createAiPlayer(opponentName, "Default"));

        // Order determines play/draw
        final List<RegisteredPlayer> players = testDeckPlaysFirst
                ? List.of(testPlayer, oppPlayer)
                : List.of(oppPlayer, testPlayer);

        // Empty guis map triggers the humanCount==0 spectator path
        // WebGuiBase.getNewGuiGame() returns HeadlessGuiGame
        final Map<RegisteredPlayer, IGuiGame> guis = Map.of();

        // Use a CompletableFuture to wait for game completion
        final CompletableFuture<Void> gameComplete = new CompletableFuture<>();

        final HostedMatch hostedMatch = new HostedMatch();
        hostedMatch.setEndGameHook(() -> gameComplete.complete(null));
        hostedMatch.startMatch(rules, null, players, guis, null);

        // Capture game reference now — hostedMatch.getGame() may return null after game ends
        final Game gameRef = hostedMatch.getGame();

        // Commander boards are more complex (token armies, many activated abilities);
        // give the AI more time per decision to avoid constant timeouts
        if (isCommander && gameRef != null) {
            gameRef.AI_TIMEOUT = 15;
        }

        // Wait for the game to finish with a wall-clock timeout.
        // Without this, a stalled game (e.g. repeated AI timeouts with no progress)
        // would block forever since MAX_TURNS is only checked post-hoc.
        final int maxTurns = isCommander ? MAX_TURNS_COMMANDER : MAX_TURNS_CONSTRUCTED;
        final int gameTimeoutSeconds = isCommander ? 300 : 120; // 5 min for commander, 2 min for constructed
        try {
            gameComplete.get(gameTimeoutSeconds, TimeUnit.SECONDS);
        } catch (final TimeoutException e) {
            Logger.warn("Simulation {}: game exceeded wall-clock timeout ({}s), forcing game over",
                    simulationId, gameTimeoutSeconds);
            if (gameRef != null && !gameRef.isGameOver()) {
                gameRef.setGameOver(GameEndReason.Draw);
            }
            // Give the endGameHook a moment to fire after setGameOver
            try {
                gameComplete.get(5, TimeUnit.SECONDS);
            } catch (final Exception ignored) { }
        } catch (final ExecutionException | InterruptedException e) {
            Logger.error(e, "Error waiting for game completion in simulation {}", simulationId);
        }

        // Extract result — use captured gameRef since hostedMatch.getGame() may be null after game ends
        if (gameRef == null) {
            Logger.warn("Simulation {}: game reference is null, skipping (opponent={})", simulationId, opponentName);
            return null;
        }
        final SimulationResult extracted = GameStatExtractor.extract(
                gameRef, testPlayer, oppPlayer, testDeckPlaysFirst);

        // Guard: reject games that never actually played (setup failure, thread-safety issue)
        if (extracted.getTurns() == 0) {
            Logger.warn("Simulation {} discarding invalid game: 0 turns played (opponent={})",
                    simulationId, opponentName);
            return null;
        }

        // Stalemate: game exceeded max turns AND nobody won (truly stuck game)
        // Long games with a winner are legitimate results, not stalemates
        final SimulationResult result;
        if (extracted.getTurns() >= maxTurns && !extracted.isWon()) {
            result = new SimulationResult(
                    false, true, extracted.getTurns(), extracted.getMulligans(), extracted.isOnPlay(),
                    extracted.getFinalLifeTotal(), extracted.getOpponentFinalLife(),
                    extracted.getCardsDrawn(), extracted.getEmptyHandTurns(),
                    extracted.getFirstThreatTurn(), extracted.getThirdLandTurn(), extracted.getFourthLandTurn(),
                    extracted.getTotalLandsPlayed(),
                    extracted.getCardsInHand(), extracted.getCardDrawCounts(),
                    extracted.getOpponentDeckName()
            );
        } else {
            result = extracted;
        }

        // Persist raw game log with per-game stats for recalculation
        if (gameRef != null) {
            try {
                GameLogPersistence.persistGameLog(
                        gameRef, testDeckName, opponentName,
                        testPlayer, "simulation", simulationId, testDeckPlaysFirst, result);
            } catch (final Exception e) {
                Logger.warn(e, "Failed to persist game log for simulation {}", simulationId);
            }
        }

        return result;
    }

    /**
     * Look up a simulation job by ID.
     */
    public static SimulationJob getJob(final String id) {
        return activeJobs.get(id);
    }

    /**
     * Cancel a running simulation.
     */
    public static void cancelJob(final String id) {
        final SimulationJob job = activeJobs.get(id);
        if (job != null) {
            job.cancel();
        }
    }
}
