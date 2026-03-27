package forge.web.api;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.javalin.http.Context;

import org.tinylog.Logger;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.localinstance.properties.ForgeConstants;
import forge.web.WebServer;
import forge.web.simulation.SimulationJob;
import forge.web.simulation.SimulationRunner;
import forge.web.simulation.SimulationSummary;

/**
 * REST handler for simulation CRUD operations.
 * Manages starting simulations, querying status/history, cancellation,
 * and persisting results in the SQLite simulation database.
 */
public final class SimulationHandler {

    private static final ObjectMapper JSON = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new ParameterNamesModule());

    private static final Set<Integer> VALID_GAME_COUNTS = Set.of(10, 50, 100, 500);

    private SimulationHandler() { }

    private static final Set<String> VALID_AI_PROFILES = Set.of("Reckless", "Default", "Cautious", "Experimental", "Burn", "auto");

    /**
     * POST /api/simulations/start
     * Body: { deckName: string, gameCount: number, aiProfile?: string, opponentDeckNames?: string[] }
     */
    @SuppressWarnings("unchecked")
    public static void start(final Context ctx) {
        final Map<String, Object> body = ctx.bodyAsClass(Map.class);
        final String deckName = (String) body.get("deckName");
        final Number gameCountNum = (Number) body.get("gameCount");

        if (deckName == null || deckName.trim().isEmpty()) {
            ctx.status(400).json(Map.of("error", "deckName is required"));
            return;
        }
        if (gameCountNum == null || !VALID_GAME_COUNTS.contains(gameCountNum.intValue())) {
            ctx.status(400).json(Map.of("error", "gameCount must be one of [10, 50, 100, 500]"));
            return;
        }

        final int gameCount = gameCountNum.intValue();

        // Validate AI profile. null / missing / "auto" -> pass null to trigger auto-detection.
        // An explicit profile name must be in VALID_AI_PROFILES; unknown names fall back to auto-detect.
        final String rawProfile = (String) body.get("aiProfile");
        final String aiProfile;
        if (rawProfile == null || rawProfile.trim().isEmpty() || "auto".equalsIgnoreCase(rawProfile)) {
            aiProfile = null; // signal auto-detect
        } else if (VALID_AI_PROFILES.contains(rawProfile)) {
            aiProfile = rawProfile;
        } else {
            aiProfile = null; // unknown profile -> auto-detect
            Logger.warn("Unknown aiProfile '{}', falling back to auto-detect", rawProfile);
        }

        // Load the test deck
        final Deck testDeck = loadDeckByName(deckName);
        if (testDeck == null) {
            ctx.status(404).json(Map.of("error", "Deck not found: " + deckName));
            return;
        }

        // Load opponents
        final Map<String, Deck> opponentDecks = new LinkedHashMap<>();
        final List<String> opponentDeckNames = (List<String>) body.get("opponentDeckNames");

        if (opponentDeckNames != null && !opponentDeckNames.isEmpty()) {
            // Load specific opponent decks
            for (final String oppName : opponentDeckNames) {
                final Deck oppDeck = loadDeckByName(oppName);
                if (oppDeck != null && !oppName.equals(deckName)) {
                    opponentDecks.put(oppName, oppDeck);
                }
            }
        } else {
            // Load all decks in constructed directory as opponents
            final File decksDir = new File(ForgeConstants.DECK_CONSTRUCTED_DIR);
            if (decksDir.exists() && decksDir.isDirectory()) {
                collectAllDecks(decksDir, deckName, opponentDecks);
            }
        }

        if (opponentDecks.isEmpty()) {
            ctx.status(400).json(Map.of("error", "No opponent decks available"));
            return;
        }

        final SimulationJob job = SimulationRunner.startSimulation(
                deckName, testDeck, opponentDecks, gameCount, aiProfile);

        // Register completion callback to persist results
        job.addProgressListener(summary -> {
            if (job.isComplete() || (job.isCancelled() && job.getCompletedGames() > 0)) {
                persistResult(deckName, job.getId(), summary);
            }
        });

        ctx.json(Map.of("id", job.getId()));
    }

    /**
     * GET /api/simulations/{id}/status
     */
    @SuppressWarnings("unchecked")
    public static void status(final Context ctx) {
        final String id = ctx.pathParam("id");
        final SimulationJob job = SimulationRunner.getJob(id);

        if (job != null) {
            final SimulationSummary summary = job.getProgress();
            ctx.json(wrapStatus(summary, job.isRunning(), job.isComplete(), job.isCancelled()));
            return;
        }

        // Fall back to database lookup for completed simulations
        final SimulationDatabase db = WebServer.getDatabase();
        if (db != null) {
            final Map<String, Object> run = db.getSimulationRun(id);
            if (run != null) {
                final String summaryJson = (String) run.get("summary_json");
                if (summaryJson != null) {
                    try {
                        final Map<String, Object> response = JSON.readValue(summaryJson, Map.class);
                        response.put("status", "complete");
                        ctx.json(response);
                        return;
                    } catch (final IOException e) {
                        Logger.warn("Failed to deserialize summary_json for id={}: {}", id, e.getMessage());
                    }
                }
            }
        }

        ctx.status(404).json(Map.of("error", "Simulation not found"));
    }

    /**
     * POST /api/simulations/{id}/cancel
     */
    public static void cancel(final Context ctx) {
        final String id = ctx.pathParam("id");
        SimulationRunner.cancelJob(id);
        ctx.json(Map.of("status", "cancelled"));
    }

    /**
     * GET /api/simulations/history/{deckName}
     */
    @SuppressWarnings("unchecked")
    public static void history(final Context ctx) {
        final String deckName = ctx.pathParam("deckName");
        final List<Map<String, Object>> entries = new ArrayList<>();

        final SimulationDatabase db = WebServer.getDatabase();
        if (db != null) {
            final List<Map<String, Object>> runs = db.listSimulationRunsByDeck(deckName);
            for (final Map<String, Object> run : runs) {
                final Map<String, Object> entry = new LinkedHashMap<>();
                // Populate fields from summary_json (which contains the full serialized summary)
                final String summaryJson = (String) run.get("summary_json");
                if (summaryJson != null) {
                    try {
                        final Map<String, Object> full = JSON.readValue(summaryJson, Map.class);
                        entry.put("id", full.getOrDefault("id", run.get("id")));
                        entry.put("timestamp", full.getOrDefault("timestamp", run.get("timestamp")));
                        entry.put("gamesCompleted", full.get("gamesCompleted"));
                        entry.put("gamesTotal", full.get("gamesTotal"));
                        entry.put("winRate", full.get("winRate"));
                        // Support powerScore/tier; fall back to Wilson for old records missing powerScore
                        if (full.get("powerScore") != null) {
                            entry.put("powerScore", full.get("powerScore"));
                            entry.put("tier", full.get("tier"));
                        } else {
                            final Number winRateNum = (Number) full.get("winRate");
                            final Number gamesNum = (Number) full.get("gamesCompleted");
                            final int gcompleted = gamesNum != null ? gamesNum.intValue() : 0;
                            final double wr = winRateNum != null ? winRateNum.doubleValue() : 0.0;
                            final int estimatedWins = (int) Math.round(wr / 100.0 * gcompleted);
                            final forge.web.simulation.WilsonCalculator.WilsonResult wilson =
                                    forge.web.simulation.WilsonCalculator.compute(estimatedWins, gcompleted);
                            entry.put("powerScore", wilson.getPowerScore());
                            entry.put("tier", wilson.getTier());
                        }
                        entries.add(entry);
                    } catch (final IOException e) {
                        Logger.warn("Failed to deserialize summary_json for run id={}: {}", run.get("id"), e.getMessage());
                    }
                } else {
                    // Minimal fallback when summary_json is absent
                    entry.put("id", run.get("id"));
                    entry.put("timestamp", run.get("timestamp"));
                    entry.put("gamesCompleted", run.get("total_games"));
                    entry.put("gamesTotal", run.get("total_games"));
                    entry.put("winRate", null);
                    entry.put("powerScore", run.get("power_score"));
                    entry.put("tier", run.get("archetype"));
                    entries.add(entry);
                }
            }
        }

        // Database already returns results ordered by timestamp DESC; preserve that order
        ctx.json(entries);
    }

    /**
     * DELETE /api/simulations/{id}
     */
    public static void deleteResult(final Context ctx) {
        final String id = ctx.pathParam("id");
        final SimulationDatabase db = WebServer.getDatabase();

        if (db == null) {
            ctx.status(503).json(Map.of("error", "Database unavailable"));
            return;
        }

        // Check existence before deleting to return 404 if not found
        final Map<String, Object> run = db.getSimulationRun(id);
        if (run == null) {
            ctx.status(404).json(Map.of("error", "Result not found"));
            return;
        }

        db.deleteSimulationRun(id);
        ctx.status(204);
    }

    /**
     * POST /api/simulations/{id}/recalculate
     * Reconstructs SimulationResults from saved game logs and recomputes the summary.
     */
    @SuppressWarnings("unchecked")
    public static void recalculate(final Context ctx) {
        final String id = ctx.pathParam("id");

        final SimulationDatabase db = WebServer.getDatabase();
        if (db == null) {
            ctx.status(503).json(Map.of("error", "Database unavailable"));
            return;
        }

        // Load the existing simulation run to get deckName and totalGames
        final Map<String, Object> run = db.getSimulationRun(id);
        if (run == null) {
            ctx.status(404).json(Map.of("error", "Simulation result not found"));
            return;
        }

        final String deckName = (String) run.get("deck_name");
        final int totalGames = run.get("total_games") != null
                ? ((Number) run.get("total_games")).intValue() : 0;

        // Query game logs for this simulation via indexed SQL (replaces file-scanning loop)
        final List<Map<String, Object>> logRows = db.getGameLogsBySimulationId(id);
        if (logRows.isEmpty()) {
            ctx.status(404).json(Map.of("error", "No game logs found for this simulation"));
            return;
        }

        // Reconstruct SimulationResults from database rows
        final List<forge.web.simulation.SimulationResult> results = new ArrayList<>();
        for (final Map<String, Object> row : logRows) {
            try {
                final boolean won = Boolean.TRUE.equals(row.get("won"))
                        || (row.get("won") instanceof Number && ((Number) row.get("won")).intValue() != 0);
                final boolean stalemate = Boolean.TRUE.equals(row.get("stalemate"))
                        || (row.get("stalemate") instanceof Number && ((Number) row.get("stalemate")).intValue() != 0);
                final int turns = row.get("turns") != null ? ((Number) row.get("turns")).intValue() : 0;
                final int mulligans = row.get("mulligans") != null ? ((Number) row.get("mulligans")).intValue() : 0;
                final boolean onPlay = Boolean.TRUE.equals(row.get("on_play"))
                        || (row.get("on_play") instanceof Number && ((Number) row.get("on_play")).intValue() != 0);
                final int finalLife = row.get("final_life") != null ? ((Number) row.get("final_life")).intValue() : 0;
                final int oppLife = row.get("opponent_final_life") != null ? ((Number) row.get("opponent_final_life")).intValue() : 0;
                final int cardsDrawn = row.get("cards_drawn") != null ? ((Number) row.get("cards_drawn")).intValue() : 0;
                final int emptyHandTurns = row.get("empty_hand_turns") != null ? ((Number) row.get("empty_hand_turns")).intValue() : 0;
                final int firstThreatTurn = row.get("first_threat_turn") != null ? ((Number) row.get("first_threat_turn")).intValue() : 0;
                final int thirdLandTurn = row.get("third_land_turn") != null ? ((Number) row.get("third_land_turn")).intValue() : 0;
                final int fourthLandTurn = row.get("fourth_land_turn") != null ? ((Number) row.get("fourth_land_turn")).intValue() : 0;
                final int totalLandsPlayed;
                if (row.get("total_lands_played") != null) {
                    totalLandsPlayed = ((Number) row.get("total_lands_played")).intValue();
                } else {
                    // Fallback: estimate as ~1/3 of cards drawn (rough average land ratio)
                    totalLandsPlayed = (int) Math.round(cardsDrawn * 0.33);
                }

                // Deserialize cardsInHand from JSON string column
                final String cardsInHandJson = (String) row.get("cards_in_hand_json");
                final List<String> cardsInHand;
                if (cardsInHandJson != null && !cardsInHandJson.isEmpty()) {
                    cardsInHand = JSON.readValue(cardsInHandJson, new TypeReference<List<String>>() { });
                } else {
                    cardsInHand = List.of();
                }

                // Deserialize cardDrawCounts from JSON string column
                final String cardDrawCountsJson = (String) row.get("card_draw_counts_json");
                final Map<String, Integer> cardDrawCounts;
                if (cardDrawCountsJson != null && !cardDrawCountsJson.isEmpty()) {
                    cardDrawCounts = JSON.readValue(cardDrawCountsJson, new TypeReference<Map<String, Integer>>() { });
                } else {
                    cardDrawCounts = Map.of();
                }

                final String oppDeckName = (String) row.get("opponent_deck");

                results.add(new forge.web.simulation.SimulationResult(
                        won, stalemate, turns, mulligans, onPlay,
                        finalLife, oppLife, cardsDrawn, emptyHandTurns,
                        firstThreatTurn, thirdLandTurn, fourthLandTurn,
                        totalLandsPlayed,
                        cardsInHand, cardDrawCounts, oppDeckName
                ));
            } catch (final Exception e) {
                Logger.warn("Failed to reconstruct SimulationResult from row id={}: {}", row.get("id"), e.getMessage());
            }
        }

        if (results.isEmpty()) {
            ctx.status(404).json(Map.of("error", "No game logs with stats found for this simulation"));
            return;
        }

        // Load deck for card-based playstyle scores and mana profile (if still available)
        java.util.Map<String, Double> cardScores = null;
        forge.web.simulation.ManaProfile manaProfile = null;
        if (deckName != null) {
            final Deck recalcDeck = loadDeckByName(deckName);
            if (recalcDeck != null) {
                cardScores = forge.web.simulation.DeckArchetypeClassifier.getPlaystyleScores(recalcDeck);
                manaProfile = forge.web.simulation.ManaCurveAnalyzer.analyze(recalcDeck);
            }
        }

        // Recompute summary
        final forge.web.simulation.SimulationSummary summary =
                forge.web.simulation.SimulationSummary.computeFrom(results, totalGames, cardScores, manaProfile);

        // Persist updated result
        persistResult(deckName, id, summary);

        // Return the new summary
        final Map<String, Object> response = JSON.convertValue(summary, Map.class);
        response.put("status", "complete");
        ctx.json(response);
    }

    // ========================================================================
    // Result persistence
    // ========================================================================

    /**
     * Persist a simulation result to the SQLite database.
     * Tries insert first; falls back to update if the record already exists (e.g. recalculate).
     */
    private static void persistResult(final String deckName, final String jobId,
                                       final SimulationSummary summary) {
        final SimulationDatabase db = WebServer.getDatabase();
        if (db == null) {
            Logger.warn("SimulationDatabase unavailable, cannot persist result for jobId={}", jobId);
            return;
        }

        final String timestamp = Instant.now().toString();
        final String archetype = summary.getTier(); // use tier as archetype column value
        final double powerScore = summary.getPowerScore();
        final int totalGames = summary.getGamesTotal();

        String summaryJson;
        try {
            // Serialize the full summary to JSON for the summary_json column
            final Map<String, Object> summaryMap = JSON.convertValue(summary, Map.class);
            summaryMap.put("id", jobId);
            summaryMap.put("deckName", deckName);
            summaryMap.put("timestamp", timestamp);
            summaryJson = JSON.writeValueAsString(summaryMap);
        } catch (final IOException e) {
            Logger.error(e, "Failed to serialize SimulationSummary for jobId={}", jobId);
            return;
        }

        // Check if the record already exists (recalculate calls persistResult again)
        final Map<String, Object> existing = db.getSimulationRun(jobId);
        if (existing == null) {
            db.insertSimulationRun(jobId, deckName, timestamp, totalGames, summaryJson, archetype, powerScore);
            Logger.info("Inserted simulation run: id={}, deck={}", jobId, deckName);
        } else {
            db.updateSimulationRun(jobId, totalGames, summaryJson, archetype, powerScore);
            Logger.info("Updated simulation run: id={}, deck={}", jobId, deckName);
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static Map<String, Object> wrapStatus(final SimulationSummary summary,
                                                    final boolean running,
                                                    final boolean complete,
                                                    final boolean cancelled) {
        final Map<String, Object> result = JSON.convertValue(summary, Map.class);
        final String status;
        if (running) {
            status = "running";
        } else if (cancelled) {
            status = "cancelled";
        } else if (complete) {
            status = "complete";
        } else {
            status = "pending";
        }
        result.put("status", status);
        return result;
    }

    private static Deck loadDeckByName(final String name) {
        final File decksDir = new File(ForgeConstants.DECK_CONSTRUCTED_DIR);
        if (!decksDir.exists()) {
            return null;
        }
        final File direct = new File(decksDir, name + ".dck");
        if (direct.exists()) {
            return DeckSerializer.fromFile(direct);
        }
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

    private static void collectAllDecks(final File dir, final String excludeName,
                                          final Map<String, Deck> result) {
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                collectAllDecks(file, excludeName, result);
            } else if (file.getName().endsWith(".dck")) {
                final Deck deck = DeckSerializer.fromFile(file);
                if (deck != null && !excludeName.equals(deck.getName())) {
                    result.put(deck.getName(), deck);
                }
            }
        }
    }

}
