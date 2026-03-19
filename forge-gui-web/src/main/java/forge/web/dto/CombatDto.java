package forge.web.dto;

import java.util.ArrayList;
import java.util.List;

import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.player.PlayerView;
import forge.util.collect.FCollection;

/**
 * Flat DTO for combat state. Uses ID references instead of embedded objects.
 */
public class CombatDto {

    public List<AttackerInfo> attackers;

    public CombatDto() {
        // Default constructor for Jackson
    }

    /**
     * Create a CombatDto from a CombatView instance.
     *
     * @return null if cv is null
     */
    public static CombatDto from(final CombatView cv) {
        if (cv == null) {
            return null;
        }

        final CombatDto dto = new CombatDto();
        dto.attackers = new ArrayList<>();

        for (final CardView attacker : cv.getAttackers()) {
            final AttackerInfo info = new AttackerInfo();
            info.cardId = attacker.getId();

            final GameEntityView defender = cv.getDefender(attacker);
            if (defender instanceof PlayerView) {
                info.defendingPlayerId = defender.getId();
            } else {
                // Planeswalker or other non-player defender -- use the card ID
                info.defendingPlayerId = defender != null ? defender.getId() : -1;
            }

            final FCollection<CardView> blockers = cv.getBlockers(attacker);
            info.blockerCardIds = new ArrayList<>();
            if (blockers != null) {
                for (final CardView blocker : blockers) {
                    info.blockerCardIds.add(blocker.getId());
                }
            }

            dto.attackers.add(info);
        }

        return dto;
    }

    /**
     * Information about a single attacker in combat.
     */
    public static class AttackerInfo {
        public int cardId;
        public int defendingPlayerId;
        public List<Integer> blockerCardIds;

        public AttackerInfo() {
            // Default constructor for Jackson
        }
    }
}
