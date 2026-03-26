package forge.web.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.tinylog.Logger;

import forge.deck.Deck;
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

    private static final int MAX_TURNS = 50;

    private SimulationRunner() { }

    /**
     * Resolves the AI profile to use for the test deck.
     *
     * <p>If {@code aiProfile} is non-null, non-empty, and not {@code "auto"}, it is returned
     * as-is (explicit override). Otherwise the deck is classified and the matching profile
     * is returned: {@link DeckArchetype#BURN} -> {@code "Burn"}, all other archetypes ->
     * {@code "Default"}.</p>
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
            case BURN:
                resolved = "Burn";
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
     * Start a new simulation run.
     *
     * @param testDeckName   display name of the test deck
     * @param testDeck       the test deck
     * @param opponentDecks  map of opponent name -> deck
     * @param totalGames     total number of games to play
     * @param aiProfile      AI profile to use, or {@code null}/{@code "auto"} for auto-detection
     * @return the SimulationJob tracking this run
     */
    public static SimulationJob startSimulation(final String testDeckName,
                                                 final Deck testDeck,
                                                 final Map<String, Deck> opponentDecks,
                                                 final int totalGames,
                                                 final String aiProfile) {
        final String resolvedProfile = resolveAiProfile(testDeck, aiProfile);
        final String jobId = UUID.randomUUID().toString();
        final SimulationJob job = new SimulationJob(
                jobId, testDeckName,
                new ArrayList<>(opponentDecks.keySet()),
                totalGames
        );
        activeJobs.put(jobId, job);
        simulationExecutor.submit(() -> runSimulation(job, testDeck, opponentDecks, resolvedProfile));
        return job;
    }

    /**
     * Run the simulation: distribute games across opponents, execute sequentially.
     */
    private static void runSimulation(final SimulationJob job,
                                      final Deck testDeck,
                                      final Map<String, Deck> opponentDecks,
                                      final String aiProfile) {
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

        int gameIndex = 0;
        for (int oppIdx = 0; oppIdx < opponents.size(); oppIdx++) {
            final Map.Entry<String, Deck> opponent = opponents.get(oppIdx);
            final int gamesForThisOpponent = gamesPerOpponent + (oppIdx < remainder ? 1 : 0);

            for (int g = 0; g < gamesForThisOpponent; g++) {
                if (job.isCancelled()) {
                    Logger.info("Simulation {} cancelled after {} games", job.getId(), job.getCompletedGames());
                    break;
                }

                try {
                    // Alternate play/draw
                    final boolean onPlay = (gameIndex % 2 == 0);
                    final SimulationResult result = runSingleGame(
                            testDeck, job.getTestDeckName(), opponent.getValue(),
                            opponent.getKey(), onPlay, aiProfile, job.getId());
                    job.addResult(result);
                } catch (final Exception e) {
                    Logger.error(e, "Simulation {} game {} failed", job.getId(), gameIndex);
                    // Continue with next game rather than abort entire simulation
                }

                gameIndex++;
            }

            if (job.isCancelled()) {
                break;
            }
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
        // Build explicit GameRules (no FModel dependency)
        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setPlayForAnte(false);
        rules.setManaBurn(false);
        rules.setGamesPerMatch(1);

        // Create AI players with the specified profile
        final RegisteredPlayer testPlayer = new RegisteredPlayer(testDeck);
        testPlayer.setPlayer(GamePlayerUtil.createAiPlayer(testDeckName, aiProfile));

        // Opponents always use Default profile (Medium difficulty) for consistent benchmarking
        final RegisteredPlayer oppPlayer = new RegisteredPlayer(opponentDeck);
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

        // Wait for the game to finish
        try {
            gameComplete.join();
        } catch (final Exception e) {
            Logger.error(e, "Error waiting for game completion");
        }

        // Extract result
        final SimulationResult extracted = GameStatExtractor.extract(
                hostedMatch, testPlayer, oppPlayer, testDeckPlaysFirst);

        // Stalemate: game exceeded max turns AND nobody won (truly stuck game)
        // Long games with a winner are legitimate results, not stalemates
        final SimulationResult result;
        if (extracted.getTurns() >= MAX_TURNS && !extracted.isWon()) {
            result = new SimulationResult(
                    false, true, extracted.getTurns(), extracted.getMulligans(), extracted.isOnPlay(),
                    extracted.getFinalLifeTotal(), extracted.getOpponentFinalLife(),
                    extracted.getCardsDrawn(), extracted.getEmptyHandTurns(),
                    extracted.getFirstThreatTurn(), extracted.getThirdLandTurn(), extracted.getFourthLandTurn(),
                    extracted.getCardsInHand(), extracted.getCardDrawCounts(),
                    extracted.getOpponentDeckName()
            );
        } else {
            result = extracted;
        }

        // Persist raw game log with per-game stats for recalculation
        try {
            GameLogPersistence.persistGameLog(
                    hostedMatch.getGame(), testDeckName, opponentName,
                    testPlayer, "simulation", simulationId, testDeckPlaysFirst, result);
        } catch (final Exception e) {
            Logger.warn(e, "Failed to persist game log for simulation {}", simulationId);
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
