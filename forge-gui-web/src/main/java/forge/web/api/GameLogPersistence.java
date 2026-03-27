package forge.web.api;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.tinylog.Logger;

import forge.game.Game;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameOutcome;
import forge.game.player.RegisteredPlayer;
import forge.localinstance.properties.ForgeConstants;
import forge.web.simulation.SimulationResult;

/**
 * Persists raw game logs as individual JSON files for debugging and analysis.
 * Each game produces one file in the gamelogs/ directory.
 * Optionally includes per-game SimulationResult stats for recalculation.
 */
public final class GameLogPersistence {

    private static final ObjectMapper JSON = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    static final File GAMELOGS_DIR = new File(ForgeConstants.DECK_CONSTRUCTED_DIR, "../gamelogs");

    private GameLogPersistence() { }

    /**
     * Persist the raw game log from a completed game.
     *
     * @param game           the completed Game object
     * @param playerDeckName display name of the player/test deck
     * @param opponentDeckName display name of the opponent deck
     * @param testPlayer     the test/player RegisteredPlayer (to determine winner)
     * @param source         "simulation" or "match"
     * @param simulationId   the simulation job ID (null for non-simulation games)
     * @param onPlay         whether the test player went first
     * @param simResult      the SimulationResult for this game (null for non-simulation games)
     * @return the generated log ID, or null if persistence failed
     */
    public static String persistGameLog(final Game game,
                                         final String playerDeckName,
                                         final String opponentDeckName,
                                         final RegisteredPlayer testPlayer,
                                         final String source,
                                         final String simulationId,
                                         final boolean onPlay,
                                         final SimulationResult simResult) {
        final File dir = GAMELOGS_DIR.getAbsoluteFile();
        dir.mkdirs();

        final String logId = UUID.randomUUID().toString();
        final String timestamp = Instant.now().toString();

        // Determine winner and turn count from game outcome
        final GameOutcome outcome = game.getMatch().getOutcomes().iterator().next();
        final boolean testWon = outcome.isWinner(testPlayer);
        final int turns = outcome.getLastTurnNumber();
        final String winnerName = testWon ? playerDeckName : opponentDeckName;

        // Extract log entries in chronological order
        final List<GameLogEntry> rawEntries = game.getGameLog().getLogEntries(null);
        final List<Map<String, Object>> entries = new ArrayList<>(rawEntries.size());

        // getLogEntries(null) returns newest-first; reverse for chronological
        int currentTurn = 0;
        for (int i = rawEntries.size() - 1; i >= 0; i--) {
            final GameLogEntry entry = rawEntries.get(i);

            // Track turn number
            if (entry.type() == GameLogEntryType.TURN) {
                try {
                    final String turnPart = entry.message().split("\\(")[0].trim();
                    currentTurn = Integer.parseInt(turnPart.replaceAll("[^0-9]", ""));
                } catch (final NumberFormatException e) {
                    // keep previous turn number
                }
            }

            final Map<String, Object> logEntry = new LinkedHashMap<>();
            logEntry.put("turn", currentTurn);
            logEntry.put("type", entry.type().name());
            logEntry.put("message", entry.message());
            entries.add(logEntry);
        }

        // Build the full log document
        final Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", logId);
        doc.put("timestamp", timestamp);
        doc.put("source", source);
        if (simulationId != null) {
            doc.put("simulationId", simulationId);
        }
        doc.put("playerDeck", playerDeckName);
        doc.put("opponentDeck", opponentDeckName);
        doc.put("winner", winnerName);
        doc.put("turns", turns);
        doc.put("onPlay", onPlay);

        // Per-game stats for recalculation (simulation games only)
        if (simResult != null) {
            final Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("won", simResult.isWon());
            stats.put("stalemate", simResult.isStalemate());
            stats.put("turns", simResult.getTurns());
            stats.put("mulligans", simResult.getMulligans());
            stats.put("onPlay", simResult.isOnPlay());
            stats.put("finalLifeTotal", simResult.getFinalLifeTotal());
            stats.put("opponentFinalLife", simResult.getOpponentFinalLife());
            stats.put("cardsDrawn", simResult.getCardsDrawn());
            stats.put("emptyHandTurns", simResult.getEmptyHandTurns());
            stats.put("firstThreatTurn", simResult.getFirstThreatTurn());
            stats.put("thirdLandTurn", simResult.getThirdLandTurn());
            stats.put("fourthLandTurn", simResult.getFourthLandTurn());
            stats.put("totalLandsPlayed", simResult.getTotalLandsPlayed());
            stats.put("cardsInHand", simResult.getCardsInHand());
            stats.put("cardDrawCounts", simResult.getCardDrawCounts());
            stats.put("opponentDeckName", simResult.getOpponentDeckName());
            doc.put("stats", stats);
        }

        doc.put("entries", entries);

        // Write to file
        final String filename = "gamelog-" + logId + ".json";
        final File outFile = new File(dir, filename);

        try {
            JSON.writerWithDefaultPrettyPrinter().writeValue(outFile, doc);
            Logger.info("Saved game log: {} ({} entries, {} turns)", filename, entries.size(), turns);
            return logId;
        } catch (final IOException e) {
            Logger.error(e, "Failed to save game log: {}", filename);
            return null;
        }
    }
}
