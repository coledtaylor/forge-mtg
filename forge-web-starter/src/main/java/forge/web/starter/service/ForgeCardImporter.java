package forge.web.starter.service;

import forge.web.starter.entity.Card;
import forge.web.starter.repository.CardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Imports cards from Forge cardsfolder text files into the database.
 * <p>
 * This runs on application startup and:
 * 1. Scans the Forge cardsfolder directory
 * 2. Parses each .txt file
 * 3. Creates or updates cards in the database
 * 4. Skips cards that already exist (based on name + edition)
 * <p>
 * Forge card format example:
 * <pre>
 * Name:Lightning Bolt
 * ManaCost:R
 * Types:Instant
 * A:SP$ DealDamage | ValidTgts$ Creature,Player | TgtPrompt$ Select target | NumDmg$ 3
 * Oracle:Lightning Bolt deals 3 damage to any target.
 * </pre>
 */
@Component
public class ForgeCardImporter implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ForgeCardImporter.class);

    private final CardRepository cardRepository;
    private final ForgeCardImporter self;

    @Value("${forge.cards.folder}")
    private String cardsFolderPath;

    @Value("${forge.cards.import-on-startup}")
    private boolean importOnStartup;

    @Value("${forge.cards.batch-size}")
    private int batchSize;

    public ForgeCardImporter(CardRepository cardRepository, @Autowired(required = false) ForgeCardImporter self) {
        this.cardRepository = cardRepository;
        this.self = self;
    }

    @Override
    public void run(String... args) {
        if (!importOnStartup) {
            log.info("Card import disabled. Set forge.cards.import-on-startup=true to enable.");
            return;
        }

        log.info("Starting Forge card import from: {}", cardsFolderPath);
        long startTime = System.currentTimeMillis();

        try {
            self.importCards();
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Card import completed in {} seconds", duration / 1000.0);
        } catch (Exception e) {
            log.error("❌ Card import failed", e);
        }
    }

    /**
     * Import all cards from the cardsfolder directory.
     */
    @Transactional
    public void importCards() throws IOException {
        Path cardsFolder = Paths.get(cardsFolderPath);

        if (!Files.exists(cardsFolder)) {
            log.warn("Cards folder not found: {}. Skipping import.", cardsFolder.toAbsolutePath());
            log.info("Hint: Set forge.cards.folder in application.yml to point to your Forge cardsfolder");
            return;
        }

        log.info("Scanning cardsfolder: {}", cardsFolder.toAbsolutePath());

        List<Card> batch = new ArrayList<>();
        int totalProcessed = 0;
        int totalCreated = 0;
        int totalSkipped = 0;

        // Walk through all subdirectories (a, b, c, ..., z)
        try (Stream<Path> paths = Files.walk(cardsFolder, 2)) {
            List<Path> cardFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .toList();

            log.info("Found {} card files", cardFiles.size());

            for (Path cardFile : cardFiles) {
                try {
                    Card card = parseCardFile(cardFile);
                    if (card != null) {
                        // Check if card already exists
                        String edition = card.getEdition() != null ? card.getEdition() : "Unknown";
                        boolean exists = cardRepository.findByNameAndEdition(card.getName(), edition).isPresent();

                        if (!exists) {
                            batch.add(card);
                            totalCreated++;

                            // Save in batches for better performance
                            if (batch.size() >= batchSize) {
                                cardRepository.saveAll(batch);
                                log.info("Saved batch of {} cards. Total: {} created, {} skipped",
                                        batch.size(), totalCreated, totalSkipped);
                                batch.clear();
                            }
                        } else {
                            totalSkipped++;
                        }

                        totalProcessed++;

                        if (totalProcessed % 1000 == 0) {
                            log.info("Progress: {} cards processed ({} created, {} skipped)",
                                    totalProcessed, totalCreated, totalSkipped);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse card file: {} - {}", cardFile.getFileName(), e.getMessage());
                }
            }

            // Save remaining batch
            if (!batch.isEmpty()) {
                cardRepository.saveAll(batch);
                log.info("Saved final batch of {} cards", batch.size());
            }

            log.info("📊 Import Summary:");
            log.info("   Total processed: {}", totalProcessed);
            log.info("   Created: {}", totalCreated);
            log.info("   Skipped (already exist): {}", totalSkipped);
            log.info("   Database total: {}", cardRepository.count());

        }
    }

    /**
     * Parse a Forge card text file into a Card entity
     *
     * @param cardFile Path to the card .txt file
     * @return Card entity or null if parsing fails
     */
    private Card parseCardFile(Path cardFile) throws IOException {
        List<String> lines = Files.readAllLines(cardFile);
        if (lines.isEmpty()) {
            return null;
        }

        Card card = new Card();
        StringBuilder forgeScriptBuilder = new StringBuilder();
        StringBuilder oracleTextBuilder = new StringBuilder();
        List<String> keywordAbilities = new ArrayList<>();

        for (String line : lines) {
            forgeScriptBuilder.append(line).append("\n");

            if (line.startsWith("Name:")) {
                card.setName(line.substring(5).trim());
            } else if (line.startsWith("ManaCost:")) {
                String manaCost = line.substring(9).trim();
                card.setManaCost(manaCost);
                card.setColors(extractColors(manaCost));
            } else if (line.startsWith("Types:")) {
                card.setType(line.substring(6).trim());
            } else if (line.startsWith("PT:")) {
                String pt = line.substring(3).trim();
                parsePowerToughness(card, pt);
            } else if (line.startsWith("Oracle:")) {
                oracleTextBuilder.append(line.substring(7).trim());
            } else if (line.startsWith("K:")) {
                // Keyword ability
                keywordAbilities.add(line.substring(2).trim());
            } else if (line.startsWith("R:")) {
                // Rarity
                card.setRarity(extractRarity(line.substring(2).trim()));
            }
        }

        // Set forge script
        card.setForgeScript(forgeScriptBuilder.toString());

        // Set oracle text
        if (!oracleTextBuilder.isEmpty()) {
            card.setOracleText(oracleTextBuilder.toString());
        }

        // Set abilities
        if (!keywordAbilities.isEmpty()) {
            card.setAbilities(String.join(", ", keywordAbilities));
        }

        // Set edition from file structure (cardsfolder/a/card_name.txt -> edition might be in metadata)
        // For now, use "Forge" - you can enhance this by parsing edition metadata
        card.setEdition("Forge");

        // Validate required fields
        if (card.getName() == null || card.getType() == null) {
            return null;
        }

        // Validate field lengths to prevent database errors
        validateFieldLengths(card, cardFile);

        return card;
    }

    /**
     * Validate and truncate fields that might exceed database column limits
     */
    private void validateFieldLengths(Card card, Path cardFile) {
        if (card.getName() != null && card.getName().length() > 255) {
            log.warn("Card name too long ({}), truncating: {}", card.getName().length(), cardFile.getFileName());
            card.setName(card.getName().substring(0, 255));
        }
        if (card.getType() != null && card.getType().length() > 100) {
            log.warn("Card type too long ({}), truncating: {}", card.getType().length(), cardFile.getFileName());
            card.setType(card.getType().substring(0, 100));
        }
        if (card.getManaCost() != null && card.getManaCost().length() > 50) {
            log.debug("Mana cost too long ({}), truncating", card.getManaCost().length());
            card.setManaCost(card.getManaCost().substring(0, 50));
        }
        if (card.getColors() != null && card.getColors().length() > 20) {
            log.debug("Colors too long ({}), truncating", card.getColors().length());
            card.setColors(card.getColors().substring(0, 20));
        }
        if (card.getRarity() != null && card.getRarity().length() > 500) {
            log.debug("Rarity too long ({}), truncating", card.getRarity().length());
            card.setRarity(card.getRarity().substring(0, 500));
        }
        if (card.getEdition() != null && card.getEdition().length() > 50) {
            log.debug("Edition too long ({}), truncating", card.getEdition().length());
            card.setEdition(card.getEdition().substring(0, 50));
        }
        if (card.getCollectorNumber() != null && card.getCollectorNumber().length() > 100) {
            log.debug("Collector number too long ({}), truncating", card.getCollectorNumber().length());
            card.setCollectorNumber(card.getCollectorNumber().substring(0, 100));
        }
        if (card.getArtist() != null && card.getArtist().length() > 100) {
            log.debug("Artist too long ({}), truncating", card.getArtist().length());
            card.setArtist(card.getArtist().substring(0, 100));
        }
        if (card.getImageUrl() != null && card.getImageUrl().length() > 500) {
            log.debug("Image URL too long ({}), truncating", card.getImageUrl().length());
            card.setImageUrl(card.getImageUrl().substring(0, 500));
        }
        if (card.getAbilities() != null && card.getAbilities().length() > 1000) {
            log.debug("Abilities too long ({}), truncating", card.getAbilities().length());
            card.setAbilities(card.getAbilities().substring(0, 1000));
        }
    }

    /**
     * Extract colors from mana cost
     * Example: "{2}{U}{U}" -> "U"
     * Example: "{R}{G}" -> "R,G"
     */
    private String extractColors(String manaCost) {
        if (manaCost == null || manaCost.isEmpty()) {
            return null;
        }

        List<String> colors = new ArrayList<>();
        if (manaCost.contains("W")) colors.add("W");
        if (manaCost.contains("U")) colors.add("U");
        if (manaCost.contains("B")) colors.add("B");
        if (manaCost.contains("R")) colors.add("R");
        if (manaCost.contains("G")) colors.add("G");

        return colors.isEmpty() ? null : String.join(",", colors);
    }

    /**
     * Parse power/toughness
     * Example: "3/3" -> power=3, toughness=3
     */
    private void parsePowerToughness(Card card, String pt) {
        if (pt.contains("/")) {
            String[] parts = pt.split("/");
            try {
                card.setPower(Integer.parseInt(parts[0].trim()));
                if (parts.length > 1) {
                    card.setToughness(Integer.parseInt(parts[1].trim()));
                }
            } catch (NumberFormatException e) {
                // Some cards have */*, X/X, etc. - silently ignore
            }
        }
    }

    /**
     * Extract rarity from Rarity line
     * Example: "R:Common" -> "Common"
     * Example: "Rarity:Uncommon" -> "Uncommon"
     */
    private String extractRarity(String rarityLine) {
        if (rarityLine == null || rarityLine.isEmpty()) {
            return null;
        }

        // Rarity format can vary, this is a simplified version
        String rarity = rarityLine;

        // Handle "R:Common" or "Rarity:Common" format
        if (rarityLine.contains(":")) {
            String[] parts = rarityLine.split(":", 2);
            if (parts.length > 1) {
                rarity = parts[1].trim();
            }
        }

        // Truncate if too long (max 100 characters based on entity)
        if (rarity.length() > 500) {
            log.debug("Rarity too long ({}), truncating: {}", rarity.length(), rarity);
            rarity = rarity.substring(0, 500);
        }

        return rarity;
    }
}

