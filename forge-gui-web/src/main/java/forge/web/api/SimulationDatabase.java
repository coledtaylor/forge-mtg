package forge.web.api;

import org.tinylog.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite persistence layer for simulation runs and game logs.
 * Manages a single JDBC connection with WAL mode and foreign key support.
 * Schema is auto-created on first use.
 */
public final class SimulationDatabase {

    private final Connection conn;

    /**
     * Open (or create) a SQLite database at the given file path.
     * Enables WAL journal mode and foreign key constraints, then auto-creates
     * the schema if not already present.
     *
     * @param dbFile path to the SQLite database file
     * @throws SQLException if the connection or schema creation fails
     */
    public SimulationDatabase(final File dbFile) throws SQLException {
        final String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        conn = DriverManager.getConnection(url);
        Logger.info("SimulationDatabase opened: {}", dbFile.getAbsolutePath());

        try (final Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA foreign_keys=ON");
        }

        initSchema();
    }

    // =========================================================================
    // Schema
    // =========================================================================

    private void initSchema() throws SQLException {
        try (final Statement st = conn.createStatement()) {
            st.execute(
                "CREATE TABLE IF NOT EXISTS simulation_runs (" +
                "  id TEXT PRIMARY KEY," +
                "  deck_name TEXT NOT NULL," +
                "  timestamp TEXT NOT NULL," +
                "  total_games INTEGER," +
                "  summary_json TEXT," +
                "  archetype TEXT," +
                "  power_score REAL" +
                ")"
            );

            st.execute(
                "CREATE TABLE IF NOT EXISTS game_logs (" +
                "  id TEXT PRIMARY KEY," +
                "  simulation_id TEXT REFERENCES simulation_runs(id) ON DELETE CASCADE," +
                "  timestamp TEXT NOT NULL," +
                "  source TEXT," +
                "  player_deck TEXT," +
                "  opponent_deck TEXT," +
                "  winner TEXT," +
                "  turns INTEGER," +
                "  on_play BOOLEAN," +
                "  won BOOLEAN," +
                "  stalemate BOOLEAN," +
                "  mulligans INTEGER," +
                "  final_life INTEGER," +
                "  opponent_final_life INTEGER," +
                "  cards_drawn INTEGER," +
                "  empty_hand_turns INTEGER," +
                "  first_threat_turn INTEGER," +
                "  third_land_turn INTEGER," +
                "  fourth_land_turn INTEGER," +
                "  total_lands_played INTEGER," +
                "  entries_json TEXT," +
                "  card_draw_counts_json TEXT," +
                "  cards_in_hand_json TEXT" +
                ")"
            );

            st.execute(
                "CREATE INDEX IF NOT EXISTS idx_game_logs_simulation_id " +
                "ON game_logs(simulation_id)"
            );

            st.execute(
                "CREATE INDEX IF NOT EXISTS idx_simulation_runs_deck_name " +
                "ON simulation_runs(deck_name)"
            );
        }
    }

    // =========================================================================
    // simulation_runs CRUD
    // =========================================================================

    /**
     * Insert a new simulation run record.
     */
    public void insertSimulationRun(final String id, final String deckName, final String timestamp,
                                     final int totalGames, final String summaryJson,
                                     final String archetype, final double powerScore) {
        final String sql =
            "INSERT INTO simulation_runs(id, deck_name, timestamp, total_games, summary_json, archetype, power_score) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, deckName);
            ps.setString(3, timestamp);
            ps.setInt(4, totalGames);
            ps.setString(5, summaryJson);
            ps.setString(6, archetype);
            ps.setDouble(7, powerScore);
            ps.executeUpdate();
        } catch (final SQLException e) {
            Logger.error(e, "insertSimulationRun failed for id={}", id);
        }
    }

    /**
     * Retrieve a simulation run by ID.
     *
     * @return map of column name to value, or null if not found
     */
    public Map<String, Object> getSimulationRun(final String id) {
        final String sql = "SELECT * FROM simulation_runs WHERE id = ?";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        } catch (final SQLException e) {
            Logger.error(e, "getSimulationRun failed for id={}", id);
        }
        return null;
    }

    /**
     * Update mutable fields of a simulation run.
     */
    public void updateSimulationRun(final String id, final int totalGames,
                                     final String summaryJson, final String archetype,
                                     final double powerScore) {
        final String sql =
            "UPDATE simulation_runs SET total_games=?, summary_json=?, archetype=?, power_score=? " +
            "WHERE id=?";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, totalGames);
            ps.setString(2, summaryJson);
            ps.setString(3, archetype);
            ps.setDouble(4, powerScore);
            ps.setString(5, id);
            ps.executeUpdate();
        } catch (final SQLException e) {
            Logger.error(e, "updateSimulationRun failed for id={}", id);
        }
    }

    /**
     * Delete a simulation run and its child game logs (via ON DELETE CASCADE).
     */
    public void deleteSimulationRun(final String id) {
        final String sql = "DELETE FROM simulation_runs WHERE id=?";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (final SQLException e) {
            Logger.error(e, "deleteSimulationRun failed for id={}", id);
        }
    }

    /**
     * List simulation runs for a given deck name, ordered newest-first.
     */
    public List<Map<String, Object>> listSimulationRunsByDeck(final String deckName) {
        final String sql =
            "SELECT * FROM simulation_runs WHERE deck_name=? ORDER BY timestamp DESC";
        final List<Map<String, Object>> results = new ArrayList<>();
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deckName);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToMap(rs));
                }
            }
        } catch (final SQLException e) {
            Logger.error(e, "listSimulationRunsByDeck failed for deckName={}", deckName);
        }
        return results;
    }

    // =========================================================================
    // game_logs CRUD
    // =========================================================================

    /**
     * Insert a new game log record.
     */
    public void insertGameLog(final String id, final String simulationId, final String timestamp,
                               final String source, final String playerDeck, final String opponentDeck,
                               final String winner, final int turns, final boolean onPlay,
                               final boolean won, final boolean stalemate, final int mulligans,
                               final int finalLife, final int opponentFinalLife, final int cardsDrawn,
                               final int emptyHandTurns, final int firstThreatTurn,
                               final int thirdLandTurn, final int fourthLandTurn,
                               final int totalLandsPlayed, final String entriesJson,
                               final String cardDrawCountsJson, final String cardsInHandJson) {
        final String sql =
            "INSERT INTO game_logs(" +
            "  id, simulation_id, timestamp, source, player_deck, opponent_deck, winner," +
            "  turns, on_play, won, stalemate, mulligans, final_life, opponent_final_life," +
            "  cards_drawn, empty_hand_turns, first_threat_turn, third_land_turn, fourth_land_turn," +
            "  total_lands_played, entries_json, card_draw_counts_json, cards_in_hand_json" +
            ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, simulationId);
            ps.setString(3, timestamp);
            ps.setString(4, source);
            ps.setString(5, playerDeck);
            ps.setString(6, opponentDeck);
            ps.setString(7, winner);
            ps.setInt(8, turns);
            ps.setBoolean(9, onPlay);
            ps.setBoolean(10, won);
            ps.setBoolean(11, stalemate);
            ps.setInt(12, mulligans);
            ps.setInt(13, finalLife);
            ps.setInt(14, opponentFinalLife);
            ps.setInt(15, cardsDrawn);
            ps.setInt(16, emptyHandTurns);
            ps.setInt(17, firstThreatTurn);
            ps.setInt(18, thirdLandTurn);
            ps.setInt(19, fourthLandTurn);
            ps.setInt(20, totalLandsPlayed);
            ps.setString(21, entriesJson);
            ps.setString(22, cardDrawCountsJson);
            ps.setString(23, cardsInHandJson);
            ps.executeUpdate();
        } catch (final SQLException e) {
            Logger.error(e, "insertGameLog failed for id={}", id);
        }
    }

    /**
     * Retrieve a game log by ID.
     *
     * @return map of column name to value, or null if not found
     */
    public Map<String, Object> getGameLog(final String id) {
        final String sql = "SELECT * FROM game_logs WHERE id=?";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        } catch (final SQLException e) {
            Logger.error(e, "getGameLog failed for id={}", id);
        }
        return null;
    }

    /**
     * Retrieve all game logs belonging to a simulation run.
     *
     * @return list of row maps (empty if none found)
     */
    public List<Map<String, Object>> getGameLogsBySimulationId(final String simulationId) {
        final String sql = "SELECT * FROM game_logs WHERE simulation_id=?";
        final List<Map<String, Object>> results = new ArrayList<>();
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, simulationId);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToMap(rs));
                }
            }
        } catch (final SQLException e) {
            Logger.error(e, "getGameLogsBySimulationId failed for simulationId={}", simulationId);
        }
        return results;
    }

    /**
     * Delete a single game log by ID.
     *
     * @return true if a row was deleted, false if the id was not found
     */
    public boolean deleteGameLog(final String id) {
        final String sql = "DELETE FROM game_logs WHERE id=?";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (final SQLException e) {
            Logger.error(e, "deleteGameLog failed for id={}", id);
            return false;
        }
    }

    /**
     * List game logs with optional filtering by simulation ID and/or source,
     * ordered newest-first.
     *
     * @param simulationId filter by simulation_id (pass null to skip filter)
     * @param source       filter by source column (pass null to skip filter)
     * @return list of row maps (empty if none found)
     */
    public List<Map<String, Object>> listGameLogs(final String simulationId, final String source) {
        final StringBuilder sql = new StringBuilder(
            "SELECT id, simulation_id, timestamp, source, player_deck, opponent_deck," +
            "  winner, turns, on_play" +
            " FROM game_logs");
        final List<Object> params = new ArrayList<>();

        if (simulationId != null || source != null) {
            sql.append(" WHERE");
            boolean needAnd = false;
            if (simulationId != null) {
                sql.append(" simulation_id = ?");
                params.add(simulationId);
                needAnd = true;
            }
            if (source != null) {
                if (needAnd) {
                    sql.append(" AND");
                }
                sql.append(" source = ?");
                params.add(source);
            }
        }
        sql.append(" ORDER BY timestamp DESC");

        final List<Map<String, Object>> results = new ArrayList<>();
        try (final PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToMap(rs));
                }
            }
        } catch (final SQLException e) {
            Logger.error(e, "listGameLogs failed for simulationId={}, source={}", simulationId, source);
        }
        return results;
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Close the underlying JDBC connection.
     */
    public void close() {
        try {
            conn.close();
            Logger.info("SimulationDatabase closed");
        } catch (final SQLException e) {
            Logger.error(e, "Failed to close SimulationDatabase");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static Map<String, Object> rowToMap(final ResultSet rs) throws SQLException {
        final ResultSetMetaData meta = rs.getMetaData();
        final int colCount = meta.getColumnCount();
        final Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= colCount; i++) {
            row.put(meta.getColumnName(i), rs.getObject(i));
        }
        return row;
    }
}
