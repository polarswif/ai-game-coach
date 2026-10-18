package com.gamecoach.analysis;

import com.gamecoach.model.Role;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Looks up a Marvel Rivals hero's role from the table in resources/heroes/marvel-rivals-roles.csv.
 * The scoreboard's role icon only covers each player's last hero; this table covers every hero,
 * including the others in a player's "Heroes Used" list.
 */
@Component
public class HeroRoleCatalog {

    private static final String DEFAULT_TABLE = "/heroes/marvel-rivals-roles.csv";

    private final Map<String, Role> rolesByHero = new HashMap<>();
    private final List<String> names = new ArrayList<>();

    public HeroRoleCatalog() {
        try (InputStream in = HeroRoleCatalog.class.getResourceAsStream(DEFAULT_TABLE)) {
            if (in == null) {
                throw new IllegalStateException("Missing hero role table " + DEFAULT_TABLE);
            }
            load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Role for a hero name, ignoring case and extra spaces. UNKNOWN if the hero is not listed. */
    public Role roleOf(String hero) {
        if (hero == null) {
            return Role.UNKNOWN;
        }
        return rolesByHero.getOrDefault(normalize(hero), Role.UNKNOWN);
    }

    /**
     * Every name in the table, as written there, in file order (alternate spellings included).
     * The screenshot reader gives this list to Claude so it uses the exact names.
     */
    public List<String> heroNames() {
        return List.copyOf(names);
    }

    /** True for "Deadpool" and the labeled forms like "Deadpool (Strategist)". */
    public static boolean isDeadpool(String hero) {
        return hero != null && normalize(hero).startsWith("deadpool");
    }

    /**
     * True when two hero names are the same hero, ignoring case and extra spaces.
     * All Deadpool labels count as the same hero: "Deadpool (Duelist)" matches "Deadpool".
     */
    public static boolean sameHero(String a, String b) {
        return a != null && b != null && baseName(a).equals(baseName(b));
    }

    /** The normalized name with any Deadpool role label removed, e.g. "deadpool". */
    static String baseName(String hero) {
        return isDeadpool(hero) ? "deadpool" : normalize(hero);
    }

    private void load(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;   // skip blank lines and comments
            }
            int comma = line.lastIndexOf(',');
            String hero = line.substring(0, comma);
            Role role = Role.valueOf(line.substring(comma + 1).strip());
            rolesByHero.put(normalize(hero), role);
            names.add(hero.strip());
        }
    }

    static String normalize(String hero) {
        return hero.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
