package forge.web.simulation;

import java.util.Map;

import forge.card.CardRules;
import forge.card.CardType;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;

/**
 * Classifies a deck into a {@link DeckArchetype} by analyzing card features.
 *
 * <p>Only the main deck section is inspected. Lands are excluded from all
 * feature calculations so that mana-base composition does not distort the
 * results.</p>
 */
public final class DeckArchetypeClassifier {

    // Keywords used to detect direct-damage spells (burn).
    private static final String[] DIRECT_DAMAGE_PATTERNS = {
        "deals", "damage to any target", "damage to target player",
        "damage to each opponent", "damage to target opponent",
        "damage to any target", "lightning bolt", "shock"
    };

    // Keywords used to detect counterspells.
    private static final String[] COUNTERSPELL_PATTERNS = {
        "counter target spell", "counter target instant", "counter target sorcery",
        "counter target creature", "counter target noncreature"
    };

    // Keywords used to detect card-draw spells.
    private static final String[] CARD_DRAW_PATTERNS = {
        "draw a card", "draw two cards", "draw three cards", "draw cards",
        "you draw", "draws a card", "draws two cards"
    };

    // Minimum score required for an archetype to be declared (prevents weak matches).
    private static final double SCORE_THRESHOLD = 0.25;

    private DeckArchetypeClassifier() { }

    /**
     * Returns per-archetype scores for the radar chart (0-1 each).
     *
     * <p>Scores are keyed as {@code aggro}, {@code midrange}, {@code control},
     * {@code combo}. The burn score is folded into the aggro axis using
     * {@code max(burnScore, aggroScore)} since burn is an aggro sub-strategy.</p>
     *
     * @param deck the deck to analyze; must not be {@code null}
     * @return a map of archetype axis name to score (0-1)
     */
    public static java.util.Map<String, Double> getPlaystyleScores(final Deck deck) {
        final CardPool main = deck.getMain();
        if (main == null || main.isEmpty()) {
            return getDefaultScores();
        }
        final double[] features = extractFeatures(main);
        if (features == null) {
            return getDefaultScores();
        }
        return computePlaystyleScores(features[0], features[1], features[2], features[3], features[4]);
    }

    /**
     * Returns per-archetype scores from pre-computed feature values.
     * Package-private for testing.
     */
    static java.util.Map<String, Double> computePlaystyleScores(final double avgCmc,
                                                                  final double creaturePct,
                                                                  final double directDamagePct,
                                                                  final double counterspellPct,
                                                                  final double cardDrawPct) {
        final double burnScore     = scoreBurn(avgCmc, creaturePct, directDamagePct);
        final double aggroScore    = scoreAggro(avgCmc, creaturePct, directDamagePct);
        final double controlScore  = scoreControl(avgCmc, creaturePct, counterspellPct, cardDrawPct);
        final double midrangeScore = scoreMidrange(avgCmc, creaturePct);
        final double comboScore    = scoreCombo(creaturePct, cardDrawPct);

        final java.util.Map<String, Double> scores = new java.util.HashMap<>();
        // Burn folds into aggro axis — burn IS aggro, just spell-based instead of creature-based
        scores.put("aggro", Math.min(1.0, Math.max(burnScore, aggroScore)));
        scores.put("midrange", midrangeScore);
        scores.put("control", controlScore);
        scores.put("combo", comboScore);
        return scores;
    }

    private static java.util.Map<String, Double> getDefaultScores() {
        final java.util.Map<String, Double> scores = new java.util.HashMap<>();
        scores.put("aggro", 0.0);
        scores.put("midrange", 0.0);
        scores.put("control", 0.0);
        scores.put("combo", 0.0);
        return scores;
    }

    /**
     * Extracts feature values from the main deck card pool.
     * @return [avgCmc, creaturePct, directDamagePct, counterspellPct, cardDrawPct] or null if empty
     */
    private static double[] extractFeatures(final CardPool main) {
        int totalNonLand = 0;
        int creatureCount = 0;
        int directDamageCount = 0;
        int counterspellCount = 0;
        int cardDrawCount = 0;
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
                continue;
            }

            totalNonLand += copies;
            totalCmc += rules.getManaCost().getCMC() * copies;

            if (type.isCreature()) {
                creatureCount += copies;
            }

            final String oracle = rules.getOracleText().toLowerCase();
            if (containsAny(oracle, DIRECT_DAMAGE_PATTERNS)) {
                directDamageCount += copies;
            }
            if (containsAny(oracle, COUNTERSPELL_PATTERNS)) {
                counterspellCount += copies;
            }
            if (containsAny(oracle, CARD_DRAW_PATTERNS)) {
                cardDrawCount += copies;
            }
        }

        if (totalNonLand == 0) {
            return null;
        }

        return new double[] {
            totalCmc / totalNonLand,
            (double) creatureCount / totalNonLand,
            (double) directDamageCount / totalNonLand,
            (double) counterspellCount / totalNonLand,
            (double) cardDrawCount / totalNonLand
        };
    }

    /**
     * Classifies the provided deck into a {@link DeckArchetype}.
     *
     * @param deck the deck to classify; must not be {@code null}
     * @return the best-matching archetype, or {@link DeckArchetype#UNKNOWN} if
     *         the deck does not clearly match any known profile
     */
    public static DeckArchetype classify(final Deck deck) {
        final CardPool main = deck.getMain();
        if (main == null || main.isEmpty()) {
            return DeckArchetype.UNKNOWN;
        }
        final double[] features = extractFeatures(main);
        if (features == null) {
            return DeckArchetype.UNKNOWN;
        }
        return classifyFromFeatures(features[0], features[1], features[2], features[3], features[4]);
    }

    /**
     * Classifies a deck given pre-computed feature values.
     *
     * <p>This method is package-private to allow direct testing with synthetic
     * feature vectors without requiring a fully constructed {@link Deck}.</p>
     *
     * @param avgCmc          average converted mana cost of non-land cards
     * @param creaturePct     fraction of non-land cards that are creatures (0–1)
     * @param directDamagePct fraction of non-land cards with direct-damage text (0–1)
     * @param counterspellPct fraction of non-land cards with counterspell text (0–1)
     * @param cardDrawPct     fraction of non-land cards with card-draw text (0–1)
     * @return the best-matching archetype
     */
    static DeckArchetype classifyFromFeatures(final double avgCmc,
                                               final double creaturePct,
                                               final double directDamagePct,
                                               final double counterspellPct,
                                               final double cardDrawPct) {
        double burnScore     = scoreBurn(avgCmc, creaturePct, directDamagePct);
        double aggroScore    = scoreAggro(avgCmc, creaturePct, directDamagePct);
        double controlScore  = scoreControl(avgCmc, creaturePct, counterspellPct, cardDrawPct);
        double midrangeScore = scoreMidrange(avgCmc, creaturePct);
        double comboScore    = scoreCombo(creaturePct, cardDrawPct);

        double best = SCORE_THRESHOLD;
        DeckArchetype result = DeckArchetype.UNKNOWN;

        if (burnScore > best) {
            best = burnScore;
            result = DeckArchetype.BURN;
        }
        if (aggroScore > best) {
            best = aggroScore;
            result = DeckArchetype.AGGRO;
        }
        if (controlScore > best) {
            best = controlScore;
            result = DeckArchetype.CONTROL;
        }
        if (midrangeScore > best) {
            best = midrangeScore;
            result = DeckArchetype.MIDRANGE;
        }
        if (comboScore > best) {
            result = DeckArchetype.COMBO;
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Per-archetype scoring functions (all return 0–1)
    // -------------------------------------------------------------------------

    // BURN: high direct damage (50–70%), low creatures (20–35%), low avg CMC (1.5–2.2)
    private static double scoreBurn(final double avgCmc,
                                    final double creaturePct,
                                    final double directDamagePct) {
        double score = 0.0;
        score += gaussian(directDamagePct, 0.60, 0.12) * 0.50;  // dominant signal
        score += gaussian(creaturePct,     0.28, 0.10) * 0.25;
        score += gaussian(avgCmc,          1.85, 0.35) * 0.25;
        return score;
    }

    // AGGRO: high creatures (60–70%), very low avg CMC (1.5–2.2), low direct damage
    private static double scoreAggro(final double avgCmc,
                                     final double creaturePct,
                                     final double directDamagePct) {
        double score = 0.0;
        score += gaussian(creaturePct,     0.65, 0.10) * 0.50;
        score += gaussian(avgCmc,          1.85, 0.35) * 0.30;
        // penalize if it also looks like burn
        score += gaussian(directDamagePct, 0.05, 0.06) * 0.20;
        return score;
    }

    // CONTROL: high counterspells (15–30%), high card draw (15–25%), low creatures, high CMC
    private static double scoreControl(final double avgCmc,
                                       final double creaturePct,
                                       final double counterspellPct,
                                       final double cardDrawPct) {
        double score = 0.0;
        score += gaussian(counterspellPct, 0.22, 0.08) * 0.35;
        score += gaussian(cardDrawPct,     0.20, 0.07) * 0.25;
        score += gaussian(creaturePct,     0.20, 0.08) * 0.20;
        score += gaussian(avgCmc,          3.50, 0.60) * 0.20;
        return score;
    }

    // MIDRANGE: moderate creatures (40–50%), moderate CMC (2.5–3.5)
    private static double scoreMidrange(final double avgCmc, final double creaturePct) {
        double score = 0.0;
        score += gaussian(creaturePct, 0.45, 0.08) * 0.55;
        score += gaussian(avgCmc,      3.00, 0.45) * 0.45;
        return score;
    }

    // COMBO: high card draw, low creatures, less CMC pressure
    private static double scoreCombo(final double creaturePct, final double cardDrawPct) {
        double score = 0.0;
        score += gaussian(cardDrawPct,  0.30, 0.10) * 0.60;
        score += gaussian(creaturePct,  0.10, 0.08) * 0.40;
        return score;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Gaussian similarity: returns 1.0 when {@code value == mean}, dropping off
     * symmetrically with standard deviation {@code sigma}.
     */
    private static double gaussian(final double value, final double mean, final double sigma) {
        final double diff = value - mean;
        return Math.exp(-(diff * diff) / (2.0 * sigma * sigma));
    }

    /** Returns {@code true} if {@code text} contains any of the given patterns (case-insensitive). */
    private static boolean containsAny(final String text, final String[] patterns) {
        for (final String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
