package forge.web.starter.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that manages all active game sessions.
 *
 * @Service - Spring annotation that marks this as a service component.
 * Spring will create a single instance (singleton) and inject it where needed.
 *
 * Uses ConcurrentHashMap for thread-safe access to game sessions.
 */
@Service
public class GameSessionManager {

    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Create a new game session.
     *
     * @return The newly created game session
     */
    public GameSession createGame() {
        String gameId = UUID.randomUUID().toString().substring(0, 8);
        GameSession session = new GameSession(gameId);
        activeSessions.put(gameId, session);

        System.out.println("🎮 Created new game: " + gameId);
        return session;
    }

    /**
     * Get an existing game session.
     *
     * @param gameId The ID of the game to retrieve
     * @return The game session, or null if not found
     */
    public GameSession getGame(String gameId) {
        return activeSessions.get(gameId);
    }

    /**
     * Delete a game session.
     *
     * @param gameId The ID of the game to delete
     * @return true if the game was deleted, false if it didn't exist
     */
    public boolean deleteGame(String gameId) {
        GameSession removed = activeSessions.remove(gameId);
        if (removed != null) {
            System.out.println("🗑️ Deleted game: " + gameId);
            return true;
        }
        return false;
    }

    /**
     * Get the number of active games.
     */
    public int getActiveGameCount() {
        return activeSessions.size();
    }
}

