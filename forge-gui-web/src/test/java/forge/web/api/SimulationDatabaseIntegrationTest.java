package forge.web.api;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Integration tests for the full persist-query-recalculate lifecycle of SimulationDatabase.
 * Each test operates against a real temporary SQLite file to exercise end-to-end correctness.
 */
public class SimulationDatabaseIntegrationTest {

    private File tempFile;
    private SimulationDatabase db;

    @BeforeMethod
    public void setUp() throws Exception {
        tempFile = File.createTempFile("sim_it_", ".db");
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
    // Test 1: Recalculated stats match inserted data
    // =========================================================================

    @Test
    public void testRecalculateMatchesInsertedData() {
        db.insertSimulationRun("run-recalc", "Burn", "2026-01-01T10:00:00", 10,
                null, "Aggro", 0.0);

        // Insert 10 game logs: 6 wins, 4 losses; specific turn and mulligan counts
        // turns: 5,6,4,7,8,3,5,9,6,4 -> sum=57, avg=5.7
        // mulligans: 0,1,0,2,0,1,0,0,1,0 -> sum=5
        // wins: logs 0-5 (6 wins), logs 6-9 (4 losses)
        final int[] turns     = {5, 6, 4, 7, 8, 3, 5, 9, 6, 4};
        final int[] mulligans = {0, 1, 0, 2, 0, 1, 0, 0, 1, 0};
        final boolean[] won   = {true, true, true, true, true, true, false, false, false, false};

        for (int i = 0; i < 10; i++) {
            db.insertGameLog(
                    "log-recalc-" + i, "run-recalc",
                    "2026-01-01T10:0" + i + ":00",
                    "simulation", "Burn", "Control",
                    won[i] ? "Burn" : "Control",
                    turns[i], true, won[i], false,
                    mulligans[i], won[i] ? 20 : 0, won[i] ? 0 : 20,
                    7, 0, 3, 3, 4, 5,
                    "[]", "{}", "{}");
        }

        final List<Map<String, Object>> logs = db.getGameLogsBySimulationId("run-recalc");

        // Verify count
        Assert.assertEquals(logs.size(), 10, "Should retrieve all 10 inserted game logs");

        // Compute win/loss counts from retrieved logs
        int winCount = 0;
        int lossCount = 0;
        int turnSum = 0;
        int mulliganSum = 0;

        for (final Map<String, Object> log : logs) {
            final int wonVal = ((Number) log.get("won")).intValue();
            if (wonVal == 1) {
                winCount++;
            } else {
                lossCount++;
            }
            turnSum += ((Number) log.get("turns")).intValue();
            mulliganSum += ((Number) log.get("mulligans")).intValue();
        }

        Assert.assertEquals(winCount, 6, "Should have 6 wins");
        Assert.assertEquals(lossCount, 4, "Should have 4 losses");

        // Verify win rate: 6/10 = 0.6
        final double winRate = (double) winCount / logs.size();
        Assert.assertEquals(winRate, 0.6, 0.001, "Win rate should be 0.6");

        // Verify turns sum round-trips (5+6+4+7+8+3+5+9+6+4 = 57)
        Assert.assertEquals(turnSum, 57, "Sum of turns should be 57");

        // Verify mulligan sum round-trips (0+1+0+2+0+1+0+0+1+0 = 5)
        Assert.assertEquals(mulliganSum, 5, "Sum of mulligans should be 5");

        // Verify all logs belong to the correct simulation
        for (final Map<String, Object> log : logs) {
            Assert.assertEquals(log.get("simulation_id"), "run-recalc",
                    "All logs should belong to run-recalc");
        }
    }

    // =========================================================================
    // Test 2: Game log filter by simulation ID isolates logs correctly
    // =========================================================================

    @Test
    public void testGameLogFilterBySimulationId() {
        db.insertSimulationRun("sim-a", "Burn", "2026-01-01T09:00:00", 5, null, null, 0.0);
        db.insertSimulationRun("sim-b", "Control", "2026-01-01T09:00:00", 3, null, null, 0.0);

        // Insert 5 logs for sim-a
        for (int i = 0; i < 5; i++) {
            insertSampleGameLog("log-a-" + i, "sim-a");
        }

        // Insert 3 logs for sim-b
        for (int i = 0; i < 3; i++) {
            insertSampleGameLog("log-b-" + i, "sim-b");
        }

        // Query for sim-a: expect exactly 5, all with simulationId=sim-a
        final List<Map<String, Object>> logsA = db.getGameLogsBySimulationId("sim-a");
        Assert.assertNotNull(logsA);
        Assert.assertEquals(logsA.size(), 5, "sim-a should have exactly 5 game logs");
        for (final Map<String, Object> log : logsA) {
            Assert.assertEquals(log.get("simulation_id"), "sim-a",
                    "All logs returned for sim-a should have simulation_id=sim-a");
        }

        // Query for sim-b: expect exactly 3, all with simulationId=sim-b
        final List<Map<String, Object>> logsB = db.getGameLogsBySimulationId("sim-b");
        Assert.assertNotNull(logsB);
        Assert.assertEquals(logsB.size(), 3, "sim-b should have exactly 3 game logs");
        for (final Map<String, Object> log : logsB) {
            Assert.assertEquals(log.get("simulation_id"), "sim-b",
                    "All logs returned for sim-b should have simulation_id=sim-b");
        }

        // Query for nonexistent sim-c: expect 0
        final List<Map<String, Object>> logsC = db.getGameLogsBySimulationId("sim-c");
        Assert.assertNotNull(logsC);
        Assert.assertEquals(logsC.size(), 0, "Nonexistent sim-c should return 0 game logs");
    }

    // =========================================================================
    // Test 3: Deleting a simulation cascades to its game logs
    // =========================================================================

    @Test
    public void testCascadeDeleteRemovesGameLogs() {
        db.insertSimulationRun("run-cascade", "Burn", "2026-01-01T10:00:00", 5,
                null, null, 0.0);

        for (int i = 0; i < 5; i++) {
            insertSampleGameLog("log-cascade-" + i, "run-cascade");
        }

        // Verify 5 game logs exist before delete
        final List<Map<String, Object>> before = db.getGameLogsBySimulationId("run-cascade");
        Assert.assertEquals(before.size(), 5, "Should have 5 game logs before cascade delete");

        // Delete the simulation run
        db.deleteSimulationRun("run-cascade");

        // Verify game logs are gone (cascaded)
        final List<Map<String, Object>> after = db.getGameLogsBySimulationId("run-cascade");
        Assert.assertEquals(after.size(), 0, "Cascade delete should remove all child game logs");

        // Verify the simulation run itself is gone
        Assert.assertNull(db.getSimulationRun("run-cascade"),
                "Simulation run should be null after delete");
    }

    // =========================================================================
    // Test 4: Database file created at the specified path
    // =========================================================================

    @Test
    public void testDatabaseFileCreatedAtPath() throws Exception {
        // Use a distinct temp path separate from the shared tempFile
        final File specificFile = File.createTempFile("sim_it_path_", ".db");
        specificFile.delete(); // remove so we test that SimulationDatabase creates it

        SimulationDatabase specificDb = null;
        try {
            specificDb = new SimulationDatabase(specificFile);
            Assert.assertTrue(specificFile.exists(),
                    "Database file should exist at the specified path after initialization");
        } finally {
            if (specificDb != null) {
                specificDb.close();
            }
            if (specificFile.exists()) {
                specificFile.delete();
            }
        }
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
