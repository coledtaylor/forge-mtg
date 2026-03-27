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
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerStatistics;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
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

        // Life totals from Player objects
        // Use getRegisteredPlayers() (all players) instead of getPlayers() (only surviving players)
        // because the loser is removed from ingamePlayers at game end
        int finalLifeTotal = 20;
        int opponentFinalLife = 20;
        for (final Player p : game.getRegisteredPlayers()) {
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
        for (final Player p : game.getRegisteredPlayers()) {
            if (p.getRegisteredPlayer().equals(testPlayer)) {
                testPlayerName = p.getName();
                break;
            }
        }

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

        // Extract resource stats from Player game objects directly
        // (log parsing cannot capture Library->Hand zone changes)
        int cardsDrawn = 0;
        int emptyHandTurns = 0;
        for (final Player p : game.getRegisteredPlayers()) {
            if (p.getRegisteredPlayer().equals(testPlayer)) {
                // Cards drawn = cards that left the library (initial library - remaining)
                // Initial library = deck size - opening hand size (7 minus mulligans)
                final int deckSize = testPlayer.getDeck().getMain().countAll();
                final int openingHand = 7 - mulligans;
                final int initialLibrary = deckSize - openingHand;
                final int remainingLibrary = p.getZone(ZoneType.Library).size();
                cardsDrawn = Math.max(0, initialLibrary - remainingLibrary);

                // Cards currently in hand at end of game (these are "dead" / unplayed)
                for (final Card c : p.getZone(ZoneType.Hand)) {
                    cardsInHand.add(c.getName());
                }

                // Track all cards the player saw during the game (all non-library zones)
                // This includes cards in hand, battlefield, graveyard, and exile
                for (final Card c : p.getZone(ZoneType.Hand)) {
                    cardDrawCounts.merge(c.getName(), 1, Integer::sum);
                }
                for (final Card c : p.getZone(ZoneType.Battlefield)) {
                    if (c.getOwner().equals(p)) {
                        cardDrawCounts.merge(c.getName(), 1, Integer::sum);
                    }
                }
                for (final Card c : p.getZone(ZoneType.Graveyard)) {
                    if (c.getOwner().equals(p)) {
                        cardDrawCounts.merge(c.getName(), 1, Integer::sum);
                    }
                }
                for (final Card c : p.getZone(ZoneType.Exile)) {
                    if (c.getOwner().equals(p)) {
                        cardDrawCounts.merge(c.getName(), 1, Integer::sum);
                    }
                }

                // Empty hand turns: approximation not available from end-state
                // Use 0 as default (tracked stat not yet available in engine)
                emptyHandTurns = 0;
                break;
            }
        }

        final String opponentDeckName = opponent.getDeck() != null ? opponent.getDeck().getName() : "Unknown";

        return new SimulationResult(
                won, false, turns, mulligans, onPlay,
                finalLifeTotal, opponentFinalLife,
                cardsDrawn, emptyHandTurns,
                firstThreatTurn, thirdLandTurn, fourthLandTurn,
                landCount,
                cardsInHand, cardDrawCounts,
                opponentDeckName
        );
    }

}
