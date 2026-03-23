package forge.web.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.http.Context;

import org.tinylog.Logger;

import forge.localinstance.properties.ForgeConstants;

/**
 * REST handler for game log retrieval and management.
 */
public final class GameLogHandler {

    private static final ObjectMapper JSON = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final File GAMELOGS_DIR = new File(ForgeConstants.DECK_CONSTRUCTED_DIR, "../gamelogs");

    private GameLogHandler() { }

    /**
     * GET /api/gamelogs
     * Optional query params: simulationId, source
     * Returns list of game log summaries (without entries).
     */
    @SuppressWarnings("unchecked")
    public static void list(final Context ctx) {
        final String simulationId = ctx.queryParam("simulationId");
        final String source = ctx.queryParam("source");

        final File dir = GAMELOGS_DIR.getAbsoluteFile();
        if (!dir.exists()) {
            ctx.json(List.of());
            return;
        }

        final List<Map<String, Object>> results = new ArrayList<>();
        final File[] files = dir.listFiles((d, name) -> name.startsWith("gamelog-") && name.endsWith(".json"));
        if (files == null) {
            ctx.json(List.of());
            return;
        }

        for (final File file : files) {
            try {
                final Map<String, Object> log = JSON.readValue(file, Map.class);

                // Filter by simulationId if specified
                if (simulationId != null && !simulationId.equals(log.get("simulationId"))) {
                    continue;
                }
                // Filter by source if specified
                if (source != null && !source.equals(log.get("source"))) {
                    continue;
                }

                // Return summary without entries
                final Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("id", log.get("id"));
                summary.put("timestamp", log.get("timestamp"));
                summary.put("source", log.get("source"));
                summary.put("simulationId", log.get("simulationId"));
                summary.put("playerDeck", log.get("playerDeck"));
                summary.put("opponentDeck", log.get("opponentDeck"));
                summary.put("winner", log.get("winner"));
                summary.put("turns", log.get("turns"));
                summary.put("onPlay", log.get("onPlay"));
                results.add(summary);
            } catch (final IOException e) {
                Logger.warn("Failed to read game log {}: {}", file.getName(), e.getMessage());
            }
        }

        // Sort by timestamp descending (newest first)
        results.sort(Comparator.<Map<String, Object>, String>comparing(
                e -> (String) e.get("timestamp")).reversed());

        ctx.json(results);
    }

    /**
     * GET /api/gamelogs/{id}
     * Returns the full game log including all entries.
     */
    @SuppressWarnings("unchecked")
    public static void detail(final Context ctx) {
        final String id = ctx.pathParam("id");
        final File dir = GAMELOGS_DIR.getAbsoluteFile();

        if (!dir.exists()) {
            ctx.status(404).json(Map.of("error", "Game log not found"));
            return;
        }

        final File logFile = new File(dir, "gamelog-" + id + ".json");
        if (!logFile.exists()) {
            ctx.status(404).json(Map.of("error", "Game log not found"));
            return;
        }

        try {
            final Map<String, Object> log = JSON.readValue(logFile, Map.class);
            ctx.json(log);
        } catch (final IOException e) {
            Logger.error(e, "Failed to read game log {}", id);
            ctx.status(500).json(Map.of("error", "Failed to read game log"));
        }
    }

    /**
     * DELETE /api/gamelogs/{id}
     */
    public static void delete(final Context ctx) {
        final String id = ctx.pathParam("id");
        final File dir = GAMELOGS_DIR.getAbsoluteFile();
        final File logFile = new File(dir, "gamelog-" + id + ".json");

        if (logFile.exists() && logFile.delete()) {
            ctx.status(204);
        } else {
            ctx.status(404).json(Map.of("error", "Game log not found"));
        }
    }
}
