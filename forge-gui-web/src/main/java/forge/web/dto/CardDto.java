package forge.web.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;

/**
 * Flat DTO for card state. Uses ID references instead of embedded objects
 * to avoid circular references during JSON serialization.
 */
public class CardDto {

    public int id;
    public String name;
    public String manaCost;
    public int power;
    public int toughness;
    public List<String> colors;
    public int ownerId;
    public int controllerId;
    public String zoneType;
    public boolean tapped;
    public Map<String, Integer> counters;
    public List<Integer> attachedToIds;
    public List<Integer> attachmentIds;
    public String type;
    public String oracleText;

    public CardDto() {
        // Default constructor for Jackson
    }

    /**
     * Create a CardDto from a CardView instance.
     * All object references are flattened to IDs.
     */
    public static CardDto from(final CardView cv) {
        if (cv == null) {
            return null;
        }

        final CardDto dto = new CardDto();
        dto.id = cv.getId();
        dto.name = cv.getName();

        final CardStateView state = cv.getCurrentState();
        if (state != null) {
            dto.manaCost = state.getManaCost() != null ? state.getManaCost().toString() : null;
            dto.power = state.getPower();
            dto.toughness = state.getToughness();
            dto.oracleText = state.getOracleText();
            dto.type = state.getType() != null ? state.getType().toString() : null;

            final ColorSet colorSet = state.getColors();
            dto.colors = new ArrayList<>();
            if (colorSet != null) {
                if (colorSet.hasWhite()) { dto.colors.add(MagicColor.Constant.WHITE); }
                if (colorSet.hasBlue()) { dto.colors.add(MagicColor.Constant.BLUE); }
                if (colorSet.hasBlack()) { dto.colors.add(MagicColor.Constant.BLACK); }
                if (colorSet.hasRed()) { dto.colors.add(MagicColor.Constant.RED); }
                if (colorSet.hasGreen()) { dto.colors.add(MagicColor.Constant.GREEN); }
            }
        }

        dto.ownerId = cv.getOwner() != null ? cv.getOwner().getId() : -1;
        dto.controllerId = cv.getController() != null ? cv.getController().getId() : -1;
        dto.zoneType = cv.getZone() != null ? cv.getZone().name() : null;
        dto.tapped = cv.isTapped();

        // Counters
        final Map<CounterType, Integer> viewCounters = cv.getCounters();
        if (viewCounters != null && !viewCounters.isEmpty()) {
            dto.counters = new LinkedHashMap<>();
            for (final Map.Entry<CounterType, Integer> entry : viewCounters.entrySet()) {
                dto.counters.put(entry.getKey().getName(), entry.getValue());
            }
        }

        // Attachments
        final Iterable<CardView> attached = cv.getAllAttachedCards();
        if (attached != null) {
            dto.attachmentIds = new ArrayList<>();
            for (final CardView a : attached) {
                dto.attachmentIds.add(a.getId());
            }
        }

        return dto;
    }
}
