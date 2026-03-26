package forge.web.api;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.javalin.http.Context;

import org.tinylog.Logger;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.localinstance.properties.ForgeConstants;
import forge.web.simulation.SimulationJob;
import forge.web.simulation.SimulationRunner;
import forge.web.simulation.SimulationSummary;

/**
 * REST handler for simulation CRUD operations.
 * Manages starting simulations, querying status/history, cancellation,
 * and persisting results as JSON files alongside deck files.
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
    public static void status(final Context ctx) {
        final String id = ctx.pathParam("id");
        final SimulationJob job = SimulationRunner.getJob(id);

        if (job != null) {
            final SimulationSummary summary = job.getProgress();
            ctx.json(wrapStatus(summary, job.isRunning(), job.isComplete(), job.isCancelled()));
            return;
        }

        // Check for saved result
        final File resultFile = findResultFile(id);
        if (resultFile != null) {
            try {
                final Map<String, Object> saved = JSON.readValue(resultFile, Map.class);
                ctx.json(saved);
                return;
            } catch (final IOException e) {
                Logger.warn("Failed to read saved result {}: {}", resultFile.getName(), e.getMessage());
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
    public static void history(final Context ctx) {
        final String deckName = ctx.pathParam("deckName");
        final File decksDir = new File(ForgeConstants.DECK_CONSTRUCTED_DIR);
        final List<Map<String, Object>> entries = new ArrayList<>();

        if (decksDir.exists()) {
            final String prefix = "sim-" + sanitizeFilename(deckName) + "-";
            collectSimFiles(decksDir, prefix, entries);
        }

        // Sort by timestamp descending
        entries.sort(Comparator.<Map<String, Object>, String>comparing(
                e -> (String) e.get("timestamp")).reversed());

        ctx.json(entries);
    }

    /**
     * DELETE /api/simulations/{id}
     */
    public static void deleteResult(final Context ctx) {
        final String id = ctx.pathParam("id");
        final File resultFile = findResultFile(id);

        if (resultFile != null && resultFile.exists()) {
            resultFile.delete();
            ctx.status(204);
        } else {
            ctx.status(404).json(Map.of("error", "Result not found"));
        }
    }

    /**
     * POST /api/simulations/{id}/recalculate
     * Reconstructs SimulationResults from saved game logs and recomputes the summary.
     */
    @SuppressWarnings("unchecked")
    public static void recalculate(final Context ctx) {
        final String id = ctx.pathParam("id");

        // Find the existing result file to get deckName and totalGames
        final File resultFile = findResultFile(id);
        if (resultFile == null) {
            ctx.status(404).json(Map.of("error", "Simulation result not found"));
            return;
        }

        String deckName;
        int totalGames;
        try {
            final Map<String, Object> existing = JSON.readValue(resultFile, Map.class);
            deckName = (String) existing.get("deckName");
            totalGames = ((Number) existing.getOrDefault("gamesTotal", 0)).intValue();
        } catch (final IOException e) {
            ctx.status(500).json(Map.of("error", "Failed to read existing result"));
            return;
        }

        // Load game logs for this simulation
        final File logsDir = GameLogPersistence.GAMELOGS_DIR.getAbsoluteFile();
        if (!logsDir.exists()) {
            ctx.status(404).json(Map.of("error", "No game logs found"));
            return;
        }

        final File[] logFiles = logsDir.listFiles((d, name) -> name.startsWith("gamelog-") && name.endsWith(".json"));
        if (logFiles == null || logFiles.length == 0) {
            ctx.status(404).json(Map.of("error", "No game logs found"));
            return;
        }

        // Reconstruct SimulationResults from game log stats
        final List<forge.web.simulation.SimulationResult> results = new ArrayList<>();
        for (final File logFile : logFiles) {
            try {
                final Map<String, Object> log = JSON.readValue(logFile, Map.class);
                if (!id.equals(log.get("simulationId"))) {
                    continue;
                }

                final Map<String, Object> stats = (Map<String, Object>) log.get("stats");
                if (stats == null) {
                    Logger.warn("Game log {} has no stats, skipping", logFile.getName());
                    continue;
                }

                final boolean won = Boolean.TRUE.equals(stats.get("won"));
                final boolean stalemate = Boolean.TRUE.equals(stats.get("stalemate"));
                final int turns = ((Number) stats.get("turns")).intValue();
                final int mulligans = ((Number) stats.get("mulligans")).intValue();
                final boolean onPlay = Boolean.TRUE.equals(stats.get("onPlay"));
                final int finalLife = ((Number) stats.get("finalLifeTotal")).intValue();
                final int oppLife = ((Number) stats.get("opponentFinalLife")).intValue();
                final int cardsDrawn = ((Number) stats.get("cardsDrawn")).intValue();
                final int emptyHandTurns = ((Number) stats.get("emptyHandTurns")).intValue();
                final int firstThreatTurn = ((Number) stats.get("firstThreatTurn")).intValue();
                final int thirdLandTurn = ((Number) stats.get("thirdLandTurn")).intValue();
                final int fourthLandTurn = ((Number) stats.get("fourthLandTurn")).intValue();
                final List<String> cardsInHand = (List<String>) stats.getOrDefault("cardsInHand", List.of());
                final Map<String, Object> rawDrawCounts = (Map<String, Object>) stats.getOrDefault("cardDrawCounts", Map.of());
                final Map<String, Integer> cardDrawCounts = new java.util.HashMap<>();
                for (final Map.Entry<String, Object> e : rawDrawCounts.entrySet()) {
                    cardDrawCounts.put(e.getKey(), ((Number) e.getValue()).intValue());
                }
                final String oppDeckName = (String) stats.get("opponentDeckName");

                results.add(new forge.web.simulation.SimulationResult(
                        won, stalemate, turns, mulligans, onPlay,
                        finalLife, oppLife, cardsDrawn, emptyHandTurns,
                        firstThreatTurn, thirdLandTurn, fourthLandTurn,
                        cardsInHand, cardDrawCounts, oppDeckName
                ));
            } catch (final Exception e) {
                Logger.warn("Failed to parse game log {}: {}", logFile.getName(), e.getMessage());
            }
        }

        if (results.isEmpty()) {
            ctx.status(404).json(Map.of("error", "No game logs with stats found for this simulation"));
            return;
        }

        // Recompute summary
        final forge.web.simulation.SimulationSummary summary =
                forge.web.simulation.SimulationSummary.computeFrom(results, totalGames);

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
     * Persist a simulation result as a JSON file.
     */
    private static void persistResult(final String deckName, final String jobId,
                                       final SimulationSummary summary) {
        final File decksDir = new File(ForgeConstants.DECK_CONSTRUCTED_DIR);
        decksDir.mkdirs();

        final String timestamp = Instant.now().toString();
        final String filename = "sim-" + sanitizeFilename(deckName) + "-" + jobId + ".json";
        final File outFile = new File(decksDir, filename);

        try {
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", jobId);
            data.put("deckName", deckName);
            data.put("timestamp", timestamp);
            // Serialize the full summary
            final Map<String, Object> summaryMap = JSON.convertValue(summary, Map.class);
            data.putAll(summaryMap);
            JSON.writerWithDefaultPrettyPrinter().writeValue(outFile, data);
            Logger.info("Saved simulation result: {}", filename);
        } catch (final IOException e) {
            Logger.error(e, "Failed to save simulation result: {}", filename);
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

    private static void collectSimFiles(final File dir, final String prefix,
                                          final List<Map<String, Object>> entries) {
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                collectSimFiles(file, prefix, entries);
                continue;
            }
            if (file.getName().startsWith(prefix) && file.getName().endsWith(".json")) {
                try {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> full = JSON.readValue(file, Map.class);
                    // Return summary entry only
                    final Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", full.get("id"));
                    entry.put("timestamp", full.get("timestamp"));
                    entry.put("gamesCompleted", full.get("gamesCompleted"));
                    entry.put("gamesTotal", full.get("gamesTotal"));
                    entry.put("winRate", full.get("winRate"));
                    entry.put("eloRating", full.get("eloRating"));
                    entries.add(entry);
                } catch (final IOException e) {
                    Logger.warn("Failed to parse sim file {}: {}", file.getName(), e.getMessage());
                }
            }
        }
    }

    private static File findResultFile(final String id) {
        final File decksDir = new File(ForgeConstants.DECK_CONSTRUCTED_DIR);
        if (!decksDir.exists()) {
            return null;
        }
        return scanForResultFile(decksDir, id);
    }

    private static File scanForResultFile(final File dir, final String id) {
        final File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                final File found = scanForResultFile(file, id);
                if (found != null) {
                    return found;
                }
            } else if (file.getName().contains(id) && file.getName().endsWith(".json")) {
                return file;
            }
        }
        return null;
    }

    private static String sanitizeFilename(final String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
