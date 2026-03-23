package forge.web.simulation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable per-game simulation result. Captures all statistics extracted
 * from a single AI-vs-AI game for later aggregation.
 */
public final class SimulationResult {

    private final boolean won;
    private final boolean stalemate;
    private final int turns;
    private final int mulligans;
    private final boolean onPlay;
    private final int finalLifeTotal;
    private final int opponentFinalLife;
    private final int cardsDrawn;
    private final int emptyHandTurns;
    private final int firstThreatTurn;
    private final int thirdLandTurn;
    private final int fourthLandTurn;
    private final List<String> cardsInHand;
    private final Map<String, Integer> cardDrawCounts;
    private final String opponentDeckName;

    public SimulationResult(
            boolean won,
            boolean stalemate,
            int turns,
            int mulligans,
            boolean onPlay,
            int finalLifeTotal,
            int opponentFinalLife,
            int cardsDrawn,
            int emptyHandTurns,
            int firstThreatTurn,
            int thirdLandTurn,
            int fourthLandTurn,
            List<String> cardsInHand,
            Map<String, Integer> cardDrawCounts,
            String opponentDeckName) {
        this.won = won;
        this.stalemate = stalemate;
        this.turns = turns;
        this.mulligans = mulligans;
        this.onPlay = onPlay;
        this.finalLifeTotal = finalLifeTotal;
        this.opponentFinalLife = opponentFinalLife;
        this.cardsDrawn = cardsDrawn;
        this.emptyHandTurns = emptyHandTurns;
        this.firstThreatTurn = firstThreatTurn;
        this.thirdLandTurn = thirdLandTurn;
        this.fourthLandTurn = fourthLandTurn;
        this.cardsInHand = cardsInHand != null ? Collections.unmodifiableList(cardsInHand) : Collections.emptyList();
        this.cardDrawCounts = cardDrawCounts != null ? Collections.unmodifiableMap(cardDrawCounts) : Collections.emptyMap();
        this.opponentDeckName = opponentDeckName;
    }

    public boolean isWon() { return won; }
    public boolean isStalemate() { return stalemate; }
    public int getTurns() { return turns; }
    public int getMulligans() { return mulligans; }
    public boolean isOnPlay() { return onPlay; }
    public int getFinalLifeTotal() { return finalLifeTotal; }
    public int getOpponentFinalLife() { return opponentFinalLife; }
    public int getCardsDrawn() { return cardsDrawn; }
    public int getEmptyHandTurns() { return emptyHandTurns; }
    public int getFirstThreatTurn() { return firstThreatTurn; }
    public int getThirdLandTurn() { return thirdLandTurn; }
    public int getFourthLandTurn() { return fourthLandTurn; }
    public List<String> getCardsInHand() { return cardsInHand; }
    public Map<String, Integer> getCardDrawCounts() { return cardDrawCounts; }
    public String getOpponentDeckName() { return opponentDeckName; }
}
