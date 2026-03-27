package forge.web.simulation;

/**
 * Wilson score interval calculator for deck strength rating.
 * Produces a statistically sound lower-bound confidence score (0-100)
 * with tier labels. This replaces the previous Elo-based rating.
 *
 * Formula: lower = (p + z^2/2n - z*sqrt(p(1-p)/n + z^2/4n^2)) / (1 + z^2/n)
 * where p = wins/total, z = 1.96 (95% confidence), n = total games.
 * Power Score = round(lower * 100).
 */
public final class WilsonCalculator {

    private static final double Z = 1.96;
    private static final double Z2 = Z * Z;

    private WilsonCalculator() { }

    /**
     * Result of a Wilson score interval computation.
     */
    public static final class WilsonResult {
        private final int powerScore;
        private final double confidenceLower;
        private final double confidenceUpper;
        private final String tier;

        public WilsonResult(int powerScore, double confidenceLower, double confidenceUpper, String tier) {
            this.powerScore = powerScore;
            this.confidenceLower = confidenceLower;
            this.confidenceUpper = confidenceUpper;
            this.tier = tier;
        }

        public int getPowerScore() { return powerScore; }
        public double getConfidenceLower() { return confidenceLower; }
        public double getConfidenceUpper() { return confidenceUpper; }
        public String getTier() { return tier; }
    }

    /**
     * Compute the Wilson score interval for a win/loss record.
     *
     * @param wins  number of wins
     * @param total total non-stalemate games played
     * @return a WilsonResult with powerScore, confidence bounds, and tier
     */
    public static WilsonResult compute(final int wins, final int total) {
        if (total == 0) {
            return new WilsonResult(0, 0.0, 0.0, "D Weak");
        }

        final double p = (double) wins / total;
        final double n = total;

        final double center = p + Z2 / (2 * n);
        final double margin = Z * Math.sqrt(p * (1 - p) / n + Z2 / (4 * n * n));
        final double denominator = 1 + Z2 / n;

        final double lower = Math.max(0.0, (center - margin) / denominator);
        final double upper = Math.min(1.0, (center + margin) / denominator);

        final int powerScore = (int) Math.round(lower * 100);
        final String tier = tierFor(powerScore);

        return new WilsonResult(powerScore, lower, upper, tier);
    }

    /**
     * Map a power score (0-100) to a tier label.
     *
     * @param powerScore the Wilson lower-bound score scaled to 0-100
     * @return tier label string
     */
    public static String tierFor(final int powerScore) {
        if (powerScore >= 75) return "S+ Elite";
        if (powerScore >= 65) return "S Strong";
        if (powerScore >= 55) return "A Above Average";
        if (powerScore >= 45) return "B Average";
        if (powerScore >= 30) return "C Below Average";
        return "D Weak";
    }
}
