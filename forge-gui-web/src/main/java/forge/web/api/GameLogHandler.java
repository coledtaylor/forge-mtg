package forge.web.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.http.Context;

import org.tinylog.Logger;

import forge.web.WebServer;

/**
 * REST handler for game log retrieval and management.
 */
public final class GameLogHandler {

    private static final ObjectMapper JSON = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private GameLogHandler() { }

    /**
     * GET /api/gamelogs
     * Optional query params: simulationId, source
     * Returns list of game log summaries (without entries).
     */
    public static void list(final Context ctx) {
        final String simulationId = ctx.queryParam("simulationId");
        final String source = ctx.queryParam("source");

        final SimulationDatabase db = WebServer.getDatabase();
        if (db == null) {
            ctx.json(List.of());
            return;
        }

        final List<Map<String, Object>> rows = db.listGameLogs(simulationId, source);
        final List<Map<String, Object>> results = new ArrayList<>(rows.size());

        for (final Map<String, Object> row : rows) {
            final Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", row.get("id"));
            summary.put("timestamp", row.get("timestamp"));
            summary.put("source", row.get("source"));
            summary.put("simulationId", row.get("simulation_id"));
            summary.put("playerDeck", row.get("player_deck"));
            summary.put("opponentDeck", row.get("opponent_deck"));
            summary.put("winner", row.get("winner"));
            summary.put("turns", row.get("turns"));
            summary.put("onPlay", toBoolean(row.get("on_play")));
            results.add(summary);
        }

        // Already sorted by timestamp DESC from the database query
        ctx.json(results);
    }

    /**
     * GET /api/gamelogs/{id}
     * Returns the full game log including all entries.
     */
    public static void detail(final Context ctx) {
        final String id = ctx.pathParam("id");

        final SimulationDatabase db = WebServer.getDatabase();
        if (db == null) {
            ctx.status(404).json(Map.of("error", "Game log not found"));
            return;
        }

        final Map<String, Object> row = db.getGameLog(id);
        if (row == null) {
            ctx.status(404).json(Map.of("error", "Game log not found"));
            return;
        }

        final Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", row.get("id"));
        response.put("timestamp", row.get("timestamp"));
        response.put("source", row.get("source"));
        response.put("simulationId", row.get("simulation_id"));
        response.put("playerDeck", row.get("player_deck"));
        response.put("opponentDeck", row.get("opponent_deck"));
        response.put("winner", row.get("winner"));
        response.put("turns", row.get("turns"));
        response.put("onPlay", toBoolean(row.get("on_play")));

        // Deserialize entries_json back into a list
        final String entriesJson = (String) row.get("entries_json");
        if (entriesJson != null) {
            try {
                final List<Map<String, Object>> entries =
                        JSON.readValue(entriesJson, new TypeReference<List<Map<String, Object>>>() { });
                response.put("entries", entries);
            } catch (final IOException e) {
                Logger.warn("Failed to deserialize entries_json for log {}: {}", id, e.getMessage());
                response.put("entries", List.of());
            }
        } else {
            response.put("entries", List.of());
        }

        ctx.json(response);
    }

    /**
     * DELETE /api/gamelogs/{id}
     */
    public static void delete(final Context ctx) {
        final String id = ctx.pathParam("id");

        final SimulationDatabase db = WebServer.getDatabase();
        if (db == null) {
            ctx.status(404).json(Map.of("error", "Game log not found"));
            return;
        }

        if (db.deleteGameLog(id)) {
            ctx.status(204);
        } else {
            ctx.status(404).json(Map.of("error", "Game log not found"));
        }
    }

    /**
     * Converts a database boolean value (may be Integer 0/1 from SQLite) to Boolean.
     */
    private static Boolean toBoolean(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }
}
