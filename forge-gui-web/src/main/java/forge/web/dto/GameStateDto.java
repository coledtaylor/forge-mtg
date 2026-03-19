package forge.web.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

/**
 * Top-level DTO representing the full game state.
 * All object references are flattened to IDs.
 */
public class GameStateDto {

    public List<PlayerDto> players;
    public List<CardDto> cards;
    public List<SpellAbilityDto> stack;
    public CombatDto combat;
    public String phase;
    public int turn;
    public int activePlayerId;

    public GameStateDto() {
        // Default constructor for Jackson
    }

    /**
     * Create a GameStateDto from a GameView instance.
     * Collects all visible cards across all player zones.
     */
    public static GameStateDto from(final GameView gv) {
        if (gv == null) {
            return null;
        }

        final GameStateDto dto = new GameStateDto();

        // Players
        dto.players = new ArrayList<>();
        final Set<Integer> seenCardIds = new HashSet<>();
        dto.cards = new ArrayList<>();

        final FCollectionView<PlayerView> playerViews = gv.getPlayers();
        if (playerViews != null) {
            for (final PlayerView pv : playerViews) {
                dto.players.add(PlayerDto.from(pv));

                // Collect all visible cards from all zones
                for (final ZoneType zone : ZoneType.values()) {
                    final FCollectionView<CardView> cards = pv.getCards(zone);
                    if (cards != null) {
                        for (final CardView cv : cards) {
                            if (!seenCardIds.contains(cv.getId())) {
                                seenCardIds.add(cv.getId());
                                dto.cards.add(CardDto.from(cv));
                            }
                        }
                    }
                }
            }
        }

        // Stack
        dto.stack = new ArrayList<>();
        final FCollectionView<StackItemView> stackItems = gv.getStack();
        if (stackItems != null) {
            for (final StackItemView siv : stackItems) {
                dto.stack.add(SpellAbilityDto.from(siv));
            }
        }

        // Combat
        dto.combat = CombatDto.from(gv.getCombat());

        // Phase and turn
        dto.phase = gv.getPhase() != null ? gv.getPhase().name() : null;
        dto.turn = gv.getTurn();
        dto.activePlayerId = gv.getPlayerTurn() != null ? gv.getPlayerTurn().getId() : -1;

        return dto;
    }
}
