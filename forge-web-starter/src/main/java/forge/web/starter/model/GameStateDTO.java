package forge.web.starter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the current state of a game.
 * <p>
 * This simplified version shows the concept of serializing
 * game state to send to the frontend.
 */
public class GameStateDTO {
    private String gameId;
    private String status;              // "WAITING", "IN_PROGRESS", "FINISHED"
    private String currentPlayer;       // "Player1" or "Player2"
    private String currentPhase;        // "Draw", "Main", "Combat", "End"

    private int player1Life = 20;
    private int player2Life = 20;

    private List<CardDTO> player1Hand = new ArrayList<>();
    private List<CardDTO> player2Hand = new ArrayList<>();

    private List<CardDTO> battlefield = new ArrayList<>();

    private String lastAction;          // Description of last action taken

    public GameStateDTO() {
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(String currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public int getPlayer1Life() {
        return player1Life;
    }

    public void setPlayer1Life(int player1Life) {
        this.player1Life = player1Life;
    }

    public int getPlayer2Life() {
        return player2Life;
    }

    public void setPlayer2Life(int player2Life) {
        this.player2Life = player2Life;
    }

    public List<CardDTO> getPlayer1Hand() {
        return player1Hand;
    }

    public void setPlayer1Hand(List<CardDTO> player1Hand) {
        this.player1Hand = player1Hand;
    }

    public List<CardDTO> getPlayer2Hand() {
        return player2Hand;
    }

    public void setPlayer2Hand(List<CardDTO> player2Hand) {
        this.player2Hand = player2Hand;
    }

    public List<CardDTO> getBattlefield() {
        return battlefield;
    }

    public void setBattlefield(List<CardDTO> battlefield) {
        this.battlefield = battlefield;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }
}

