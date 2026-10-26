package com.gamecoach.analysis;

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
import java.util.Map;
import java.util.Optional;

/**
 * Every hero's two team-ups, from resources/heroes/marvel-rivals-team-ups.csv.
 * The scoreboard shows only the partner's portrait, so the team-up is found from (hero, partner).
 */
@Component
public class TeamUpCatalog {

    private static final String DEFAULT_TABLE = "/heroes/marvel-rivals-team-ups.csv";

    private final Map<String, List<TeamUp>> teamUpsByHero = new HashMap<>();

    public TeamUpCatalog() {
        try (InputStream in = TeamUpCatalog.class.getResourceAsStream(DEFAULT_TABLE)) {
            if (in == null) {
                throw new IllegalStateException("Missing team-up table " + DEFAULT_TABLE);
            }
            load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The hero's team-ups (normally two). Empty for an unknown hero. Deadpool's labels all share his. */
    public List<TeamUp> forHero(String hero) {
        if (hero == null) {
            return List.of();
        }
        return teamUpsByHero.getOrDefault(HeroRoleCatalog.baseName(hero), List.of());
    }

    /**
     * The team-up between these two heroes, if there is one. The table lists each team-up under one
     * hero, but the scoreboard can show it from either side (Doctor Strange's box shows Magik for
     * Magik's Chain of Cyttorak), so the partner's own list is checked too.
     */
    public Optional<TeamUp> find(String hero, String partner) {
        Optional<TeamUp> own = forHero(hero).stream()
                .filter(t -> HeroRoleCatalog.sameHero(t.partner(), partner))
                .findFirst();
        return own.isPresent() ? own : forHero(partner).stream()
                .filter(t -> HeroRoleCatalog.sameHero(t.partner(), hero))
                .findFirst();
    }

    /** Every hero this hero can share a team-up with, from either side of the table, in table order. */
    public List<String> partnersOf(String hero) {
        java.util.LinkedHashSet<String> partners = new java.util.LinkedHashSet<>();
        forHero(hero).forEach(t -> partners.add(t.partner()));
        teamUpsByHero.values().forEach(list -> list.stream()
                .filter(t -> HeroRoleCatalog.sameHero(t.partner(), hero))
                .forEach(t -> partners.add(t.hero())));
        return List.copyOf(partners);
    }

    private void load(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // split("\\|") because "|" alone is a regex meaning "or"; the backslashes make it a plain bar.
            String[] cols = line.split("\\|");
            if (cols.length != 3) {
                throw new IllegalStateException("Team-up row needs 3 columns (hero|team-up|partner): " + line);
            }
            TeamUp teamUp = new TeamUp(cols[0].strip(), cols[1].strip(), cols[2].strip());
            teamUpsByHero.computeIfAbsent(HeroRoleCatalog.baseName(teamUp.hero()), k -> new ArrayList<>()).add(teamUp);
        }
    }
}
