package forge.web.starter.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import forge.web.starter.model.GameAction;
import forge.web.starter.model.GameStateDTO;
import forge.web.starter.service.GameSession;
import forge.web.starter.service.GameSessionManager;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time game updates.
 *
 * This handles:
 * - Client connections
 * - Incoming messages (game actions)
 * - Broadcasting state updates to connected clients
 *
 * @Component - Spring will create and manage this bean
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final GameSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    // Track which WebSocket sessions are connected to which games
    private final Map<String, WebSocketSession> gameSubscriptions = new ConcurrentHashMap<>();

    public GameWebSocketHandler(GameSessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Called when a client connects to the WebSocket.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String gameId = extractGameId(session);
        gameSubscriptions.put(session.getId(), session);

        System.out.println("🔌 WebSocket connected: " + session.getId() + " (Game: " + gameId + ")");

        // Send current game state immediately
        GameSession gameSession = sessionManager.getGame(gameId);
        if (gameSession != null) {
            sendGameState(session, gameSession);
        }
    }

    /**
     * Called when a message is received from the client.
     *
     * Expects JSON messages like:
     * {
     *   "gameId": "abc123",
     *   "playerId": "Player1",
     *   "actionType": "DRAW"
     * }
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("📨 Received WebSocket message: " + payload);

        try {
            // Parse the action
            GameAction action = objectMapper.readValue(payload, GameAction.class);

            // Get the game session
            GameSession gameSession = sessionManager.getGame(action.getGameId());
            if (gameSession == null) {
                sendError(session, "Game not found: " + action.getGameId());
                return;
            }

            // Process the action
            processAction(gameSession, action);

            // Broadcast updated state to all connected clients for this game
            broadcastGameState(action.getGameId(), gameSession);

        } catch (Exception e) {
            System.err.println("❌ Error processing WebSocket message: " + e.getMessage());
            sendError(session, "Invalid action: " + e.getMessage());
        }
    }

    /**
     * Called when a client disconnects.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        gameSubscriptions.remove(session.getId());
        System.out.println("🔌 WebSocket disconnected: " + session.getId());
    }

    /**
     * Process a game action.
     */
    private void processAction(GameSession gameSession, GameAction action) {
        switch (action.getActionType()) {
            case "DRAW":
                gameSession.drawCard(action.getPlayerId());
                break;
            case "PLAY_CARD":
                gameSession.playCard(action.getPlayerId(), action.getCardId());
                break;
            case "PASS_TURN":
                gameSession.passTurn();
                break;
            default:
                System.err.println("Unknown action type: " + action.getActionType());
        }
    }

    /**
     * Send game state to a specific WebSocket session.
     */
    private void sendGameState(WebSocketSession session, GameSession gameSession) throws IOException {
        GameStateDTO state = gameSession.toDTO();
        String json = objectMapper.writeValueAsString(state);
        session.sendMessage(new TextMessage(json));
    }

    /**
     * Broadcast game state to all connected clients for a game.
     */
    private void broadcastGameState(String gameId, GameSession gameSession) {
        GameStateDTO state = gameSession.toDTO();

        try {
            String json = objectMapper.writeValueAsString(state);
            TextMessage message = new TextMessage(json);

            // Send to all connected clients
            for (WebSocketSession session : gameSubscriptions.values()) {
                if (session.isOpen() && gameId.equals(extractGameId(session))) {
                    session.sendMessage(message);
                }
            }

            System.out.println("📡 Broadcasted game state for game: " + gameId);

        } catch (IOException e) {
            System.err.println("❌ Error broadcasting game state: " + e.getMessage());
        }
    }

    /**
     * Send an error message to the client.
     */
    private void sendError(WebSocketSession session, String error) throws IOException {
        String json = objectMapper.writeValueAsString(Map.of("error", error));
        session.sendMessage(new TextMessage(json));
    }

    /**
     * Extract game ID from the WebSocket URL path.
     * URL pattern: /ws/game/{gameId}
     */
    private String extractGameId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }
}

