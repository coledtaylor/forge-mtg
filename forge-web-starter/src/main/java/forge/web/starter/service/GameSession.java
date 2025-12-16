package forge.web.starter.service;

import forge.web.starter.model.CardDTO;
import forge.web.starter.model.GameStateDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single game session.
 *
 * In a real application, this would interface with the Forge game engine.
 * For learning purposes, we'll keep it simple with basic state management.
 */
public class GameSession {
    private static final AtomicLong cardIdCounter = new AtomicLong(1);

    private final String gameId;
    private String status = "WAITING";
    private String currentPlayer = "Player1";
    private String currentPhase = "Draw";

    private int player1Life = 20;
    private int player2Life = 20;

    private List<CardDTO> player1Hand = new ArrayList<>();
    private List<CardDTO> player2Hand = new ArrayList<>();
    private List<CardDTO> battlefield = new ArrayList<>();

    private String lastAction;

    public GameSession(String gameId) {
        this.gameId = gameId;
        initializeGame();
    }

    /**
     * Initialize the game with some sample cards.
     */
    private void initializeGame() {
        // Give each player some starting cards
        player1Hand.add(createCard("Lightning Bolt", "Instant", 1));
        player1Hand.add(createCard("Grizzly Bears", "Creature", 2, 2, 2));
        player1Hand.add(createCard("Giant Growth", "Instant", 1));

        player2Hand.add(createCard("Dark Ritual", "Instant", 1));
        player2Hand.add(createCard("Zombie", "Creature", 2, 2, 2));

        this.status = "IN_PROGRESS";
        this.lastAction = "Game started";
    }

    /**
     * Create a sample card.
     */
    private CardDTO createCard(String name, String type, int manaCost) {
        return new CardDTO(
            cardIdCounter.getAndIncrement(),
            name,
            type,
            manaCost,
            0, 0,
            "Sample card text"
        );
    }

    private CardDTO createCard(String name, String type, int manaCost, int power, int toughness) {
        return new CardDTO(
            cardIdCounter.getAndIncrement(),
            name,
            type,
            manaCost,
            power, toughness,
            "Sample creature"
        );
    }

    /**
     * Convert this session to a DTO for sending to clients.
     */
    public GameStateDTO toDTO() {
        GameStateDTO dto = new GameStateDTO();
        dto.setGameId(gameId);
        dto.setStatus(status);
        dto.setCurrentPlayer(currentPlayer);
        dto.setCurrentPhase(currentPhase);
        dto.setPlayer1Life(player1Life);
        dto.setPlayer2Life(player2Life);
        dto.setPlayer1Hand(new ArrayList<>(player1Hand));
        dto.setPlayer2Hand(new ArrayList<>(player2Hand));
        dto.setBattlefield(new ArrayList<>(battlefield));
        dto.setLastAction(lastAction);
        return dto;
    }

    /**
     * Draw a card for a player (simplified).
     */
    public void drawCard(String player) {
        CardDTO newCard = createCard("New Card", "Instant", 1);

        if ("Player1".equals(player)) {
            player1Hand.add(newCard);
        } else {
            player2Hand.add(newCard);
        }

        lastAction = player + " drew a card";
    }

    /**
     * Play a card from hand to battlefield (simplified).
     */
    public boolean playCard(String player, Long cardId) {
        List<CardDTO> hand = "Player1".equals(player) ? player1Hand : player2Hand;

        CardDTO card = hand.stream()
            .filter(c -> c.getId().equals(cardId))
            .findFirst()
            .orElse(null);

        if (card != null) {
            hand.remove(card);
            battlefield.add(card);
            lastAction = player + " played " + card.getName();
            return true;
        }

        return false;
    }

    /**
     * Pass the turn to the other player.
     */
    public void passTurn() {
        currentPlayer = "Player1".equals(currentPlayer) ? "Player2" : "Player1";
        currentPhase = "Draw";
        lastAction = currentPlayer + "'s turn";
    }
}

