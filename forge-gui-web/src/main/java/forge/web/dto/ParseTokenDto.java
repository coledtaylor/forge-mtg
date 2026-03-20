package forge.web.dto;

import forge.deck.DeckRecognizer;
import forge.item.PaperCard;

/**
 * DTO for a single parse token returned by the deck import parse endpoint.
 * Maps DeckRecognizer.Token fields to JSON-serializable public fields.
 */
public class ParseTokenDto {
    public String type;
    public int quantity;
    public String text;
    public String cardName;
    public String setCode;
    public String collectorNumber;
    public String section;

    public static ParseTokenDto from(final DeckRecognizer.Token token) {
        final ParseTokenDto dto = new ParseTokenDto();
        dto.type = token.getType().name();
        dto.quantity = token.getQuantity();
        dto.text = token.getText();
        if (token.isCardToken() && token.getCard() != null) {
            final PaperCard pc = token.getCard();
            dto.cardName = pc.getName();
            dto.setCode = pc.getEdition();
            dto.collectorNumber = pc.getCollectorNumber();
        }
        dto.section = token.getTokenSection() != null
                ? token.getTokenSection().name() : null;
        return dto;
    }
}
