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

    /** The hero's team-up whose partner is this hero, if there is one. */
    public Optional<TeamUp> find(String hero, String partner) {
        return forHero(hero).stream()
                .filter(t -> HeroRoleCatalog.sameHero(t.partner(), partner))
                .findFirst();
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
            if (cols.length != 5) {
                throw new IllegalStateException("Team-up row needs 5 columns: " + line);
            }
            TeamUp teamUp = new TeamUp(cols[0].strip(), cols[1].strip(), cols[2].strip(), cols[3].strip(), cols[4].strip());
            teamUpsByHero.computeIfAbsent(HeroRoleCatalog.baseName(teamUp.hero()), k -> new ArrayList<>()).add(teamUp);
        }
    }
}
