package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates archetype AI profile infrastructure added in Feature 0004 Phase 1.
 *
 * <p>This test class verifies two things for each archetype that maps to a named AI profile:</p>
 * <ol>
 *   <li>The {@code .ai} file exists at {@code forge-gui/res/ai/{Name}.ai} and can be parsed.</li>
 *   <li>Key properties in each profile have archetype-appropriate values, confirming the profile
 *       was intentionally configured rather than copied from a generic template.</li>
 * </ol>
 *
 * <p>Any failing test indicates either a missing {@code .ai} file or a misconfigured property.
 * Classification coverage (AGGRO, BURN, MIDRANGE, CONTROL, COMBO, UNKNOWN) is separately verified
 * in {@code forge-gui-web} by {@code DeckArchetypeClassifierTest}, which uses the
 * {@code DeckArchetypeClassifier.classifyFromFeatures()} method with representative feature vectors.</p>
 *
 * <p>Note: tests use relative paths anchored to {@code ../forge-gui/res/ai/} which resolves
 * correctly when run from the {@code forge-gui-desktop} module directory (the Maven default).</p>
 */
public class ArchetypeProfileTest {

    /** Path to the AI profiles directory, relative to the forge-gui-desktop module root. */
    private static final String AI_PROFILE_DIR = "../forge-gui/res/ai/";

    private static final String EXT = ".ai";

    // ---------------------------------------------------------------------------
    // Helper: parse a .ai file into a property map
    // ---------------------------------------------------------------------------

    /**
     * Reads a profile file and returns its key=value entries as a String map.
     * Lines starting with {@code #} and blank lines are ignored.
     *
     * @param profileName the profile base name (e.g. {@code "Aggro"})
     * @return a map of property name to raw string value
     * @throws IOException if the file cannot be read
     */
    private Map<String, String> loadProfileProperties(final String profileName) throws IOException {
        final File file = new File(AI_PROFILE_DIR + profileName + EXT);
        final List<String> lines = Files.readAllLines(file.toPath());
        final Map<String, String> props = new HashMap<>();
        for (final String line : lines) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            final int eq = trimmed.indexOf('=');
            if (eq > 0) {
                final String key = trimmed.substring(0, eq).trim();
                final String value = trimmed.substring(eq + 1).trim();
                props.put(key, value);
            }
        }
        return props;
    }

    // ---------------------------------------------------------------------------
    // Setup: verify the profile directory itself exists before running any test
    // ---------------------------------------------------------------------------

    @BeforeClass
    public void profileDirectoryMustExist() {
        final File dir = new File(AI_PROFILE_DIR);
        AssertJUnit.assertTrue(
                "AI profile directory must exist at: " + dir.getAbsolutePath(),
                dir.isDirectory());
    }

    // ---------------------------------------------------------------------------
    // Test group 1: File existence — one test per archetype profile
    // ---------------------------------------------------------------------------

    @Test
    public void aggroProfileFileExists() {
        final File file = new File(AI_PROFILE_DIR + "Aggro" + EXT);
        AssertJUnit.assertTrue(
                "Aggro.ai must exist at: " + file.getAbsolutePath(),
                file.exists() && file.isFile());
    }

    @Test
    public void burnProfileFileExists() {
        final File file = new File(AI_PROFILE_DIR + "Burn" + EXT);
        AssertJUnit.assertTrue(
                "Burn.ai must exist at: " + file.getAbsolutePath(),
                file.exists() && file.isFile());
    }

    @Test
    public void midrangeProfileFileExists() {
        final File file = new File(AI_PROFILE_DIR + "Midrange" + EXT);
        AssertJUnit.assertTrue(
                "Midrange.ai must exist at: " + file.getAbsolutePath(),
                file.exists() && file.isFile());
    }

    @Test
    public void controlProfileFileExists() {
        final File file = new File(AI_PROFILE_DIR + "Control" + EXT);
        AssertJUnit.assertTrue(
                "Control.ai must exist at: " + file.getAbsolutePath(),
                file.exists() && file.isFile());
    }

    @Test
    public void comboProfileFileExists() {
        final File file = new File(AI_PROFILE_DIR + "Combo" + EXT);
        AssertJUnit.assertTrue(
                "Combo.ai must exist at: " + file.getAbsolutePath(),
                file.exists() && file.isFile());
    }

    // ---------------------------------------------------------------------------
    // Test group 2: Profile property validation — spot-check key archetype values
    // ---------------------------------------------------------------------------

    /**
     * Aggro profile: {@code PLAY_AGGRO} must be {@code true} to enable aggressive
     * attack sequencing.
     */
    @Test
    public void aggroProfileHasPlayAggroTrue() throws IOException {
        final Map<String, String> props = loadProfileProperties("Aggro");
        AssertJUnit.assertEquals(
                "Aggro.ai PLAY_AGGRO must be 'true'",
                "true", props.get("PLAY_AGGRO"));
    }

    /**
     * Control profile: {@code CHANCE_TO_COUNTER_CMC_3} must be {@code 100} so the
     * control AI counters every 3-CMC threat.
     */
    @Test
    public void controlProfileCountersCmc3AtHundredPercent() throws IOException {
        final Map<String, String> props = loadProfileProperties("Control");
        AssertJUnit.assertEquals(
                "Control.ai CHANCE_TO_COUNTER_CMC_3 must be '100'",
                "100", props.get("CHANCE_TO_COUNTER_CMC_3"));
    }

    /**
     * Combo profile: {@code TRY_TO_PRESERVE_BUYBACK_SPELLS} must be {@code true}
     * to conserve hand resources for combo assembly.
     */
    @Test
    public void comboProfilePreservesBuybackSpells() throws IOException {
        final Map<String, String> props = loadProfileProperties("Combo");
        AssertJUnit.assertEquals(
                "Combo.ai TRY_TO_PRESERVE_BUYBACK_SPELLS must be 'true'",
                "true", props.get("TRY_TO_PRESERVE_BUYBACK_SPELLS"));
    }

    /**
     * Midrange profile: {@code CHANCE_TO_ATTACK_INTO_TRADE} must be in the range
     * 20–60 (opportunistic trades, not all-out aggression, not zero).
     */
    @Test
    public void midrangeProfileAttackIntoTradeIsBalanced() throws IOException {
        final Map<String, String> props = loadProfileProperties("Midrange");
        final String rawValue = props.get("CHANCE_TO_ATTACK_INTO_TRADE");
        AssertJUnit.assertNotNull(
                "Midrange.ai must define CHANCE_TO_ATTACK_INTO_TRADE",
                rawValue);
        final int value = Integer.parseInt(rawValue);
        AssertJUnit.assertTrue(
                "Midrange.ai CHANCE_TO_ATTACK_INTO_TRADE must be between 20 and 60 (inclusive), was: " + value,
                value >= 20 && value <= 60);
    }

    /**
     * Burn profile: {@code BURN_FACE_DAMAGE_PRIORITY} must be {@code true} to
     * keep direct-damage spells aimed at the opponent player.
     */
    @Test
    public void burnProfileHasFaceDamagePriorityTrue() throws IOException {
        final Map<String, String> props = loadProfileProperties("Burn");
        AssertJUnit.assertEquals(
                "Burn.ai BURN_FACE_DAMAGE_PRIORITY must be 'true'",
                "true", props.get("BURN_FACE_DAMAGE_PRIORITY"));
    }

    // ---------------------------------------------------------------------------
    // Test group 3: Profile completeness — each archetype file must be non-empty
    //               and must parse at least one property (not a stub/placeholder)
    // ---------------------------------------------------------------------------

    @Test
    public void aggroProfileIsParseable() throws IOException {
        final Map<String, String> props = loadProfileProperties("Aggro");
        AssertJUnit.assertFalse(
                "Aggro.ai must contain at least one key=value property",
                props.isEmpty());
    }

    @Test
    public void burnProfileIsParseable() throws IOException {
        final Map<String, String> props = loadProfileProperties("Burn");
        AssertJUnit.assertFalse(
                "Burn.ai must contain at least one key=value property",
                props.isEmpty());
    }

    @Test
    public void midrangeProfileIsParseable() throws IOException {
        final Map<String, String> props = loadProfileProperties("Midrange");
        AssertJUnit.assertFalse(
                "Midrange.ai must contain at least one key=value property",
                props.isEmpty());
    }

    @Test
    public void controlProfileIsParseable() throws IOException {
        final Map<String, String> props = loadProfileProperties("Control");
        AssertJUnit.assertFalse(
                "Control.ai must contain at least one key=value property",
                props.isEmpty());
    }

    @Test
    public void comboProfileIsParseable() throws IOException {
        final Map<String, String> props = loadProfileProperties("Combo");
        AssertJUnit.assertFalse(
                "Combo.ai must contain at least one key=value property",
                props.isEmpty());
    }
}
