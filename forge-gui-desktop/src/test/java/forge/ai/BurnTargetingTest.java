package forge.ai;

import forge.ai.ability.DamageDealAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.Collections;

/**
 * Tests for burn-specific AI targeting logic controlled by the Burn.ai profile
 * (BURN_FACE_DAMAGE_PRIORITY, BURN_LETHAL_CHECK, BURN_REMOVE_LIFELINK_CREATURES).
 *
 * All new AiProps default to false, so existing behavior is preserved when they
 * are not set (verified by testDefaultProfilePreservesExistingBehavior).
 */
public class BurnTargetingTest extends AITest {

    /**
     * Subclass that exposes the protected shouldTgtP method for direct unit testing.
     */
    private static class TestableDamageDealAi extends DamageDealAi {
        public boolean callShouldTgtP(Player comp, SpellAbility sa, int d, boolean noPrevention) {
            return shouldTgtP(comp, sa, d, noPrevention);
        }
    }

    private static final TestableDamageDealAi TESTABLE_AI = new TestableDamageDealAi();

    /** Helper: build a SpellAbility with NumDmg=dmg sourced from a Mountain in AI's hand. */
    private SpellAbility makeBurnSa(Game game, Player ai, int dmg) {
        Card source = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        SpellAbility sa = new SpellAbility.EmptySa(ApiType.DealDamage, source, ai);
        sa.putParam("NumDmg", String.valueOf(dmg));
        return sa;
    }

    // ---------------------------------------------------------------------------
    // Test 1: BURN_FACE_DAMAGE_PRIORITY=true — shouldTgtP returns true even at 20 life
    // ---------------------------------------------------------------------------
    @Test
    public void testBurnFacePriorityTargetsPlayer() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        // Opponent is at full life; no creatures on board
        opponent.setLife(20, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        // Activate the Burn profile props directly on the AI lobby player
        LobbyPlayerAi aiLobbyPlayer = (LobbyPlayerAi) ai.getLobbyPlayer();
        aiLobbyPlayer.setAiProfile("Burn");

        SpellAbility sa = makeBurnSa(game, ai, 3);

        // With BURN_FACE_DAMAGE_PRIORITY enabled in the Burn profile, shouldTgtP must return true
        // even though opponent is at 20 life (well above the < 5 threshold of default logic).
        boolean result = TESTABLE_AI.callShouldTgtP(ai, sa, 3, false);

        AssertJUnit.assertTrue(
                "Burn profile with BURN_FACE_DAMAGE_PRIORITY should always target opponent player",
                result);
    }

    // ---------------------------------------------------------------------------
    // Test 2: Default profile — existing behavior preserved (shouldTgtP returns false at 20 life)
    // ---------------------------------------------------------------------------
    @Test
    public void testDefaultProfilePreservesExistingBehavior() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        opponent.setLife(20, null);

        // AI has a small hand (1 card) so probability-based logic doesn't trigger
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        // Use default profile (null profile → all AiProps at their defaults = false for burn props)
        LobbyPlayerAi aiLobbyPlayer = (LobbyPlayerAi) ai.getLobbyPlayer();
        aiLobbyPlayer.setAiProfile("Default");

        SpellAbility sa = makeBurnSa(game, ai, 3);

        // With default profile and opponent at 20 life, existing logic returns false
        // (damage doesn't drop opponent below 5, hand size is small, sorcery-speed in MAIN2 without excess)
        boolean result = TESTABLE_AI.callShouldTgtP(ai, sa, 3, false);

        AssertJUnit.assertFalse(
                "Default profile should not force face targeting at 20 opponent life with small hand",
                result);
    }

    // ---------------------------------------------------------------------------
    // Test 3: BURN_LETHAL_CHECK — enough burn in hand to finish opponent → go face
    // ---------------------------------------------------------------------------
    @Test
    public void testLethalCheckAlwaysTargetsPlayer() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        // Opponent at low life
        opponent.setLife(6, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        LobbyPlayerAi aiLobbyPlayer = (LobbyPlayerAi) ai.getLobbyPlayer();
        aiLobbyPlayer.setAiProfile("Burn");

        // Add a second burn spell in hand: Lightning Bolt (3 damage)
        // The SA itself deals 3 damage, so total = 3 (SA) + 3 (hand card) = 6 >= 6 life
        Card secondBolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);

        SpellAbility sa = makeBurnSa(game, ai, 3);

        boolean result = TESTABLE_AI.callShouldTgtP(ai, sa, 3, false);

        AssertJUnit.assertTrue(
                "BURN_LETHAL_CHECK: enough burn in hand to lethal should target the player",
                result);
    }

    // ---------------------------------------------------------------------------
    // Test 4: BURN_REMOVE_LIFELINK_CREATURES overrides BURN_FACE_DAMAGE_PRIORITY
    //         when a killable lifelink creature is present
    // ---------------------------------------------------------------------------
    @Test
    public void testLifelinkCreatureOverridesFacePriority() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        opponent.setLife(20, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        // Put a 2/2 lifelink creature on opponent's side (toughness 2, killable by 3 damage).
        // Ajani's Pridemate is a 2/2; we add Lifelink via addChangedCardKeywords so it is
        // definitely present regardless of the card's printed text.
        Card lifelinkCreature = addCard("Ajani's Pridemate", opponent);
        lifelinkCreature.addChangedCardKeywords(
                Collections.singletonList(Keyword.LIFELINK.toString()),
                null, false, game.getNextTimestamp(), null);

        game.getAction().checkStateEffects(true);

        LobbyPlayerAi aiLobbyPlayer = (LobbyPlayerAi) ai.getLobbyPlayer();
        aiLobbyPlayer.setAiProfile("Burn");

        SpellAbility sa = makeBurnSa(game, ai, 3);

        // Even though BURN_FACE_DAMAGE_PRIORITY is true in the Burn profile,
        // BURN_REMOVE_LIFELINK_CREATURES is checked first and should return false
        // (delegate to creature targeting) when a killable lifelink creature exists.
        boolean result = TESTABLE_AI.callShouldTgtP(ai, sa, 3, false);

        AssertJUnit.assertFalse(
                "BURN_REMOVE_LIFELINK_CREATURES: should not go face when a killable lifelink creature is present",
                result);
    }
}
