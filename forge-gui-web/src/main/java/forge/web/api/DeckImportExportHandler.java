package forge.web.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.javalin.http.Context;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckRecognizer;
import forge.deck.DeckSection;
import forge.deck.io.DeckSerializer;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.web.dto.ParseTokenDto;

/**
 * REST handler for deck import parsing and deck export formatting.
 * Parse wraps DeckRecognizer to tokenize raw text; export formats a saved deck.
 */
public final class DeckImportExportHandler {

    private DeckImportExportHandler() { }

    /**
     * POST /api/decks/parse
     * Body: { "text": "4 Lightning Bolt\n..." }
     * Returns: array of ParseTokenDto
     */
    @SuppressWarnings("unchecked")
    public static void parse(final Context ctx) {
        final Map<String, String> body = ctx.bodyAsClass(Map.class);
        final String text = body.get("text");
        if (text == null || text.trim().isEmpty()) {
            ctx.json(List.of());
            return;
        }
        final String[] lines = text.split("\\r?\\n");
        final DeckRecognizer recognizer = new DeckRecognizer();

        // Parse line-by-line instead of using parseCardList to:
        // 1. Preserve original line order (parseCardList reorders by section)
        // 2. Avoid auto-assigning legendary creatures to Commander section
        // 3. Let the frontend handle commander detection from blank-line patterns
        final List<ParseTokenDto> result = new ArrayList<>();
        DeckSection currentSection = DeckSection.Main;

        for (final String line : lines) {
            final DeckRecognizer.Token token = recognizer.recognizeLine(line, currentSection);
            if (token == null) {
                continue;
            }

            // Track explicit section headers (e.g. "Commander", "Sideboard")
            if (token.getType() == DeckRecognizer.TokenType.DECK_SECTION_NAME) {
                currentSection = DeckSection.valueOf(token.getText());
            }

            // Override auto-assigned Commander section — force all cards to use
            // the current explicit section context instead. This prevents
            // DeckRecognizer from auto-promoting legendary creatures to Commander.
            final ParseTokenDto dto = ParseTokenDto.from(token);
            if (token.isCardToken() && token.getTokenSection() == DeckSection.Commander
                    && currentSection != DeckSection.Commander) {
                dto.section = currentSection.name();
            }
            result.add(dto);
        }
        ctx.json(result);
    }

    /**
     * GET /api/decks/{name}/export?format=generic|mtgo|arena|forge
     * Returns: { "text": "formatted deck text" }
     */
    public static void export(final Context ctx) {
        final String name = ctx.pathParam("name");
        String format = ctx.queryParam("format");
        if (format == null) {
            format = "generic";
        }

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

        final String text = switch (format.toLowerCase()) {
            case "mtgo" -> formatMtgo(deck);
            case "arena" -> formatArena(deck);
            case "forge" -> formatForge(deck);
            default -> formatGeneric(deck);
        };
        ctx.json(Map.of("text", text));
    }

    // -- Format methods --

    /** Generic: "4 Lightning Bolt" */
    private static String formatGeneric(final Deck deck) {
        final StringBuilder sb = new StringBuilder();
        appendGenericSection(sb, deck.getOrCreate(DeckSection.Main));
        final CardPool side = deck.get(DeckSection.Sideboard);
        if (side != null && !side.isEmpty()) {
            sb.append("\nSideboard\n");
            appendGenericSection(sb, side);
        }
        final CardPool cmdr = deck.get(DeckSection.Commander);
        if (cmdr != null && !cmdr.isEmpty()) {
            sb.append("\nCommander\n");
            appendGenericSection(sb, cmdr);
        }
        return sb.toString().trim();
    }

    private static void appendGenericSection(final StringBuilder sb, final CardPool pool) {
        for (final Map.Entry<PaperCard, Integer> e : pool) {
            sb.append(e.getValue()).append(" ").append(e.getKey().getName()).append("\n");
        }
    }

    /** MTGO: "4 Lightning Bolt (2XM)" */
    private static String formatMtgo(final Deck deck) {
        final StringBuilder sb = new StringBuilder();
        appendMtgoSection(sb, deck.getOrCreate(DeckSection.Main));
        final CardPool side = deck.get(DeckSection.Sideboard);
        if (side != null && !side.isEmpty()) {
            sb.append("\nSideboard\n");
            appendMtgoSection(sb, side);
        }
        final CardPool cmdr = deck.get(DeckSection.Commander);
        if (cmdr != null && !cmdr.isEmpty()) {
            sb.append("\nCommander\n");
            appendMtgoSection(sb, cmdr);
        }
        return sb.toString().trim();
    }

    private static void appendMtgoSection(final StringBuilder sb, final CardPool pool) {
        for (final Map.Entry<PaperCard, Integer> e : pool) {
            final PaperCard pc = e.getKey();
            sb.append(e.getValue()).append(" ").append(pc.getName())
              .append(" (").append(pc.getEdition()).append(")\n");
        }
    }

    /** Arena: "4 Lightning Bolt (2XM) 123" */
    private static String formatArena(final Deck deck) {
        final StringBuilder sb = new StringBuilder();
        appendArenaSection(sb, deck.getOrCreate(DeckSection.Main));
        final CardPool side = deck.get(DeckSection.Sideboard);
        if (side != null && !side.isEmpty()) {
            sb.append("\nSideboard\n");
            appendArenaSection(sb, side);
        }
        final CardPool cmdr = deck.get(DeckSection.Commander);
        if (cmdr != null && !cmdr.isEmpty()) {
            sb.append("\nCommander\n");
            appendArenaSection(sb, cmdr);
        }
        return sb.toString().trim();
    }

    private static void appendArenaSection(final StringBuilder sb, final CardPool pool) {
        for (final Map.Entry<PaperCard, Integer> e : pool) {
            final PaperCard pc = e.getKey();
            sb.append(e.getValue()).append(" ").append(pc.getName())
              .append(" (").append(pc.getEdition()).append(") ")
              .append(pc.getCollectorNumber()).append("\n");
        }
    }

    /** Forge .dck: full metadata format */
    private static String formatForge(final Deck deck) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[metadata]\n");
        sb.append("Name=").append(deck.getName()).append("\n");
        if (deck.getComment() != null && !deck.getComment().isEmpty()) {
            sb.append("Comment=").append(deck.getComment()).append("\n");
        }
        sb.append("[Main]\n");
        appendForgeSection(sb, deck.getOrCreate(DeckSection.Main));
        final CardPool side = deck.get(DeckSection.Sideboard);
        if (side != null && !side.isEmpty()) {
            sb.append("[Sideboard]\n");
            appendForgeSection(sb, side);
        }
        final CardPool cmdr = deck.get(DeckSection.Commander);
        if (cmdr != null && !cmdr.isEmpty()) {
            sb.append("[Commander]\n");
            appendForgeSection(sb, cmdr);
        }
        return sb.toString().trim();
    }

    private static void appendForgeSection(final StringBuilder sb, final CardPool pool) {
        sb.append(pool.toCardList("\n")).append("\n");
    }

    // -- Deck file lookup (replicates DeckHandler.findDeckFile pattern) --

    private static File findDeckFile(final String name) {
        final File decksDir = new File(ForgeConstants.DECK_CONSTRUCTED_DIR);
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
