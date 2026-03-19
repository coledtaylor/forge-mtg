package forge.web.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

/**
 * Flat DTO for player state. Uses ID references for cards instead of
 * embedded objects to avoid circular references.
 */
public class PlayerDto {

    public int id;
    public String name;
    public int life;
    public int poisonCounters;
    public Map<String, Integer> mana;
    public Map<String, List<Integer>> zones;

    public PlayerDto() {
        // Default constructor for Jackson
    }

    /**
     * Create a PlayerDto from a PlayerView instance.
     */
    public static PlayerDto from(final PlayerView pv) {
        if (pv == null) {
            return null;
        }

        final PlayerDto dto = new PlayerDto();
        dto.id = pv.getId();
        dto.name = pv.getName();
        dto.life = pv.getLife();

        // Poison counters
        dto.poisonCounters = pv.getCounters(CounterEnumType.POISON);

        // Mana pool
        dto.mana = new LinkedHashMap<>();
        dto.mana.put(MagicColor.Constant.WHITE, pv.getMana(ManaAtom.WHITE));
        dto.mana.put(MagicColor.Constant.BLUE, pv.getMana(ManaAtom.BLUE));
        dto.mana.put(MagicColor.Constant.BLACK, pv.getMana(ManaAtom.BLACK));
        dto.mana.put(MagicColor.Constant.RED, pv.getMana(ManaAtom.RED));
        dto.mana.put(MagicColor.Constant.GREEN, pv.getMana(ManaAtom.GREEN));
        dto.mana.put(MagicColor.Constant.COLORLESS, pv.getMana(ManaAtom.COLORLESS));

        // Zones with card IDs
        dto.zones = new LinkedHashMap<>();
        for (final ZoneType zone : ZoneType.values()) {
            final FCollectionView<CardView> cards = pv.getCards(zone);
            if (cards != null && !cards.isEmpty()) {
                final List<Integer> cardIds = new ArrayList<>();
                for (final CardView cv : cards) {
                    cardIds.add(cv.getId());
                }
                dto.zones.put(zone.name(), cardIds);
            }
        }

        return dto;
    }
}
