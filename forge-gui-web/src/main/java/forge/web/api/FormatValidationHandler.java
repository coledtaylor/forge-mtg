package forge.web.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import io.javalin.http.Context;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.io.DeckSerializer;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;

/**
 * REST handler for deck format validation.
 * Checks a deck against format legality rules and returns illegal cards with reasons.
 */
public final class FormatValidationHandler {

    private FormatValidationHandler() { }

    public static void validate(final Context ctx) {
        final String name = ctx.pathParam("name");
        final String formatName = ctx.queryParam("format");

        if (formatName == null || formatName.isEmpty()) {
            ctx.status(400).json(Map.of("error", "format query parameter is required"));
            return;
        }

        final File decksDir = new File(ForgeConstants.DECK_CONSTRUCTED_DIR);
        final File deckFile = findDeckFile(decksDir, name);

        if (deckFile == null) {
            ctx.status(404).json(Map.of("error", "Deck not found"));
            return;
        }

        final Deck deck = DeckSerializer.fromFile(deckFile);
        if (deck == null) {
            ctx.status(404).json(Map.of("error", "Deck not found"));
            return;
        }

        final GameFormat format = FModel.getFormats().get(formatName);
        if (format == null) {
            ctx.status(400).json(Map.of("error", "Unknown format: " + formatName));
            return;
        }

        final List<Map<String, String>> illegalCards = new ArrayList<>();
        final Predicate<PaperCard> filterRules = format.getFilterRules();

        for (final DeckSection section : new DeckSection[]{DeckSection.Main, DeckSection.Sideboard, DeckSection.Commander}) {
            final CardPool pool = deck.get(section);
            if (pool == null) {
                continue;
            }
            for (final Map.Entry<PaperCard, Integer> entry : pool) {
                final PaperCard card = entry.getKey();
                if (!filterRules.test(card)) {
                    illegalCards.add(Map.of(
                        "name", card.getName(),
                        "section", section.name().toLowerCase(),
                        "reason", "Not legal in " + formatName
                    ));
                }
            }
        }

        // Also check getDeckConformanceProblem for deck-level issues (card count, etc.)
        final String conformanceProblem = format.getDeckConformanceProblem(deck);

        ctx.json(Map.of(
            "legal", illegalCards.isEmpty() && conformanceProblem == null,
            "illegalCards", illegalCards,
            "conformanceProblem", conformanceProblem != null ? conformanceProblem : ""
        ));
    }

    private static File findDeckFile(final File decksDir, final String name) {
        if (!decksDir.exists()) {
            return null;
        }
        final File direct = new File(decksDir, name + ".dck");
        if (direct.exists()) {
            return direct;
        }
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
