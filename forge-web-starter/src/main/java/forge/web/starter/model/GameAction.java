package forge.web.starter.model;

/**
 * Represents an action a player wants to take.
 * <p>
 * This will be sent from the frontend to the backend via REST or WebSocket.
 */
public class GameAction {
    private String gameId;
    private String playerId;
    private String actionType;  // "DRAW", "PLAY_CARD", "ATTACK", "PASS_TURN"
    private Long cardId;        // Optional: which card is involved
    private String target;      // Optional: target for the action

    public GameAction() {
    }

    public GameAction(String gameId, String playerId, String actionType, Long cardId, String target) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.actionType = actionType;
        this.cardId = cardId;
        this.target = target;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}

