package com.gamecoach.service.claude;

import com.gamecoach.parsing.HeroDoubt;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Makes the reader say how sure it is of each portrait, shared by both games' readers.
 *
 * <p>Each row gets three extra tool fields: {@code portraitLooks} (what the portrait shows, written
 * before the hero is chosen, which makes the model look instead of guess), {@code heroSure}, and
 * {@code heroAlternatives}. Rows marked unsure become {@link HeroDoubt}s for the review page.
 */
final class PortraitCheck {

    /** At most this many alternatives are offered per row. */
    static final int MAX_ALTERNATIVES = 2;

    private PortraitCheck() {
    }

    /**
     * The prompt section explaining the three fields.
     *
     * @param referenceSheet true when a HERO PORTRAITS sheet is sent: the hero is found by matching it,
     *                       and only unsure rows are described (descriptions are the costly part of a reply)
     */
    static String rules(boolean referenceSheet) {
        if (referenceSheet) {
            return """
                    Portraits (a wrong hero ruins the coaching), for every row:
                    1. Find the portrait's face on HERO PORTRAITS and choose that hero.
                    2. Set heroSure to true when it clearly matches one picture. Set it to false when two \
                    pictures could fit, when the portrait is dark or partly hidden, or when it matches no \
                    picture (a costume portrait or a hero missing from the sheet).
                    3. Only when heroSure is false: describe what the portrait shows in portraitLooks (a few \
                    words: hair, face, mask, colors) and put up to two other heroes it could be in \
                    heroAlternatives, most likely first. Leave both out when sure.
                    """;
        }
        return """
                Portraits (the hardest part, and a wrong hero ruins the coaching), for every row:
                1. In portraitLooks, first describe what the portrait actually shows in a few words: hair, \
                face, mask or helmet, costume colors, glow, weapon. Write it before choosing the hero.
                2. Then choose the hero whose look matches that description.
                3. Set heroSure to true only when the description has features that belong to that hero and \
                to no similar one. Set it to false when the portrait is small, dark, partly hidden or \
                alternate art, when two heroes could fit, and for heroes added in recent seasons, which you \
                may know less well. An unsure mark costs the player a glance; a wrong confident one costs \
                them their coaching.
                4. When unsure, put up to two other heroes it could be in heroAlternatives, most likely \
                first, from HERO NAMES.
                """;
    }

    /**
     * Adds the three fields after "hero" (with a sheet: looks only for unsure rows), or with
     * portraitLooks placed before "hero" so it is written first (without a sheet: every row).
     */
    static Map<String, Object> withPortraitFields(Map<String, Object> player, boolean referenceSheet) {
        Map<String, Object> looks = Map.of("type", "string", "description", referenceSheet
                ? "Only when heroSure is false: what the portrait shows in a few words."
                : "What the portrait shows in a few words, written before choosing the hero.");
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> e : player.entrySet()) {
            if (e.getKey().equals("hero") && !referenceSheet) {
                out.put("portraitLooks", looks);
            }
            out.put(e.getKey(), e.getValue());
            if (e.getKey().equals("hero")) {
                out.put("heroSure", Map.of("type", "boolean", "description", referenceSheet
                        ? "True when the portrait clearly matches one picture on HERO PORTRAITS."
                        : "True only when the portrait clearly shows this hero's own features."));
                if (referenceSheet) {
                    out.put("portraitLooks", looks);
                }
                out.put("heroAlternatives", Map.of("type", "array", "items", Map.of("type", "string"),
                        "maxItems", MAX_ALTERNATIVES,
                        "description", "Only when unsure: other heroes it could be, most likely first."));
            }
        }
        return out;
    }

    /** The required-field list with the new fields added (looks only when every row is described). */
    static List<String> withPortraitRequired(boolean referenceSheet, List<String> required) {
        List<String> out = new ArrayList<>(required);
        if (!referenceSheet) {
            out.add("portraitLooks");
        }
        out.add("heroSure");
        return out;
    }

    /**
     * One doubt per row marked unsure, or whose hero isn't a known name.
     *
     * @param rows     the tool's player rows, in the same order as the match's players
     * @param official maps a name to the hero's official spelling; empty when it isn't a hero
     */
    static List<HeroDoubt> doubts(List<Map<String, Object>> rows, Function<String, Optional<String>> official) {
        List<HeroDoubt> doubts = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String hero = ToolInput.string(row, "hero");
            Optional<String> known = hero == null ? Optional.empty() : official.apply(hero);
            if (sure(row) && known.isPresent()) {
                continue;
            }
            Set<String> alternatives = new LinkedHashSet<>();
            for (String name : ToolInput.strings(row, "heroAlternatives")) {
                official.apply(name)
                        .filter(n -> known.isEmpty() || !n.equalsIgnoreCase(known.get()))
                        .ifPresent(alternatives::add);
            }
            doubts.add(new HeroDoubt(i, ToolInput.string(row, "name"), known.orElse(hero),
                    alternatives.stream().limit(MAX_ALTERNATIVES).toList(), ToolInput.string(row, "portraitLooks")));
        }
        return doubts;
    }

    /** A missing answer counts as sure, so older replies and test data raise no doubts. */
    private static boolean sure(Map<String, Object> row) {
        Object value = row.get("heroSure");
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return !s.strip().toLowerCase(Locale.ROOT).equals("false");
        }
        return true;
    }
}
