package forge.web.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.javalin.http.Context;

import org.tinylog.Logger;

import forge.StaticData;
import forge.card.ColorSet;
import forge.item.PaperCard;
import forge.item.SealedTemplate;
import forge.item.generation.UnOpenedProduct;
import forge.web.dto.JumpstartPackDto;

/**
 * REST handler for Jumpstart pack browsing. Reads Forge's built-in
 * Jumpstart pack definitions from StaticData and returns pack summaries.
 */
public final class JumpstartHandler {

    private JumpstartHandler() { }

    /**
     * GET /api/jumpstart/packs
     *
     * Returns a JSON array of JumpstartPackDto objects for all JMP and J22
     * pack variants, sorted by theme name then set code.
     */
    public static void listPacks(final Context ctx) {
        final List<JumpstartPackDto> packs = new ArrayList<>();

        for (final SealedTemplate template : StaticData.instance().getSpecialBoosters()) {
            final String name = template.getName();
            if (name == null || (!name.startsWith("JMP ") && !name.startsWith("J22 "))) {
                continue;
            }

            final String setCode = name.substring(0, 3);
            final String theme = name.substring(4).replaceAll("\\s+\\d+$", "");

            List<PaperCard> cards;
            try {
                final UnOpenedProduct product = new UnOpenedProduct(template);
                cards = product.get();
            } catch (final Exception e) {
                Logger.warn("Failed to open Jumpstart pack '{}': {}", name, e.getMessage());
                continue;
            }

            final Set<String> colorSet = new LinkedHashSet<>();
            for (final PaperCard card : cards) {
                final ColorSet ci = card.getRules().getColorIdentity();
                if (ci.hasWhite()) { colorSet.add("W"); }
                if (ci.hasBlue()) { colorSet.add("U"); }
                if (ci.hasBlack()) { colorSet.add("B"); }
                if (ci.hasRed()) { colorSet.add("R"); }
                if (ci.hasGreen()) { colorSet.add("G"); }
            }

            packs.add(new JumpstartPackDto(name, theme, setCode, cards.size(), new ArrayList<>(colorSet)));
        }

        packs.sort(Comparator.comparing(JumpstartPackDto::getTheme)
                .thenComparing(JumpstartPackDto::getSetCode));

        ctx.json(packs);
    }
}
