package forge.web.api;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public class SimulationDatabaseTest {

    private File tempFile;
    private SimulationDatabase db;

    @BeforeMethod
    public void setUp() throws Exception {
        tempFile = File.createTempFile("sim_test_", ".db");
        db = new SimulationDatabase(tempFile);
    }

    @AfterMethod
    public void tearDown() {
        if (db != null) {
            db.close();
        }
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    // =========================================================================
    // Test 1: Schema tables created on construction
    // =========================================================================

    @Test
    public void testSchemaCreatedOnConstruction() throws Exception {
        final String url = "jdbc:sqlite:" + tempFile.getAbsolutePath();
        try (final Connection conn = DriverManager.getConnection(url);
             final Statement st = conn.createStatement()) {
            final ResultSet rs = st.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('simulation_runs','game_logs') ORDER BY name"
            );
            int count = 0;
            while (rs.next()) {
                count++;
            }
            Assert.assertEquals(count, 2, "Both tables should exist");
        }
    }

    // =========================================================================
    // Test 2: Indexes created
    // =========================================================================

    @Test
    public void testIndexesCreated() throws Exception {
        final String url = "jdbc:sqlite:" + tempFile.getAbsolutePath();
        try (final Connection conn = DriverManager.getConnection(url);
             final Statement st = conn.createStatement()) {
            final ResultSet rs = st.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='index' AND name IN " +
                "('idx_game_logs_simulation_id','idx_simulation_runs_deck_name') ORDER BY name"
            );
            int count = 0;
            while (rs.next()) {
                count++;
            }
            Assert.assertEquals(count, 2, "Both indexes should exist");
        }
    }

    // =========================================================================
    // Test 3: Insert and get simulation run (round-trip all 7 columns)
    // =========================================================================

    @Test
    public void testInsertAndGetSimulationRun() {
        db.insertSimulationRun("run-1", "Burn", "2026-01-01T10:00:00", 100,
                "{\"wins\":60}", "Aggro", 72.5);

        final Map<String, Object> row = db.getSimulationRun("run-1");
        Assert.assertNotNull(row);
        Assert.assertEquals(row.get("id"), "run-1");
        Assert.assertEquals(row.get("deck_name"), "Burn");
        Assert.assertEquals(row.get("timestamp"), "2026-01-01T10:00:00");
        Assert.assertEquals(((Number) row.get("total_games")).intValue(), 100);
        Assert.assertEquals(row.get("summary_json"), "{\"wins\":60}");
        Assert.assertEquals(row.get("archetype"), "Aggro");
        Assert.assertEquals(((Number) row.get("power_score")).doubleValue(), 72.5, 0.001);
    }

    // =========================================================================
    // Test 4: Get simulation run not found
    // =========================================================================

    @Test
    public void testGetSimulationRunNotFound() {
        final Map<String, Object> row = db.getSimulationRun("nonexistent-id");
        Assert.assertNull(row);
    }

    // =========================================================================
    // Test 5: Update simulation run
    // =========================================================================

    @Test
    public void testUpdateSimulationRun() {
        db.insertSimulationRun("run-u", "Burn", "2026-01-01T10:00:00", 50,
                "{\"wins\":30}", "Aggro", 60.0);

        db.updateSimulationRun("run-u", 200, "{\"wins\":120}", "Midrange", 85.0);

        final Map<String, Object> row = db.getSimulationRun("run-u");
        Assert.assertNotNull(row);
        Assert.assertEquals(((Number) row.get("total_games")).intValue(), 200);
        Assert.assertEquals(row.get("summary_json"), "{\"wins\":120}");
        Assert.assertEquals(row.get("archetype"), "Midrange");
        Assert.assertEquals(((Number) row.get("power_score")).doubleValue(), 85.0, 0.001);
        // Immutable fields should be unchanged
        Assert.assertEquals(row.get("deck_name"), "Burn");
        Assert.assertEquals(row.get("timestamp"), "2026-01-01T10:00:00");
    }

    // =========================================================================
    // Test 6: Delete simulation run
    // =========================================================================

    @Test
    public void testDeleteSimulationRun() {
        db.insertSimulationRun("run-d", "Burn", "2026-01-01T10:00:00", 50,
                null, null, 0.0);

        db.deleteSimulationRun("run-d");

        Assert.assertNull(db.getSimulationRun("run-d"));
    }

    // =========================================================================
    // Test 7: Delete simulation run cascades game logs
    // =========================================================================

    @Test
    public void testDeleteSimulationRunCascadesGameLogs() {
        db.insertSimulationRun("run-cascade", "Burn", "2026-01-01T10:00:00", 1,
                null, null, 0.0);
        insertSampleGameLog("log-cascade", "run-cascade");

        db.deleteSimulationRun("run-cascade");

        Assert.assertNull(db.getSimulationRun("run-cascade"));
        Assert.assertNull(db.getGameLog("log-cascade"));
    }

    // =========================================================================
    // Test 8: Insert and get game log (round-trip all stat columns)
    // =========================================================================

    @Test
    public void testInsertAndGetGameLog() {
        db.insertSimulationRun("run-gl", "Burn", "2026-01-01T10:00:00", 1, null, null, 0.0);

        db.insertGameLog("log-1", "run-gl", "2026-01-01T10:01:00",
                "simulation", "Burn", "Control", "Burn",
                7, true, true, false,
                1, 12, 0,
                10, 0, 3, 3, 4, 6,
                "[\"entry1\"]", "{\"Bolt\":3}", "{\"turn1\":7}");

        final Map<String, Object> row = db.getGameLog("log-1");
        Assert.assertNotNull(row);
        Assert.assertEquals(row.get("id"), "log-1");
        Assert.assertEquals(row.get("simulation_id"), "run-gl");
        Assert.assertEquals(row.get("timestamp"), "2026-01-01T10:01:00");
        Assert.assertEquals(row.get("source"), "simulation");
        Assert.assertEquals(row.get("player_deck"), "Burn");
        Assert.assertEquals(row.get("opponent_deck"), "Control");
        Assert.assertEquals(row.get("winner"), "Burn");
        Assert.assertEquals(((Number) row.get("turns")).intValue(), 7);
        // SQLite stores booleans as integers (1/0)
        Assert.assertEquals(((Number) row.get("on_play")).intValue(), 1);
        Assert.assertEquals(((Number) row.get("won")).intValue(), 1);
        Assert.assertEquals(((Number) row.get("stalemate")).intValue(), 0);
        Assert.assertEquals(((Number) row.get("mulligans")).intValue(), 1);
        Assert.assertEquals(((Number) row.get("final_life")).intValue(), 12);
        Assert.assertEquals(((Number) row.get("opponent_final_life")).intValue(), 0);
        Assert.assertEquals(((Number) row.get("cards_drawn")).intValue(), 10);
        Assert.assertEquals(((Number) row.get("empty_hand_turns")).intValue(), 0);
        Assert.assertEquals(((Number) row.get("first_threat_turn")).intValue(), 3);
        Assert.assertEquals(((Number) row.get("third_land_turn")).intValue(), 3);
        Assert.assertEquals(((Number) row.get("fourth_land_turn")).intValue(), 4);
        Assert.assertEquals(((Number) row.get("total_lands_played")).intValue(), 6);
        Assert.assertEquals(row.get("entries_json"), "[\"entry1\"]");
        Assert.assertEquals(row.get("card_draw_counts_json"), "{\"Bolt\":3}");
        Assert.assertEquals(row.get("cards_in_hand_json"), "{\"turn1\":7}");
    }

    // =========================================================================
    // Test 9: Get game log not found
    // =========================================================================

    @Test
    public void testGetGameLogNotFound() {
        Assert.assertNull(db.getGameLog("nonexistent-log-id"));
    }

    // =========================================================================
    // Test 10: Get game logs by simulation ID (3 for target, 1 for other)
    // =========================================================================

    @Test
    public void testGetGameLogsBySimulationId() {
        db.insertSimulationRun("run-a", "Burn", "2026-01-01T10:00:00", 3, null, null, 0.0);
        db.insertSimulationRun("run-b", "Control", "2026-01-01T11:00:00", 1, null, null, 0.0);

        insertSampleGameLog("log-a1", "run-a");
        insertSampleGameLog("log-a2", "run-a");
        insertSampleGameLog("log-a3", "run-a");
        insertSampleGameLog("log-b1", "run-b");

        final List<Map<String, Object>> logs = db.getGameLogsBySimulationId("run-a");
        Assert.assertNotNull(logs);
        Assert.assertEquals(logs.size(), 3);

        for (final Map<String, Object> log : logs) {
            Assert.assertEquals(log.get("simulation_id"), "run-a");
        }
    }

    // =========================================================================
    // Test 11: Get game logs by simulation ID returns empty for sim with no logs
    // =========================================================================

    @Test
    public void testGetGameLogsBySimulationIdEmpty() {
        db.insertSimulationRun("run-empty", "Burn", "2026-01-01T10:00:00", 0, null, null, 0.0);

        final List<Map<String, Object>> logs = db.getGameLogsBySimulationId("run-empty");
        Assert.assertNotNull(logs);
        Assert.assertTrue(logs.isEmpty());
    }

    // =========================================================================
    // Test 12: List simulation runs by deck (2 Burn + 1 Control, ordered DESC)
    // =========================================================================

    @Test
    public void testListSimulationRunsByDeck() {
        db.insertSimulationRun("run-burn-1", "Burn", "2026-01-01T09:00:00", 50, null, "Aggro", 60.0);
        db.insertSimulationRun("run-burn-2", "Burn", "2026-01-01T11:00:00", 100, null, "Aggro", 75.0);
        db.insertSimulationRun("run-ctrl-1", "Control", "2026-01-01T10:00:00", 80, null, "Control", 65.0);

        final List<Map<String, Object>> results = db.listSimulationRunsByDeck("Burn");
        Assert.assertNotNull(results);
        Assert.assertEquals(results.size(), 2);

        for (final Map<String, Object> row : results) {
            Assert.assertEquals(row.get("deck_name"), "Burn");
        }

        // Verify descending order by timestamp: newer first
        final String first = (String) results.get(0).get("timestamp");
        final String second = (String) results.get(1).get("timestamp");
        Assert.assertTrue(first.compareTo(second) > 0,
                "Results should be ordered by timestamp DESC (newer first)");
    }

    // =========================================================================
    // Test 13: List simulation runs by deck returns empty for nonexistent deck
    // =========================================================================

    @Test
    public void testListSimulationRunsByDeckEmpty() {
        final List<Map<String, Object>> results = db.listSimulationRunsByDeck("NonexistentDeck");
        Assert.assertNotNull(results);
        Assert.assertTrue(results.isEmpty());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private void insertSampleGameLog(final String id, final String simulationId) {
        db.insertGameLog(id, simulationId, "2026-01-01T10:00:00",
                "simulation", "Burn", "Control", "Burn",
                7, true, true, false,
                0, 20, 0,
                7, 0, 3, 3, 4, 5,
                "[]", "{}", "{}");
    }
}
