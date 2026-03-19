package forge.web.dto;

import forge.game.spellability.StackItemView;

/**
 * Flat DTO for stack items (spells/abilities on the stack).
 * Uses ID references instead of embedded objects.
 */
public class SpellAbilityDto {

    public int id;
    public String name;
    public String description;
    public int sourceCardId;
    public int activatingPlayerId;

    public SpellAbilityDto() {
        // Default constructor for Jackson
    }

    /**
     * Create a SpellAbilityDto from a StackItemView instance.
     */
    public static SpellAbilityDto from(final StackItemView siv) {
        if (siv == null) {
            return null;
        }

        final SpellAbilityDto dto = new SpellAbilityDto();
        dto.id = siv.getId();
        dto.description = siv.getText();
        dto.sourceCardId = siv.getSourceCard() != null ? siv.getSourceCard().getId() : -1;
        dto.activatingPlayerId = siv.getActivatingPlayer() != null ? siv.getActivatingPlayer().getId() : -1;
        dto.name = siv.getSourceCard() != null ? siv.getSourceCard().getName() : null;
        return dto;
    }
}
