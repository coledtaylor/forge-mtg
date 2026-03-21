package forge.web.simulation;

import java.util.List;

/**
 * Standard Elo rating computation. Calculates a deck's Elo rating
 * from a sequence of win/loss results against opponents all assumed
 * to be 1500 Elo (per-run, not cumulative).
 */
public final class EloCalculator {

    private static final int START_ELO = 1500;
    private static final int K_FACTOR = 32;

    private EloCalculator() { }

    /**
     * A single game result for Elo computation.
     */
    public static final class EloResult {
        private final int opponentElo;
        private final boolean won;

        public EloResult(int opponentElo, boolean won) {
            this.opponentElo = opponentElo;
            this.won = won;
        }

        public int getOpponentElo() { return opponentElo; }
        public boolean isWon() { return won; }
    }

    /**
     * Compute Elo rating from a sequence of game results.
     *
     * @param results ordered list of game results
     * @return final Elo rating (rounded to nearest integer)
     */
    public static int computeElo(final List<EloResult> results) {
        double elo = START_ELO;
        for (final EloResult result : results) {
            double expected = 1.0 / (1.0 + Math.pow(10.0, (result.opponentElo - elo) / 400.0));
            double actual = result.won ? 1.0 : 0.0;
            elo += K_FACTOR * (actual - expected);
        }
        return (int) Math.round(elo);
    }
}
