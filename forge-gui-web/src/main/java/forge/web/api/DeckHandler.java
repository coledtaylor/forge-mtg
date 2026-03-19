package forge.web.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.javalin.http.Context;

import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.io.DeckSerializer;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.web.dto.DeckDetailDto;
import forge.web.dto.DeckSummaryDto;

/**
 * REST handler for deck CRUD operations. Decks are persisted as .dck files
 * in the constructed deck directory using Forge's DeckSerializer.
 */
public final class DeckHandler {

    private DeckHandler() { }

    private static File getDecksDir() {
        return new File(ForgeConstants.DECK_CONSTRUCTED_DIR);
    }

    public static void list(final Context ctx) {
        final File decksDir = getDecksDir();
        final List<DeckSummaryDto> result = new ArrayList<>();

        if (decksDir.exists() && decksDir.isDirectory()) {
            collectDecks(decksDir, decksDir, result);
        }

        ctx.json(result);
    }

    private static void collectDecks(final File dir, final File baseDir,
                                     final List<DeckSummaryDto> result) {
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                collectDecks(file, baseDir, result);
            } else if (file.getName().endsWith(".dck")) {
                final Deck deck = DeckSerializer.fromFile(file);
                if (deck != null) {
                    final String relativePath = baseDir.toPath()
                            .relativize(file.toPath()).toString().replace('\\', '/');
                    result.add(DeckSummaryDto.from(deck, relativePath));
                }
            }
        }
    }

    public static void create(final Context ctx) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> body = ctx.bodyAsClass(Map.class);
        final String name = (String) body.get("name");

        if (name == null || name.trim().isEmpty()) {
            ctx.status(400).json(Map.of("error", "Deck name is required"));
            return;
        }

        final Deck deck = new Deck(name.trim());

        final String format = (String) body.get("format");
        if (format != null && !format.isEmpty()) {
            deck.setComment(format);
        }

        final File decksDir = getDecksDir();
        decksDir.mkdirs();
        DeckSerializer.writeDeck(deck, new File(decksDir, deck.getBestFileName() + ".dck"));

        ctx.status(201).json(DeckDetailDto.from(deck));
    }

    public static void get(final Context ctx) {
        final String name = ctx.pathParam("name");
        final File deckFile = findDeckFile(name);

        if (deckFile == null) {
            ctx.status(404).json(Map.of("error", "Deck not found"));
            return;
        }

        final Deck deck = DeckSerializer.fromFile(deckFile);
        if (deck == null) {
            ctx.status(404).json(Map.of("error", "Deck not found"));
            return;
        }

        ctx.json(DeckDetailDto.from(deck));
    }

    @SuppressWarnings("unchecked")
    public static void update(final Context ctx) {
        final String name = ctx.pathParam("name");
        final File deckFile = findDeckFile(name);

        if (deckFile == null) {
            ctx.status(404).json(Map.of("error", "Deck not found"));
            return;
        }

        final Deck deck = DeckSerializer.fromFile(deckFile);
        if (deck == null) {
            ctx.status(404).json(Map.of("error", "Deck not found"));
            return;
        }

        final Map<String, Object> body = ctx.bodyAsClass(Map.class);

        // Update main section
        final Map<String, Integer> mainCards = (Map<String, Integer>) body.get("main");
        if (mainCards != null) {
            deck.getOrCreate(DeckSection.Main).clear();
            for (final Map.Entry<String, Integer> entry : mainCards.entrySet()) {
                final PaperCard card = FModel.getMagicDb().getCommonCards().getCard(entry.getKey());
                if (card != null) {
                    deck.getOrCreate(DeckSection.Main).add(card, entry.getValue());
                }
            }
        }

        // Update sideboard section
        final Map<String, Integer> sideCards = (Map<String, Integer>) body.get("sideboard");
        if (sideCards != null) {
            deck.getOrCreate(DeckSection.Sideboard).clear();
            for (final Map.Entry<String, Integer> entry : sideCards.entrySet()) {
                final PaperCard card = FModel.getMagicDb().getCommonCards().getCard(entry.getKey());
                if (card != null) {
                    deck.getOrCreate(DeckSection.Sideboard).add(card, entry.getValue());
                }
            }
        }

        // Update commander section
        final Map<String, Integer> commanderCards = (Map<String, Integer>) body.get("commander");
        if (commanderCards != null) {
            deck.getOrCreate(DeckSection.Commander).clear();
            for (final Map.Entry<String, Integer> entry : commanderCards.entrySet()) {
                final PaperCard card = FModel.getMagicDb().getCommonCards().getCard(entry.getKey());
                if (card != null) {
                    deck.getOrCreate(DeckSection.Commander).add(card, entry.getValue());
                }
            }
        }

        DeckSerializer.writeDeck(deck, deckFile);
        ctx.json(DeckDetailDto.from(deck));
    }

    public static void delete(final Context ctx) {
        final String name = ctx.pathParam("name");
        final File deckFile = findDeckFile(name);

        if (deckFile == null) {
            ctx.status(404).json(Map.of("error", "Deck not found"));
            return;
        }

        deckFile.delete();
        ctx.status(204);
    }

    /**
     * Finds a .dck file matching the given deck name by scanning the decks directory.
     * Tries exact filename match first, then scans files and checks deck metadata.
     */
    private static File findDeckFile(final String name) {
        final File decksDir = getDecksDir();
        if (!decksDir.exists()) {
            return null;
        }

        // Try direct filename match (name with safe characters + .dck)
        final File direct = new File(decksDir, name + ".dck");
        if (direct.exists()) {
            return direct;
        }

        // Scan directory for matching deck name
        return scanForDeck(decksDir, name);
    }

    private static File scanForDeck(final File dir, final String name) {
        final File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                final File found = scanForDeck(file, name);
                if (found != null) {
                    return found;
                }
            } else if (file.getName().endsWith(".dck")) {
                final Deck deck = DeckSerializer.fromFile(file);
                if (deck != null && name.equals(deck.getName())) {
                    return file;
                }
            }
        }
        return null;
    }
}
