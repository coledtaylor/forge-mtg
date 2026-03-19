package forge.web.dto;

import java.util.ArrayList;
import java.util.List;

import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.ColorSet;
import forge.item.PaperCard;

/**
 * Flat DTO for card search results. Separate from CardDto (game-state DTO)
 * because search results need setCode and collectorNumber for Scryfall image
 * URLs but do not need game-state fields like ownerId or zoneType.
 */
public class CardSearchDto {

    public String name;
    public String manaCost;
    public String typeLine;
    public String oracleText;
    public int power;
    public int toughness;
    public List<String> colors;
    public int cmc;
    public String setCode;
    public String collectorNumber;

    public CardSearchDto() {
        // Default constructor for Jackson
    }

    public static CardSearchDto from(final PaperCard pc) {
        final CardSearchDto dto = new CardSearchDto();
        dto.name = pc.getName();

        final CardRules rules = pc.getRules();
        dto.manaCost = rules.getManaCost().toString();
        dto.typeLine = rules.getType().toString();
        dto.oracleText = rules.getOracleText();
        dto.cmc = rules.getManaCost().getCMC();
        dto.power = rules.getIntPower();
        dto.toughness = rules.getIntToughness();

        final ColorSet colorIdentity = rules.getColorIdentity();
        dto.colors = new ArrayList<>();
        if (colorIdentity.hasWhite()) { dto.colors.add("W"); }
        if (colorIdentity.hasBlue()) { dto.colors.add("U"); }
        if (colorIdentity.hasBlack()) { dto.colors.add("B"); }
        if (colorIdentity.hasRed()) { dto.colors.add("R"); }
        if (colorIdentity.hasGreen()) { dto.colors.add("G"); }

        final CardEdition edition = StaticData.instance().getEditions().get(pc.getEdition());
        dto.setCode = edition != null ? edition.getScryfallCode() : pc.getEdition().toLowerCase();
        dto.collectorNumber = pc.getCollectorNumber();

        return dto;
    }
}
