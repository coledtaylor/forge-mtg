package forge.web.starter.model;

/**
 * Data Transfer Object (DTO) for a simple card.
 * <p>
 * DTOs are used to transfer data between the API and clients.
 * They are serialized to/from JSON automatically by Spring.
 */
public class CardDTO {
    private Long id;
    private String name;
    private String type;      // "Creature", "Instant", "Sorcery", etc.
    private int manaCost;
    private int power;        // For creatures
    private int toughness;    // For creatures
    private String text;

    public CardDTO() {
    }

    public CardDTO(Long id, String name, String type, int manaCost, int power, int toughness, String text) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.manaCost = manaCost;
        this.power = power;
        this.toughness = toughness;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getManaCost() {
        return manaCost;
    }

    public void setManaCost(int manaCost) {
        this.manaCost = manaCost;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getToughness() {
        return toughness;
    }

    public void setToughness(int toughness) {
        this.toughness = toughness;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
