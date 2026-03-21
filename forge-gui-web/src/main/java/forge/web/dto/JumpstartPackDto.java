package forge.web.dto;

import java.util.List;

/**
 * Immutable DTO for Jumpstart pack summaries returned by the pack list API.
 * Each entry represents one pack variant (e.g., "JMP Angels 1" is distinct
 * from "JMP Angels 2").
 */
public class JumpstartPackDto {

    private final String id;
    private final String theme;
    private final String setCode;
    private final int cardCount;
    private final List<String> colors;

    public JumpstartPackDto(final String id, final String theme, final String setCode,
                            final int cardCount, final List<String> colors) {
        this.id = id;
        this.theme = theme;
        this.setCode = setCode;
        this.cardCount = cardCount;
        this.colors = colors;
    }

    public String getId() { return id; }
    public String getTheme() { return theme; }
    public String getSetCode() { return setCode; }
    public int getCardCount() { return cardCount; }
    public List<String> getColors() { return colors; }
}
