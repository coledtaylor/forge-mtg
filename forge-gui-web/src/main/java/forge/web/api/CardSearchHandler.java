package forge.web.api;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.javalin.http.Context;

import forge.card.CardRulesPredicates;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.model.FModel;
import forge.util.ComparableOp;
import forge.util.PredicateString;
import forge.web.dto.CardSearchDto;

/**
 * REST handler for card search. Supports filtering by name, color identity,
 * type, CMC, and format with pagination.
 */
public final class CardSearchHandler {

    private CardSearchHandler() { }

    public static void search(final Context ctx) {
        final String q = ctx.queryParam("q");
        final String color = ctx.queryParam("color");
        final String type = ctx.queryParam("type");
        final String cmcStr = ctx.queryParam("cmc");
        final String cmcOp = ctx.queryParamAsClass("cmcOp", String.class).getOrDefault("eq");
        final String format = ctx.queryParam("format");
        final int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        final int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(20);

        Predicate<PaperCard> filter = pc -> true;

        if (q != null && !q.isEmpty()) {
            filter = filter.and(PaperCardPredicates.searchableName(
                    PredicateString.StringOp.CONTAINS_IC, q));
        }

        if (color != null && !color.isEmpty()) {
            for (final char c : color.toUpperCase().toCharArray()) {
                final Predicate<PaperCard> colorPred = switch (c) {
                    case 'W' -> pc -> pc.getRules().getColorIdentity().hasWhite();
                    case 'U' -> pc -> pc.getRules().getColorIdentity().hasBlue();
                    case 'B' -> pc -> pc.getRules().getColorIdentity().hasBlack();
                    case 'R' -> pc -> pc.getRules().getColorIdentity().hasRed();
                    case 'G' -> pc -> pc.getRules().getColorIdentity().hasGreen();
                    default -> null;
                };
                if (colorPred != null) {
                    filter = filter.and(colorPred);
                }
            }
        }

        if (type != null && !type.isEmpty()) {
            filter = filter.and(PaperCardPredicates.fromRules(
                    CardRulesPredicates.coreType(type)));
        }

        if (cmcStr != null && !cmcStr.isEmpty()) {
            final int cmcValue = Integer.parseInt(cmcStr);
            final ComparableOp op = switch (cmcOp) {
                case "lt" -> ComparableOp.LESS_THAN;
                case "gt" -> ComparableOp.GREATER_THAN;
                case "lte" -> ComparableOp.LT_OR_EQUAL;
                case "gte" -> ComparableOp.GT_OR_EQUAL;
                default -> ComparableOp.EQUALS;
            };
            filter = filter.and(PaperCardPredicates.fromRules(
                    CardRulesPredicates.cmc(op, cmcValue)));
        }

        if (format != null && !format.isEmpty()) {
            final forge.game.GameFormat fmt = FModel.getFormats().get(format);
            if (fmt != null) {
                filter = filter.and(fmt.getFilterRules());
            }
        }

        final List<PaperCard> allMatches = FModel.getMagicDb().getCommonCards()
                .getUniqueCards().stream()
                .filter(filter)
                .sorted(Comparator.comparing(PaperCard::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        final int totalCount = allMatches.size();
        final int totalPages = (int) Math.ceil((double) totalCount / limit);

        final List<CardSearchDto> pageResults = allMatches.stream()
                .skip((long) (page - 1) * limit)
                .limit(limit)
                .map(CardSearchDto::from)
                .collect(Collectors.toList());

        ctx.json(Map.of(
                "cards", pageResults,
                "total", totalCount,
                "page", page,
                "limit", limit,
                "totalPages", totalPages
        ));
    }
}
