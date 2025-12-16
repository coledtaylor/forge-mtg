package forge.web.starter.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * JPA Entity for storing Magic: The Gathering cards in a database.
 * This replaces the in-memory ArrayList approach for cloud-ready storage.
 * <p>
 * Key features:
 * - Persistent storage (survives restarts)
 * - Indexed for fast queries
 * - Supports pagination
 * - Ready for cloud deployment
 */
@Entity
@Table(name = "cards", indexes = {
    @Index(name = "idx_card_name", columnList = "name"),
    @Index(name = "idx_card_type", columnList = "type"),
    @Index(name = "idx_card_colors", columnList = "colors"),
    @Index(name = "idx_card_edition", columnList = "edition")
})
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(length = 50)
    private String manaCost;  // e.g., "{2}{U}{U}"

    @Column(length = 10)
    private String colors;    // e.g., "U,B" for Blue/Black

    @Column(length = 20)
    private String rarity;    // Common, Uncommon, Rare, Mythic

    private Integer power;
    private Integer toughness;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(length = 10)
    private String edition;   // e.g., "M21", "DOM"

    @Column(length = 20)
    private String collectorNumber;

    @Column(length = 500)
    private String imageUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Card() {
    }

    public Card(String name, String type, String manaCost, Integer power, Integer toughness, String text) {
        this.name = name;
        this.type = type;
        this.manaCost = manaCost;
        this.power = power;
        this.toughness = toughness;
        this.text = text;
    }

    // Getters and Setters
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

    public String getManaCost() {
        return manaCost;
    }

    public void setManaCost(String manaCost) {
        this.manaCost = manaCost;
    }

    public String getColors() {
        return colors;
    }

    public void setColors(String colors) {
        this.colors = colors;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public Integer getPower() {
        return power;
    }

    public void setPower(Integer power) {
        this.power = power;
    }

    public Integer getToughness() {
        return toughness;
    }

    public void setToughness(Integer toughness) {
        this.toughness = toughness;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getCollectorNumber() {
        return collectorNumber;
    }

    public void setCollectorNumber(String collectorNumber) {
        this.collectorNumber = collectorNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Card{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", manaCost='" + manaCost + '\'' +
                ", power=" + power +
                ", toughness=" + toughness +
                '}';
    }
}
