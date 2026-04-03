package forge.ai.ability;

import forge.ai.AiProps;
import forge.ai.AiProfileUtil;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public abstract class DamageAiBase extends SpellAbilityAi {
    protected boolean avoidTargetP(final Player comp, final SpellAbility sa) {
        Player enemy = comp.getWeakestOpponent();
        // Logic for cards that damage owner, like Fireslinger
        // Do not target a player if they aren't below 75% of our health.
        // Unless Lifelink will cancel the damage to us
        Card hostcard = sa.getHostCard();
        boolean lifelink = hostcard.hasKeyword(Keyword.LIFELINK);
        if (!lifelink) {
            for (Card ench : hostcard.getEnchantedBy()) {
                // Treat cards enchanted by older cards with "when enchanted creature deals damage, gain life" as if they had lifelink.
                if (ench.hasSVar("LikeLifeLink")) {
                    if ("True".equals(ench.getSVar("LikeLifeLink"))) {
                        lifelink = true;
                    }
                }
            }
        }
        if ("SelfDamage".equals(sa.getParam("AILogic"))) {
            if (comp.getLife() * 0.75 < enemy.getLife()) {
                return !lifelink;
            }
        }
        return false;
    }

    protected boolean shouldTgtP(final Player comp, final SpellAbility sa, final int d, final boolean noPrevention) {
        int restDamage = d;
        final Game game = comp.getGame();
        Player enemy = comp.getWeakestOpponent();
        boolean dmgByCardsInHand = false;
        Card hostcard = sa.getHostCard();

        if ("X".equals(sa.getParam("NumDmg")) && hostcard != null && sa.hasSVar(sa.getParam("NumDmg")) &&
                sa.getSVar(sa.getParam("NumDmg")).equals("TargetedPlayer$CardsInHand")) {
            dmgByCardsInHand = true;
        }
        // Not sure if type choice implemented for the AI yet but it should at least recognize this spell hits harder on larger enemy hand size
        if ("Blood Oath".equals(hostcard.getName())) {
            dmgByCardsInHand = true;
        }

        if (!sa.canTarget(enemy)) {
            return false;
        }
        if (sa.getTargets() != null && sa.getTargets().contains(enemy)) {
            return false;
        }

        // If the opponent will gain life (ex. Fiery Justice), not beneficial unless life gain is harmful or ignored
        if ("OpponentGainLife".equals(sa.getParam("AILogic")) && ComputerUtil.lifegainPositive(enemy, hostcard)) {
            return false;
        }

        // Benefits hitting players?
        // If has triggered ability on dealing damage to an opponent, go for it!
        for (Trigger trig : hostcard.getTriggers()) {
            if (trig.getMode() == TriggerType.DamageDone) {
                if ("Opponent".equals(trig.getParam("ValidTarget"))
                        && !"True".equals(trig.getParam("CombatDamage"))) {
                    return true;
                }
            }
        }

        if (avoidTargetP(comp, sa)) {
            return false;
        }

        if (!enemy.canLoseLife()) {
            return false;
        }

        if (!noPrevention) {
            restDamage = ComputerUtilCombat.predictDamageTo(enemy, restDamage, hostcard, false);
        } else {
            restDamage = enemy.staticReplaceDamage(restDamage, hostcard, false);
        }
        if (restDamage == 0) {
            return false;
        }

        // Burn profile: check burn-specific AiProps before falling through to default logic.
        // These only activate when explicitly set to true in an AI profile; defaults are false,
        // so existing behavior is completely unchanged for the Default and other profiles.

        // BURN_LETHAL_CHECK: if total damage from instant/sorcery spells in hand >= enemy life, go face
        if (AiProfileUtil.getBoolProperty(comp, AiProps.BURN_LETHAL_CHECK)) {
            int totalBurnInHand = restDamage; // count the current spell's damage too
            for (Card c : comp.getCardsIn(ZoneType.Hand)) {
                if (c.equals(sa.getHostCard())) {
                    continue; // already counted via restDamage
                }
                if (c.isInstant() || c.isSorcery()) {
                    for (SpellAbility handSa : c.getSpellAbilities()) {
                        if (handSa.getApi() == ApiType.DealDamage) {
                            String numDmgParam = handSa.getParam("NumDmg");
                            if (numDmgParam != null) {
                                try {
                                    totalBurnInHand += Integer.parseInt(numDmgParam);
                                } catch (NumberFormatException e) {
                                    // non-literal X spells — skip, can't trivially evaluate
                                }
                            }
                            break;
                        }
                    }
                }
            }
            if (totalBurnInHand >= enemy.getLife()) {
                return true; // enough burn in hand to lethal — go face
            }
        }

        // BURN_REMOVE_LIFELINK_CREATURES: if a killable lifelink creature is present, don't go face
        // (creature targeting logic will handle it)
        if (AiProfileUtil.getBoolProperty(comp, AiProps.BURN_REMOVE_LIFELINK_CREATURES)) {
            for (Player opp : comp.getOpponents()) {
                for (Card c : opp.getCreaturesInPlay()) {
                    if (c.hasKeyword(Keyword.LIFELINK) && c.getNetToughness() <= restDamage) {
                        return false; // there is a killable lifelink creature — let creature targeting handle it
                    }
                }
            }
        }

        // BURN_REMOVE_TAX_CREATURES: if a killable creature taxes our noncreature spells
        // (e.g. Thalia, Vryn Wingmare), removing it saves more mana than the face damage costs.
        // Only divert if we have 2+ burn spells left in hand (so the mana savings matter).
        if (AiProfileUtil.getBoolProperty(comp, AiProps.BURN_REMOVE_TAX_CREATURES)) {
            int burnSpellsInHand = 0;
            for (final Card c : comp.getCardsIn(ZoneType.Hand)) {
                if (c.equals(sa.getHostCard())) {
                    continue;
                }
                if (c.isInstant() || c.isSorcery()) {
                    for (final SpellAbility handSa : c.getSpellAbilities()) {
                        if (handSa.getApi() == ApiType.DealDamage) {
                            burnSpellsInHand++;
                            break;
                        }
                    }
                }
            }
            if (burnSpellsInHand >= 2) {
                for (final Player opp : comp.getOpponents()) {
                    for (final Card c : opp.getCreaturesInPlay()) {
                        if (c.getNetToughness() > restDamage) {
                            continue; // can't kill it with this spell
                        }
                        for (final StaticAbility stab : c.getStaticAbilities()) {
                            if (stab.getMode().contains(StaticAbilityMode.RaiseCost)) {
                                final String validCard = stab.getParam("ValidCard");
                                if (validCard != null && validCard.contains("nonCreature")) {
                                    return false; // killable tax creature — let creature targeting handle it
                                }
                            }
                        }
                    }
                }
            }
        }

        // BURN_FACE_DAMAGE_PRIORITY: always go face when this flag is set
        if (AiProfileUtil.getBoolProperty(comp, AiProps.BURN_FACE_DAMAGE_PRIORITY)) {
            return true;
        }

        final CardCollectionView hand = comp.getCardsIn(ZoneType.Hand);

        if ((enemy.getLife() - restDamage) < 5) {
            // drop the human to less than 5 life
            return true;
        }

        if (sa.isSpell()) {
            PhaseHandler phase = game.getPhaseHandler();
            // If this is a spell, cast it instead of discarding
            if ((phase.is(PhaseType.END_OF_TURN) || phase.is(PhaseType.MAIN2))
                    && phase.isPlayerTurn(comp) && hand.size() > comp.getMaxHandSize()) {
                return true;
            }

            // chance to burn player based on current hand size
            if (hand.size() > 2) {
                float value = 0;
                if (isSorcerySpeed(sa, comp)) {
                    //lower chance for sorcery as other spells may be cast in main2
                    if (phase.isPlayerTurn(comp) && phase.is(PhaseType.MAIN2)) {
                        value = 1.0f * restDamage / enemy.getLife();
                    }
                } else {
                    // If Sudden Impact type spell, and can hit at least 3 cards during draw phase
                    // have a 100% chance to go for it, enemy hand will only lose cards over time!
                    // But if 3 or less cards, use normal rules, just in case enemy starts holding card or plays a draw spell or we need mana for other instants.
                    if (phase.isPlayerTurn(enemy)) {
                        if (dmgByCardsInHand
                                && (phase.is(PhaseType.DRAW))
                                && (enemy.getCardsIn(ZoneType.Hand).size() > 3)) {
                            value = 1;
                        } else if (phase.is(PhaseType.END_OF_TURN)
                                || ((dmgByCardsInHand && phase.getPhase().isAfter(PhaseType.UPKEEP)))) {
                            value = 1.5f * restDamage / enemy.getLife();
                        }
                    }
                }
                if (value > 0) { //more likely to burn with larger hand
                    for (int i = 3; i < hand.size(); i++) {
                        value *= 1.1f;
                    }
                }
                if (value < 0.2f) { //hard floor to reduce ridiculous odds for instants over time
                    return false;
                }
                final float chance = MyRandom.getRandom().nextFloat();
                return chance < value;
            }
        }

        return false;
    }
}
