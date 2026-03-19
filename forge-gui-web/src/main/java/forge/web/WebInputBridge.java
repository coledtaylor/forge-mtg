package forge.web;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges the synchronous game engine thread with asynchronous WebSocket communication.
 * When the engine needs player input, it registers a CompletableFuture by inputId and blocks.
 * When the client responds via WebSocket, the future is completed, unblocking the game thread.
 */
public class WebInputBridge {

    private final ConcurrentHashMap<String, CompletableFuture<String>> pending = new ConcurrentHashMap<>();

    /**
     * Register a new pending input request.
     *
     * @param inputId unique identifier for this input request
     * @return a CompletableFuture that will be completed when the client responds
     */
    public CompletableFuture<String> register(final String inputId) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        pending.put(inputId, future);
        return future;
    }

    /**
     * Complete a pending input request with the client's response.
     *
     * @param inputId the identifier of the input request
     * @param responseJson the client's response as a JSON string
     * @return true if the inputId was found and completed, false otherwise
     */
    public boolean complete(final String inputId, final String responseJson) {
        final CompletableFuture<String> future = pending.remove(inputId);
        if (future == null) {
            return false;
        }
        future.complete(responseJson);
        return true;
    }

    /**
     * Cancel all pending input requests. Used when a game session expires or is terminated.
     * All pending futures are completed exceptionally with GameSessionExpiredException.
     */
    public void cancelAll() {
        for (final CompletableFuture<String> future : pending.values()) {
            future.completeExceptionally(new GameSessionExpiredException("Game session expired"));
        }
        pending.clear();
    }

    /**
     * @return the number of pending input requests
     */
    public int pendingCount() {
        return pending.size();
    }

    /**
     * Check if an inputId is currently registered and pending.
     *
     * @param inputId the identifier to check
     * @return true if the inputId is registered
     */
    public boolean hasPending(final String inputId) {
        return pending.containsKey(inputId);
    }

    /**
     * Exception thrown when a game session has expired or been terminated.
     */
    public static class GameSessionExpiredException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public GameSessionExpiredException(final String message) {
            super(message);
        }
    }
}
