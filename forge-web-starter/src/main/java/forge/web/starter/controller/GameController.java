package forge.web.starter.controller;

import forge.web.starter.model.GameAction;
import forge.web.starter.model.GameStateDTO;
import forge.web.starter.service.GameSession;
import forge.web.starter.service.GameSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for game-related endpoints.
 * <p>
 * This demonstrates:
 * - Dependency injection with @Autowired
 * - POST endpoints for actions
 * - ResponseEntity for better HTTP responses
 */
@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameSessionManager sessionManager;

    /**
     * Constructor injection (preferred over field injection).
     * Spring will automatically provide the GameSessionManager instance.
     */
    @Autowired
    public GameController(GameSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * POST /api/game/new
     * <p>
     * Create a new game session.
     * Returns the initial game state.
     */
    @PostMapping("/new")
    public GameStateDTO createNewGame() {
        System.out.println("🎮 POST /api/game/new");
        GameSession session = sessionManager.createGame();
        return session.toDTO();
    }

    /**
     * GET /api/game/{gameId}
     * <p>
     * Get the current state of a game.
     * Try it: <a href="http://localhost:8080/api/game/">...</a>{gameId}
     * (Replace {gameId} with the ID from creating a game)
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameStateDTO> getGameState(@PathVariable String gameId) {
        System.out.println("📊 GET /api/game/" + gameId);

        GameSession session = sessionManager.getGame(gameId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(session.toDTO());
    }

    /**
     * POST /api/game/{gameId}/action
     * <p>
     * Perform an action in a game.
     * Send JSON like:
     * {
     *   "gameId": "abc123",
     *   "playerId": "Player1",
     *   "actionType": "DRAW"
     * }
     */
    @PostMapping("/{gameId}/action")
    public ResponseEntity<GameStateDTO> performAction(
            @PathVariable String gameId,
            @RequestBody GameAction action) {

        System.out.println("🎯 POST /api/game/" + gameId + "/action - " + action.getActionType());

        GameSession session = sessionManager.getGame(gameId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        // Process the action
        switch (action.getActionType()) {
            case "DRAW":
                session.drawCard(action.getPlayerId());
                break;
            case "PLAY_CARD":
                session.playCard(action.getPlayerId(), action.getCardId());
                break;
            case "PASS_TURN":
                session.passTurn();
                break;
            default:
                return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(session.toDTO());
    }

    /**
     * DELETE /api/game/{gameId}
     * <p>
     * Delete a game session.
     */
    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(@PathVariable String gameId) {
        System.out.println("🗑️ DELETE /api/game/" + gameId);

        boolean deleted = sessionManager.deleteGame(gameId);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * GET /api/game/stats
     * <p>
     * Get statistics about active games.
     */
    @GetMapping("/stats")
    public String getStats() {
        return "Active games: " + sessionManager.getActiveGameCount();
    }
}

