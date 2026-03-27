package forge.web.simulation;

import java.util.Map;

import forge.card.CardRules;
import forge.card.CardType;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;

/**
 * Derives a {@link ManaProfile} from a deck's mana curve.
 *
 * <p>All computation is stateless and purely functional; use the static
 * {@link #analyze(Deck)} entry point.</p>
 */
public final class ManaCurveAnalyzer {

    private ManaCurveAnalyzer() { }

    /**
     * Analyze a deck and return its mana profile.
     *
     * @param deck the deck to analyze; must not be {@code null}
     * @return a populated {@link ManaProfile}
     */
    public static ManaProfile analyze(final Deck deck) {
        final CardPool main = deck.getMain();
        if (main == null || main.isEmpty()) {
            return defaultProfile();
        }

        int landCount = 0;
        int totalNonLand = 0;
        double totalCmc = 0.0;

        for (final Map.Entry<PaperCard, Integer> entry : main) {
            final PaperCard card = entry.getKey();
            final int copies = entry.getValue();
            final CardRules rules = card.getRules();
            if (rules == null) {
                continue;
            }
            final CardType type = rules.getType();
            if (type.isLand()) {
                landCount += copies;
            } else {
                totalNonLand += copies;
                totalCmc += rules.getManaCost().getCMC() * copies;
            }
        }

        final int deckSize = landCount + totalNonLand;
        final double avgCmc = totalNonLand > 0 ? totalCmc / totalNonLand : 2.0;

        // Classify archetype to name it in the profile
        final DeckArchetype archetype = DeckArchetypeClassifier.classify(deck);
        final String archetypeName = archetype.name().toLowerCase();

        // Karsten formula: recommended land count (rounded to 1 dp)
        final double recommendedLands = Math.round((19.59 + 1.90 * avgCmc) * 10.0) / 10.0;

        // Key turn: turn by which the deck needs its critical mana online
        final int keyTurn;
        if (avgCmc <= 2.0) {
            keyTurn = 3;
        } else if (avgCmc <= 3.0) {
            keyTurn = 4;
        } else {
            keyTurn = 5;
        }

        // Lands needed by key turn (capped at actual land count)
        final int rawNeeded = Math.min(keyTurn, (int) Math.ceil(avgCmc) + 1);
        final int landsNeededByKeyTurn = Math.min(rawNeeded, Math.max(1, landCount));

        // Excess threshold: lands beyond which extras are dead draws that turn
        final int landExcessThreshold = landsNeededByKeyTurn + 2;

        // Hypergeometric probabilities
        // Opening hand = 7 cards, then +1 per turn => 7 + keyTurn cards seen by key turn
        final int drawsForScrew = 7 + keyTurn;
        // Give 3 extra turns of draws for flood assessment
        final int drawsForFlood = 7 + keyTurn + 3;

        final double screwProbability = hypergeometricPLess(
                deckSize, landCount, drawsForScrew, landsNeededByKeyTurn);
        final double floodProbability = hypergeometricPAtLeast(
                deckSize, landCount, drawsForFlood, landExcessThreshold);

        return new ManaProfile(
                landCount, deckSize, avgCmc, recommendedLands,
                keyTurn, landsNeededByKeyTurn, landExcessThreshold,
                screwProbability, floodProbability, archetypeName);
    }

    // -------------------------------------------------------------------------
    // Hypergeometric helpers
    // -------------------------------------------------------------------------

    /**
     * P(X < k) for X ~ Hypergeometric(N, K, n):
     * probability of drawing fewer than {@code k} successes when drawing
     * {@code n} cards from a population of {@code N} containing {@code K} successes.
     *
     * <p>This is the mana-screw probability: you draw fewer lands than you need.</p>
     */
    static double hypergeometricPLess(final int populationSize,
                                       final int successCount,
                                       final int draws,
                                       final int k) {
        double prob = 0.0;
        final int maxPossible = Math.min(draws, successCount);
        for (int x = 0; x < k && x <= maxPossible; x++) {
            prob += hypergeometricPMF(populationSize, successCount, draws, x);
        }
        return Math.min(1.0, Math.max(0.0, prob));
    }

    /**
     * P(X >= k) for X ~ Hypergeometric(N, K, n):
     * probability of drawing at least {@code k} successes.
     *
     * <p>This is the mana-flood probability: you draw more lands than you need.</p>
     */
    static double hypergeometricPAtLeast(final int populationSize,
                                          final int successCount,
                                          final int draws,
                                          final int k) {
        double prob = 0.0;
        final int maxPossible = Math.min(draws, successCount);
        for (int x = k; x <= maxPossible; x++) {
            prob += hypergeometricPMF(populationSize, successCount, draws, x);
        }
        return Math.min(1.0, Math.max(0.0, prob));
    }

    /**
     * Hypergeometric PMF: P(X = x) = C(K,x) * C(N-K, n-x) / C(N, n).
     *
     * <p>Computed via log-gamma to avoid factorial overflow for large deck sizes.</p>
     *
     * @param N total population size (deck size)
     * @param K number of successes in population (land count)
     * @param n number of draws (cards seen)
     * @param x number of observed successes (lands drawn)
     * @return probability that exactly x successes are drawn
     */
    static double hypergeometricPMF(final int N, final int K, final int n, final int x) {
        if (x < 0 || x > K || x > n || (n - x) > (N - K) || n > N) {
            return 0.0;
        }
        // log P = logC(K,x) + logC(N-K, n-x) - logC(N, n)
        final double logP = logCombination(K, x)
                + logCombination(N - K, n - x)
                - logCombination(N, n);
        return Math.exp(logP);
    }

    /**
     * log C(n, k) = log(n!) - log(k!) - log((n-k)!) via log-gamma.
     */
    private static double logCombination(final int n, final int k) {
        if (k < 0 || k > n) {
            return Double.NEGATIVE_INFINITY;
        }
        if (k == 0 || k == n) {
            return 0.0;
        }
        return logGamma(n + 1) - logGamma(k + 1) - logGamma(n - k + 1);
    }

    /**
     * Stirling-series approximation of log(Gamma(x)) accurate to ~1e-12 for x >= 1.
     * For small integers this is exact enough for our purposes.
     */
    private static double logGamma(final double x) {
        if (x <= 0) {
            return Double.NEGATIVE_INFINITY;
        }
        // Use Lanczos approximation (g=7, n=9 coefficients)
        final double[] c = {
            0.99999999999980993,
            676.5203681218851,
            -1259.1392167224028,
            771.32342877765313,
            -176.61502916214059,
            12.507343278686905,
            -0.13857109526572012,
            9.9843695780195716e-6,
            1.5056327351493116e-7
        };
        if (x < 0.5) {
            return Math.log(Math.PI / Math.sin(Math.PI * x)) - logGamma(1 - x);
        }
        double xi = x - 1;
        double a = c[0];
        for (int i = 1; i < 9; i++) {
            a += c[i] / (xi + i);
        }
        final double t = xi + 7.5;
        return 0.5 * Math.log(2 * Math.PI) + (xi + 0.5) * Math.log(t) - t + Math.log(a);
    }

    /** Fallback profile for decks with no cards. */
    private static ManaProfile defaultProfile() {
        return new ManaProfile(0, 0, 2.0, 23.4, 3, 3, 5, 0.0, 0.0, "unknown");
    }
}
