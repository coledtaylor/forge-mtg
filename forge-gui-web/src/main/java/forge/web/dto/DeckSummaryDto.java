package forge.web.dto;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.card.ColorSet;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;

/**
 * Flat DTO for deck list items. Provides name, card count, and color identity
 * for deck browser display.
 */
public class DeckSummaryDto {

    public String name;
    public int cardCount;
    public List<String> colors;
    public String path;

    public DeckSummaryDto() {
        // Default constructor for Jackson
    }

    public static DeckSummaryDto from(final Deck deck, final String relativePath) {
        final DeckSummaryDto dto = new DeckSummaryDto();
        dto.name = deck.getName();
        dto.path = relativePath;

        // Sum card counts across all sections
        int count = 0;
        final Set<String> colorSet = new LinkedHashSet<>();

        for (final Map.Entry<DeckSection, CardPool> entry : deck) {
            final CardPool pool = entry.getValue();
            count += pool.countAll();
            for (final Map.Entry<PaperCard, Integer> cardEntry : pool) {
                final ColorSet ci = cardEntry.getKey().getRules().getColorIdentity();
                if (ci.hasWhite()) { colorSet.add("W"); }
                if (ci.hasBlue()) { colorSet.add("U"); }
                if (ci.hasBlack()) { colorSet.add("B"); }
                if (ci.hasRed()) { colorSet.add("R"); }
                if (ci.hasGreen()) { colorSet.add("G"); }
            }
        }

        dto.cardCount = count;
        dto.colors = new ArrayList<>(colorSet);
        return dto;
    }
}
