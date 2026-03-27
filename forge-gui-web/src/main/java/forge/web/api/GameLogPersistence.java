package forge.web.api;

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
import forge.web.WebServer;
import forge.web.simulation.SimulationResult;

/**
 * Persists raw game logs to the SQLite simulation database.
 * Each game produces one row in the game_logs table.
 * Optionally includes per-game SimulationResult stats for recalculation.
 */
public final class GameLogPersistence {

    private static final ObjectMapper JSON = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * Legacy directory reference retained for SimulationHandler compatibility.
     * Will be removed in Phase 2 Task 2 when SimulationHandler migrates to SQLite.
     * @deprecated use SimulationDatabase instead
     */
    @Deprecated
    static final java.io.File GAMELOGS_DIR = new java.io.File(ForgeConstants.DECK_CONSTRUCTED_DIR, "../gamelogs");

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

        // Serialize entries list to JSON string
        String entriesJson = null;
        try {
            entriesJson = JSON.writeValueAsString(entries);
        } catch (final IOException e) {
            Logger.warn("Failed to serialize game log entries for logId={}", logId);
        }

        // Extract stats fields (simulation games only)
        boolean won = false;
        boolean stalemate = false;
        int mulligans = 0;
        int finalLife = 0;
        int opponentFinalLife = 0;
        int cardsDrawn = 0;
        int emptyHandTurns = 0;
        int firstThreatTurn = 0;
        int thirdLandTurn = 0;
        int fourthLandTurn = 0;
        int totalLandsPlayed = 0;
        String cardDrawCountsJson = null;
        String cardsInHandJson = null;

        if (simResult != null) {
            won = simResult.isWon();
            stalemate = simResult.isStalemate();
            mulligans = simResult.getMulligans();
            finalLife = simResult.getFinalLifeTotal();
            opponentFinalLife = simResult.getOpponentFinalLife();
            cardsDrawn = simResult.getCardsDrawn();
            emptyHandTurns = simResult.getEmptyHandTurns();
            firstThreatTurn = simResult.getFirstThreatTurn();
            thirdLandTurn = simResult.getThirdLandTurn();
            fourthLandTurn = simResult.getFourthLandTurn();
            totalLandsPlayed = simResult.getTotalLandsPlayed();

            try {
                cardDrawCountsJson = JSON.writeValueAsString(simResult.getCardDrawCounts());
            } catch (final IOException e) {
                Logger.warn("Failed to serialize cardDrawCounts for logId={}", logId);
            }

            try {
                cardsInHandJson = JSON.writeValueAsString(simResult.getCardsInHand());
            } catch (final IOException e) {
                Logger.warn("Failed to serialize cardsInHand for logId={}", logId);
            }
        }

        // Write to database
        final SimulationDatabase db = WebServer.getDatabase();
        if (db == null) {
            Logger.error("SimulationDatabase is not initialized; cannot persist game log {}", logId);
            return null;
        }

        db.insertGameLog(
                logId, simulationId, timestamp, source,
                playerDeckName, opponentDeckName, winnerName,
                turns, onPlay,
                won, stalemate, mulligans,
                finalLife, opponentFinalLife, cardsDrawn,
                emptyHandTurns, firstThreatTurn,
                thirdLandTurn, fourthLandTurn, totalLandsPlayed,
                entriesJson, cardDrawCountsJson, cardsInHandJson);

        Logger.info("Saved game log: {} ({} entries, {} turns)", logId, entries.size(), turns);
        return logId;
    }
}
