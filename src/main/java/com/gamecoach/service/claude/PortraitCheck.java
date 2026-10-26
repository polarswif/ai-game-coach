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

    /** The prompt section explaining the three fields. */
    static String rules() {
        return """
                Portraits (the hardest part, and a wrong hero ruins the coaching), for every row:
                1. In portraitLooks, first describe what the portrait actually shows in a few words: hair, \
                face, mask or helmet, costume colors, glow, weapon. Write it before choosing the hero.
                2. Then choose the hero whose look matches that description.
                3. Set heroSure to true only when the description has features that belong to that hero and \
                to no similar one. Set it to false when the portrait is small, dark, partly hidden or \
                alternate costume art, when two heroes could fit, and for heroes added in recent seasons, \
                which you may know less well. Most scoreboards have several unsure rows: an unsure mark \
                costs the player a glance, a wrong confident one costs them their coaching.
                4. When unsure, put up to two other heroes it could be in heroAlternatives, most likely \
                first, from HERO NAMES.
                """;
    }

    /** Adds the three fields, with portraitLooks placed before "hero" so it is written first. */
    static Map<String, Object> withPortraitFields(Map<String, Object> player) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> e : player.entrySet()) {
            if (e.getKey().equals("hero")) {
                out.put("portraitLooks", Map.of("type", "string",
                        "description", "What the portrait shows in a few words, written before choosing the hero."));
            }
            out.put(e.getKey(), e.getValue());
            if (e.getKey().equals("hero")) {
                out.put("heroSure", Map.of("type", "boolean",
                        "description", "True only when the portrait clearly shows this hero's own features."));
                out.put("heroAlternatives", Map.of("type", "array", "items", Map.of("type", "string"),
                        "maxItems", MAX_ALTERNATIVES,
                        "description", "When unsure: other heroes it could be, most likely first; empty when sure."));
            }
        }
        return out;
    }

    /** The required-field list with the new fields added. */
    static List<String> withPortraitRequired(List<String> required) {
        List<String> out = new ArrayList<>(required);
        out.add("portraitLooks");
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
