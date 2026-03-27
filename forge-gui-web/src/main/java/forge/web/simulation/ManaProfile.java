package forge.web.simulation;

/**
 * Immutable mana profile derived from a deck's mana curve.
 *
 * <p>Captures the deck's land count, average CMC, Karsten-recommended land count,
 * key turn (the turn by which the deck ideally has its critical mana online),
 * and pre-computed hypergeometric mana screw/flood probabilities that serve as
 * baselines for evaluating actual simulation results.</p>
 *
 * <p>Serializes cleanly to JSON via Jackson (all fields exposed as getters,
 * no circular references).</p>
 */
public final class ManaProfile {

    private final int landCount;
    private final int deckSize;
    private final double avgCmc;
    private final double recommendedLands;
    private final int keyTurn;
    private final int landsNeededByKeyTurn;
    private final int landExcessThreshold;
    private final double screwProbability;
    private final double floodProbability;
    private final String archetype;

    public ManaProfile(final int landCount,
                       final int deckSize,
                       final double avgCmc,
                       final double recommendedLands,
                       final int keyTurn,
                       final int landsNeededByKeyTurn,
                       final int landExcessThreshold,
                       final double screwProbability,
                       final double floodProbability,
                       final String archetype) {
        this.landCount = landCount;
        this.deckSize = deckSize;
        this.avgCmc = avgCmc;
        this.recommendedLands = recommendedLands;
        this.keyTurn = keyTurn;
        this.landsNeededByKeyTurn = landsNeededByKeyTurn;
        this.landExcessThreshold = landExcessThreshold;
        this.screwProbability = screwProbability;
        this.floodProbability = floodProbability;
        this.archetype = archetype;
    }

    public int getLandCount() { return landCount; }
    public int getDeckSize() { return deckSize; }
    public double getAvgCmc() { return avgCmc; }
    public double getRecommendedLands() { return recommendedLands; }
    public int getKeyTurn() { return keyTurn; }
    public int getLandsNeededByKeyTurn() { return landsNeededByKeyTurn; }
    public int getLandExcessThreshold() { return landExcessThreshold; }
    public double getScrewProbability() { return screwProbability; }
    public double getFloodProbability() { return floodProbability; }
    public String getArchetype() { return archetype; }
}
