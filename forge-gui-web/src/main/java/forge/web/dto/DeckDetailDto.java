package forge.web.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.ColorSet;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;

/**
 * Flat DTO for full deck contents. Provides card lists grouped by section
 * (main, sideboard, commander) with Scryfall identifiers for each card.
 */
public class DeckDetailDto {

    public String name;
    public List<DeckCardEntry> main;
    public List<DeckCardEntry> sideboard;
    public List<DeckCardEntry> commander;

    public DeckDetailDto() {
        // Default constructor for Jackson
    }

    public static DeckDetailDto from(final Deck deck) {
        final DeckDetailDto dto = new DeckDetailDto();
        dto.name = deck.getName();
        dto.main = toEntries(deck.getOrCreate(DeckSection.Main));
        dto.sideboard = toEntries(deck.get(DeckSection.Sideboard));
        dto.commander = toEntries(deck.get(DeckSection.Commander));
        return dto;
    }

    private static List<DeckCardEntry> toEntries(final CardPool pool) {
        final List<DeckCardEntry> entries = new ArrayList<>();
        if (pool == null) {
            return entries;
        }
        for (final Map.Entry<PaperCard, Integer> entry : pool) {
            final PaperCard pc = entry.getKey();
            final DeckCardEntry dce = new DeckCardEntry();
            dce.name = pc.getName();
            dce.quantity = entry.getValue();
            final CardEdition edition = StaticData.instance().getEditions().get(pc.getEdition());
            dce.setCode = edition != null ? edition.getScryfallCode() : pc.getEdition().toLowerCase();
            dce.collectorNumber = pc.getCollectorNumber();

            final CardRules rules = pc.getRules();
            dce.manaCost = rules.getManaCost().toString();
            dce.typeLine = rules.getType().toString();
            dce.cmc = rules.getManaCost().getCMC();
            dce.colors = new ArrayList<>();
            final ColorSet ci = rules.getColorIdentity();
            if (ci.hasWhite()) { dce.colors.add("W"); }
            if (ci.hasBlue()) { dce.colors.add("U"); }
            if (ci.hasBlack()) { dce.colors.add("B"); }
            if (ci.hasRed()) { dce.colors.add("R"); }
            if (ci.hasGreen()) { dce.colors.add("G"); }

            entries.add(dce);
        }
        return entries;
    }

    public static class DeckCardEntry {
        public String name;
        public int quantity;
        public String setCode;
        public String collectorNumber;
        public String manaCost;
        public String typeLine;
        public int cmc;
        public List<String> colors;

        public DeckCardEntry() {
            // Default constructor for Jackson
        }
    }
}
