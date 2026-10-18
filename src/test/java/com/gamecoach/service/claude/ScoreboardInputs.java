package com.gamecoach.service.claude;

import com.gamecoach.model.HeroStats;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.Medal;
import com.gamecoach.model.PlayerLine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Test helper: writes a match the way Claude would return it through submit_scoreboard (a Map, as
 * Jackson parses the JSON). Turning the real fake matches into tool input and back checks the
 * reader without any network call.
 */
final class ScoreboardInputs {

    private ScoreboardInputs() {
    }

    static Map<String, Object> of(MatchDetails match) {
        Map<String, Object> input = new HashMap<>();
        input.put("result", match.result().name());
        input.put("duration", String.format(Locale.ROOT, "%d:%02d", match.duration().toMinutes(), match.duration().toSecondsPart()));
        input.put("mode", match.mode());
        input.put("map", match.map());
        input.put("replayId", match.replayId());
        input.put("bans", match.bans());
        List<Object> players = new ArrayList<>();
        for (PlayerLine p : match.players()) {
            players.add(row(p));
        }
        input.put("players", players);
        input.put("warnings", List.of());
        return input;
    }

    static Map<String, Object> row(PlayerLine p) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", p.name());
        row.put("team", p.team());
        row.put("role", p.role().name());
        row.put("hero", p.lastHero());
        row.put("heroesUsed", p.heroes().stream().map(HeroStats::hero).toList());
        row.put("teamUpPartner", p.teamUp() == null ? null : p.teamUp().partner());
        row.put("teamUpEnhanced", p.teamUp() != null && p.teamUp().enhanced());
        row.put("portrait", p.proficiency() == null || p.proficiency().portrait() == null ? "DEFAULT"
                : p.proficiency().portrait().name());
        row.put("badge", p.proficiency() == null || p.proficiency().badge() == null ? "NONE"
                : p.proficiency().badge().name());
        // Jackson reads small JSON numbers as Integer and decimals as Double.
        row.put("kills", p.kills());
        row.put("deaths", p.deaths());
        row.put("assists", p.assists());
        row.put("finalHits", p.finalHits());
        row.put("damage", p.damage());
        row.put("blocked", p.blocked());
        row.put("healing", p.healing());
        row.put("accuracyPercent", p.accuracyPercent());
        row.put("uploader", p.uploader());
        row.put("mvp", p.mvp());
        row.put("svp", p.svp());
        row.put("medals", p.medals().stream().map(Medal::name).toList());
        row.put("multiKill", p.multiKill());
        return row;
    }
}
