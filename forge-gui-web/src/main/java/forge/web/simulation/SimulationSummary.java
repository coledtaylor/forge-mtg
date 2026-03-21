package forge.web.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated statistics across all completed simulation games.
 * Computed from a list of SimulationResult instances.
 */
public final class SimulationSummary {

    // Win/loss
    private int totalGames;
    private int wins;
    private int losses;
    private int draws;
    private double winRate;
    private double winRateOnPlay;
    private double winRateOnDraw;

    // Matchups
    private Map<String, MatchupStats> matchups = new HashMap<>();

    // Speed
    private double avgTurns;
    private int fastestWin;
    private int slowestWin;
    private double avgFirstThreatTurn;

    // Mulligan
    private double keepRate;
    private double avgMulligans;
    private double winRateAfterMulligan;

    // Mana
    private double avgThirdLandTurn;
    private double avgFourthLandTurn;
    private double manaScrew;
    private double manaFlood;

    // Resources
    private double avgCardsDrawn;
    private double avgEmptyHandTurns;
    private double avgLifeAtWin;
    private double avgLifeAtLoss;

    // Per-card
    private Map<String, CardPerformance> cardPerformance = new HashMap<>();

    // Rating
    private int eloRating;
    private Map<String, Double> playstyle = new HashMap<>();

    // Progress
    private boolean cancelled;
    private int gamesCompleted;
    private int gamesTotal;

    // ========================================================================
    // Inner classes
    // ========================================================================

    public static final class MatchupStats {
        private final int games;
        private final int wins;
        private final double winRate;

        public MatchupStats(int games, int wins, double winRate) {
            this.games = games;
            this.wins = wins;
            this.winRate = winRate;
        }

        public int getGames() { return games; }
        public int getWins() { return wins; }
        public double getWinRate() { return winRate; }
    }

    public static final class CardPerformance {
        private final int gamesDrawn;
        private final double winRateWhenDrawn;
        private final double deadCardRate;

        public CardPerformance(int gamesDrawn, double winRateWhenDrawn, double deadCardRate) {
            this.gamesDrawn = gamesDrawn;
            this.winRateWhenDrawn = winRateWhenDrawn;
            this.deadCardRate = deadCardRate;
        }

        public int getGamesDrawn() { return gamesDrawn; }
        public double getWinRateWhenDrawn() { return winRateWhenDrawn; }
        public double getDeadCardRate() { return deadCardRate; }
    }

    // ========================================================================
    // Static factory
    // ========================================================================

    public static SimulationSummary computeFrom(final List<SimulationResult> results, final int totalPlanned) {
        final SimulationSummary s = new SimulationSummary();
        s.gamesTotal = totalPlanned;
        s.gamesCompleted = results.size();
        s.totalGames = results.size();

        if (results.isEmpty()) {
            s.fastestWin = -1;
            s.slowestWin = -1;
            s.avgThirdLandTurn = -1.0;
            s.avgFourthLandTurn = -1.0;
            return s;
        }

        // Win/loss counting
        int onPlayGames = 0, onPlayWins = 0;
        int onDrawGames = 0, onDrawWins = 0;
        int mulliganGames = 0, mulliganWins = 0;
        int totalMulligans = 0;
        int totalTurns = 0;
        int fastWin = Integer.MAX_VALUE, slowWin = Integer.MIN_VALUE;
        int threatTurnSum = 0, threatTurnCount = 0;
        int thirdLandSum = 0, thirdLandCount = 0;
        int fourthLandSum = 0, fourthLandCount = 0;
        int manaScrew = 0, manaFlood = 0;
        int totalCardsDrawn = 0, totalEmptyHandTurns = 0;
        int lifeAtWinSum = 0, winCount = 0;
        int lifeAtLossSum = 0, lossCount = 0;

        // Per-opponent tracking
        final Map<String, List<SimulationResult>> byOpponent = new HashMap<>();

        // Per-card tracking
        final Map<String, Integer> cardDrawnGames = new HashMap<>();
        final Map<String, Integer> cardDrawnWins = new HashMap<>();
        final Map<String, Integer> cardStuckCount = new HashMap<>();
        final Map<String, Integer> cardTotalGames = new HashMap<>();

        for (final SimulationResult r : results) {
            // Win/loss
            if (r.isWon()) {
                s.wins++;
                winCount++;
                lifeAtWinSum += r.getFinalLifeTotal();
                if (r.getTurns() < fastWin) fastWin = r.getTurns();
                if (r.getTurns() > slowWin) slowWin = r.getTurns();
            } else {
                s.losses++;
                lossCount++;
                lifeAtLossSum += r.getFinalLifeTotal();
            }

            // Play/draw
            if (r.isOnPlay()) {
                onPlayGames++;
                if (r.isWon()) onPlayWins++;
            } else {
                onDrawGames++;
                if (r.isWon()) onDrawWins++;
            }

            // Mulligans
            totalMulligans += r.getMulligans();
            if (r.getMulligans() > 0) {
                mulliganGames++;
                if (r.isWon()) mulliganWins++;
            }

            // Speed
            totalTurns += r.getTurns();
            if (r.getFirstThreatTurn() > 0) {
                threatTurnSum += r.getFirstThreatTurn();
                threatTurnCount++;
            }

            // Land drops
            if (r.getThirdLandTurn() > 0) {
                thirdLandSum += r.getThirdLandTurn();
                thirdLandCount++;
            }
            if (r.getFourthLandTurn() > 0) {
                fourthLandSum += r.getFourthLandTurn();
                fourthLandCount++;
            }

            // Mana screw: < 3 lands by turn 4 (only count games that lasted at least 4 turns)
            if (r.getTurns() >= 4) {
                if (r.getThirdLandTurn() < 0 || r.getThirdLandTurn() > 4) {
                    manaScrew++;
                }
            }
            // Mana flood: game went > 10 turns and test deck lost
            if (r.getTurns() > 10 && !r.isWon()) {
                manaFlood++;
            }

            // Resources
            totalCardsDrawn += r.getCardsDrawn();
            // Sentinel -1 means "not tracked" — treat as 0
            if (r.getEmptyHandTurns() >= 0) {
                totalEmptyHandTurns += r.getEmptyHandTurns();
            }

            // Per-opponent
            byOpponent.computeIfAbsent(r.getOpponentDeckName(), k -> new ArrayList<>()).add(r);

            // Per-card draw tracking
            for (final Map.Entry<String, Integer> entry : r.getCardDrawCounts().entrySet()) {
                final String card = entry.getKey();
                cardDrawnGames.merge(card, 1, Integer::sum);
                if (r.isWon()) {
                    cardDrawnWins.merge(card, 1, Integer::sum);
                }
            }

            // Cards stuck in hand at end
            for (final String card : r.getCardsInHand()) {
                cardStuckCount.merge(card, 1, Integer::sum);
                cardTotalGames.merge(card, 1, Integer::sum);
            }
        }

        // Aggregate (all rates stored as 0-100 percentages for direct display)
        s.winRate = s.totalGames > 0 ? 100.0 * s.wins / s.totalGames : 0;
        s.winRateOnPlay = onPlayGames > 0 ? 100.0 * onPlayWins / onPlayGames : 0;
        s.winRateOnDraw = onDrawGames > 0 ? 100.0 * onDrawWins / onDrawGames : 0;

        s.avgTurns = (double) totalTurns / s.totalGames;
        s.fastestWin = fastWin == Integer.MAX_VALUE ? -1 : fastWin;
        s.slowestWin = slowWin == Integer.MIN_VALUE ? -1 : slowWin;
        s.avgFirstThreatTurn = threatTurnCount > 0 ? (double) threatTurnSum / threatTurnCount : -1;

        s.keepRate = s.totalGames > 0 ? 100.0 * (s.totalGames - mulliganGames) / s.totalGames : 100.0;
        s.avgMulligans = s.totalGames > 0 ? (double) totalMulligans / s.totalGames : 0;
        s.winRateAfterMulligan = mulliganGames > 0 ? 100.0 * mulliganWins / mulliganGames : 0;

        s.avgThirdLandTurn = thirdLandCount > 0 ? (double) thirdLandSum / thirdLandCount : -1.0;
        s.avgFourthLandTurn = fourthLandCount > 0 ? (double) fourthLandSum / fourthLandCount : -1.0;
        s.manaScrew = 100.0 * manaScrew / s.totalGames;
        s.manaFlood = 100.0 * manaFlood / s.totalGames;

        s.avgCardsDrawn = (double) totalCardsDrawn / s.totalGames;
        s.avgEmptyHandTurns = (double) totalEmptyHandTurns / s.totalGames;
        s.avgLifeAtWin = winCount > 0 ? (double) lifeAtWinSum / winCount : 0;
        s.avgLifeAtLoss = lossCount > 0 ? (double) lifeAtLossSum / lossCount : 0;

        // Per-opponent matchups
        for (final Map.Entry<String, List<SimulationResult>> entry : byOpponent.entrySet()) {
            final List<SimulationResult> oppResults = entry.getValue();
            int oppWins = 0;
            for (final SimulationResult r : oppResults) {
                if (r.isWon()) oppWins++;
            }
            double oppWinRate = oppResults.isEmpty() ? 0 : 100.0 * oppWins / oppResults.size();
            s.matchups.put(entry.getKey(), new MatchupStats(oppResults.size(), oppWins, oppWinRate));
        }

        // Per-card performance
        for (final String card : cardDrawnGames.keySet()) {
            int drawn = cardDrawnGames.getOrDefault(card, 0);
            int drawnWins = cardDrawnWins.getOrDefault(card, 0);
            int stuck = cardStuckCount.getOrDefault(card, 0);
            double cardWinRate = drawn > 0 ? 100.0 * drawnWins / drawn : 0;
            double deadRate = drawn > 0 ? 100.0 * stuck / drawn : 0;
            s.cardPerformance.put(card, new CardPerformance(drawn, cardWinRate, deadRate));
        }

        // Playstyle heuristics (0.0 to 1.0 scores)
        // Aggro: fast wins, low avg turns, early threats
        double aggroScore = 0;
        if (s.avgTurns > 0) {
            // Normalize: 5 turns = max aggro (1.0), 15+ turns = no aggro (0.0)
            aggroScore = Math.max(0, Math.min(1, (15.0 - s.avgTurns) / 10.0));
        }
        // Boost aggro if first threat is very early
        if (s.avgFirstThreatTurn > 0 && s.avgFirstThreatTurn <= 3) {
            aggroScore = Math.min(1, aggroScore + 0.15);
        }

        // Control: long games, high life differential at win, mana flood resilience
        double controlScore = 0;
        if (s.avgTurns > 0) {
            // Normalize: 12+ turns = max control (1.0), 5 turns = no control (0.0)
            controlScore = Math.max(0, Math.min(1, (s.avgTurns - 5.0) / 7.0));
        }
        // Boost control if winning with high life
        if (winCount > 0 && s.avgLifeAtWin > 15) {
            controlScore = Math.min(1, controlScore + 0.1);
        }

        // Midrange: moderate speed, balanced play/draw win rates
        double midrangeScore = 0;
        if (s.avgTurns >= 6 && s.avgTurns <= 12) {
            // Peak midrange around turn 8-9
            midrangeScore = 1.0 - Math.abs(s.avgTurns - 8.5) / 4.0;
            midrangeScore = Math.max(0, Math.min(1, midrangeScore));
        }
        // Boost if play/draw win rates are close (adaptable)
        if (onPlayGames > 0 && onDrawGames > 0) {
            double playDrawDiff = Math.abs(s.winRateOnPlay - s.winRateOnDraw);
            if (playDrawDiff < 15) { // Less than 15% difference
                midrangeScore = Math.min(1, midrangeScore + 0.15);
            }
        }

        // Combo: fast wins with big variance (fastest win much faster than average)
        double comboScore = 0;
        if (s.fastestWin > 0 && s.avgTurns > 0) {
            double winSpread = s.avgTurns - s.fastestWin;
            // Big spread between fastest and average suggests combo potential
            comboScore = Math.max(0, Math.min(1, winSpread / 6.0));
        }
        // Boost combo if very fast fastest win
        if (s.fastestWin > 0 && s.fastestWin <= 4) {
            comboScore = Math.min(1, comboScore + 0.2);
        }

        s.playstyle.put("aggro", Math.round(aggroScore * 100.0) / 100.0);
        s.playstyle.put("midrange", Math.round(midrangeScore * 100.0) / 100.0);
        s.playstyle.put("control", Math.round(controlScore * 100.0) / 100.0);
        s.playstyle.put("combo", Math.round(comboScore * 100.0) / 100.0);

        // Elo
        final List<EloCalculator.EloResult> eloResults = new ArrayList<>();
        for (final SimulationResult r : results) {
            eloResults.add(new EloCalculator.EloResult(1500, r.isWon()));
        }
        s.eloRating = EloCalculator.computeElo(eloResults);

        return s;
    }

    // ========================================================================
    // Getters
    // ========================================================================

    public int getTotalGames() { return totalGames; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getDraws() { return draws; }
    public double getWinRate() { return winRate; }
    public double getWinRateOnPlay() { return winRateOnPlay; }
    public double getWinRateOnDraw() { return winRateOnDraw; }
    public Map<String, MatchupStats> getMatchups() { return matchups; }
    public double getAvgTurns() { return avgTurns; }
    public int getFastestWin() { return fastestWin; }
    public int getSlowestWin() { return slowestWin; }
    public double getAvgFirstThreatTurn() { return avgFirstThreatTurn; }
    public double getKeepRate() { return keepRate; }
    public double getAvgMulligans() { return avgMulligans; }
    public double getWinRateAfterMulligan() { return winRateAfterMulligan; }
    public double getAvgThirdLandTurn() { return avgThirdLandTurn; }
    public double getAvgFourthLandTurn() { return avgFourthLandTurn; }
    public double getManaScrew() { return manaScrew; }
    public double getManaFlood() { return manaFlood; }
    public double getAvgCardsDrawn() { return avgCardsDrawn; }
    public double getAvgEmptyHandTurns() { return avgEmptyHandTurns; }
    public double getAvgLifeAtWin() { return avgLifeAtWin; }
    public double getAvgLifeAtLoss() { return avgLifeAtLoss; }
    public Map<String, CardPerformance> getCardPerformance() { return cardPerformance; }
    public int getEloRating() { return eloRating; }
    public Map<String, Double> getPlaystyle() { return playstyle; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public int getGamesCompleted() { return gamesCompleted; }
    public int getGamesTotal() { return gamesTotal; }
}
