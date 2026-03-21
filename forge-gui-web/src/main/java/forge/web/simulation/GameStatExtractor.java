package forge.web.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.game.Game;
import forge.game.GameLog;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameOutcome;
import forge.game.player.Player;
import forge.game.player.PlayerStatistics;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;

/**
 * Extracts a SimulationResult from a completed AI-vs-AI game by reading
 * GameOutcome, PlayerStatistics, and GameLog entries.
 */
public final class GameStatExtractor {

    private GameStatExtractor() { }

    /**
     * Extract simulation statistics from a completed match.
     *
     * @param match      the completed HostedMatch
     * @param testPlayer the test deck's RegisteredPlayer
     * @param opponent   the opponent's RegisteredPlayer
     * @param onPlay     whether the test deck was on the play
     * @return a SimulationResult with all extracted statistics
     */
    public static SimulationResult extract(final HostedMatch match,
                                           final RegisteredPlayer testPlayer,
                                           final RegisteredPlayer opponent,
                                           final boolean onPlay) {
        final Game game = match.getGame();
        final GameOutcome outcome = game.getMatch().getOutcomes().iterator().next();

        // Winner
        final boolean won = outcome.isWinner(testPlayer);
        final int turns = outcome.getLastTurnNumber();

        // Mulligan count
        int mulligans = 0;
        for (final Map.Entry<RegisteredPlayer, PlayerStatistics> entry : outcome) {
            if (entry.getKey().equals(testPlayer)) {
                mulligans = entry.getValue().getMulliganCount();
                break;
            }
        }

        // Life totals from Player objects (before cleanup)
        int finalLifeTotal = 20;
        int opponentFinalLife = 20;
        for (final Player p : game.getPlayers()) {
            if (p.getRegisteredPlayer().equals(testPlayer)) {
                finalLifeTotal = p.getLife();
            } else if (p.getRegisteredPlayer().equals(opponent)) {
                opponentFinalLife = p.getLife();
            }
        }

        // Parse GameLog for detailed stats
        final GameLog gameLog = game.getGameLog();
        final List<GameLogEntry> allEntries = gameLog.getLogEntries(null);

        // Determine test player name for log parsing
        String testPlayerName = null;
        for (final Player p : game.getPlayers()) {
            if (p.getRegisteredPlayer().equals(testPlayer)) {
                testPlayerName = p.getName();
                break;
            }
        }

        int cardsDrawn = 0;
        int emptyHandTurns = 0;
        int firstThreatTurn = -1;
        int thirdLandTurn = -1;
        int fourthLandTurn = -1;
        int landCount = 0;
        final Map<String, Integer> cardDrawCounts = new HashMap<>();
        final List<String> cardsInHand = new ArrayList<>();

        // Track current turn for land counting
        int currentTurn = 0;

        // Entries are returned in reverse order (newest first), iterate from end for chronological
        for (int i = allEntries.size() - 1; i >= 0; i--) {
            final GameLogEntry entry = allEntries.get(i);
            final String msg = entry.message();

            // Track turns
            if (entry.type() == GameLogEntryType.TURN) {
                // Turn messages are like "Turn 1 (PlayerName)"
                try {
                    final String turnPart = msg.split("\\(")[0].trim();
                    currentTurn = Integer.parseInt(turnPart.replaceAll("[^0-9]", ""));
                } catch (final NumberFormatException e) {
                    // ignore parse failures
                }
            }

            // Card draws: ZONE_CHANGE from library to hand for test player
            if (entry.type() == GameLogEntryType.ZONE_CHANGE && testPlayerName != null
                    && msg.contains(testPlayerName)
                    && msg.contains("library") && msg.contains("hand")) {
                cardsDrawn++;
                // Try to extract card name from message
                final String cardName = extractCardNameFromZoneChange(msg);
                if (cardName != null) {
                    cardDrawCounts.merge(cardName, 1, Integer::sum);
                }
            }

            // Land drops for test player
            if (entry.type() == GameLogEntryType.LAND && testPlayerName != null
                    && msg.contains(testPlayerName)) {
                landCount++;
                if (landCount == 3 && thirdLandTurn < 0) {
                    thirdLandTurn = currentTurn;
                }
                if (landCount == 4 && fourthLandTurn < 0) {
                    fourthLandTurn = currentTurn;
                }
            }

            // First threat: STACK_ADD for test player (non-land permanent)
            if (entry.type() == GameLogEntryType.STACK_ADD && firstThreatTurn < 0
                    && testPlayerName != null && msg.contains(testPlayerName)) {
                // Any stack add by test player counts as first threat
                firstThreatTurn = currentTurn;
            }
        }

        // For empty hand turns and cards in hand at end, use sentinel values
        // since reliable detection from log text is limited
        // (exact values deferred to future enhancement)
        emptyHandTurns = -1;

        final String opponentDeckName = opponent.getDeck() != null ? opponent.getDeck().getName() : "Unknown";

        return new SimulationResult(
                won, turns, mulligans, onPlay,
                finalLifeTotal, opponentFinalLife,
                cardsDrawn, emptyHandTurns,
                firstThreatTurn, thirdLandTurn, fourthLandTurn,
                cardsInHand, cardDrawCounts,
                opponentDeckName
        );
    }

    /**
     * Attempt to extract a card name from a ZONE_CHANGE log message.
     * Messages typically look like: "CardName moved from Library to Hand"
     * or "PlayerName draws CardName"
     */
    private static String extractCardNameFromZoneChange(final String msg) {
        // Pattern: "X moved from Library to Hand" -- card name is before " moved"
        final int movedIdx = msg.indexOf(" moved from ");
        if (movedIdx > 0) {
            return msg.substring(0, movedIdx).trim();
        }
        // Pattern: "PlayerName draws CardName" -- card name is after "draws "
        final int drawsIdx = msg.indexOf(" draws ");
        if (drawsIdx > 0) {
            return msg.substring(drawsIdx + 7).trim();
        }
        return null;
    }
}
