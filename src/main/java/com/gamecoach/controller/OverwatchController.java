package com.gamecoach.controller;

import com.gamecoach.analysis.CareerAnalysis;
import com.gamecoach.analysis.CareerAnalyzer;
import com.gamecoach.datasource.CareerStatsSource;
import com.gamecoach.datasource.CareerStatsSource.PlayerMatch;
import com.gamecoach.model.CareerProfile;
import com.gamecoach.model.OverwatchMode;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/**
 * Overwatch endpoints, backed by career stats (OverFast API). A BattleTag goes in the path with a
 * dash instead of "#" (Name-1234), because "#" ends the path part of a URL.
 *
 * ?mode= is quickplay (default) or competitive. ?platform= is pc or console; leave it out to
 * combine both.
 */
@RestController
@RequestMapping("/api/overwatch")
public class OverwatchController {

    private final CareerStatsSource careers;
    private final CareerAnalyzer analyzer;

    public OverwatchController(CareerStatsSource careers, CareerAnalyzer analyzer) {
        this.careers = careers;
        this.analyzer = analyzer;
    }

    /** Finds players by name, to get the exact BattleTag (letter case matters). */
    @GetMapping("/players")
    public List<PlayerMatch> search(@RequestParam("name") String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Give a name to search for, like ?name=TeKrop.");
        }
        return careers.searchPlayers(name);
    }

    /** The career as OverFast reports it: ranks, totals, per-role and per-hero stats. */
    @GetMapping("/players/{battleTag}/profile")
    public CareerProfile profile(@PathVariable("battleTag") String battleTag,
                                 @RequestParam(name = "mode", required = false) String mode,
                                 @RequestParam(name = "platform", required = false) String platform) {
        return careers.getCareerProfile(battleTag, mode(mode), platform(platform));
    }

    /** Time split by role and hero, and each hero against the player's other heroes in its role. */
    @GetMapping("/players/{battleTag}/analysis")
    public CareerAnalysis analysis(@PathVariable("battleTag") String battleTag,
                                   @RequestParam(name = "mode", required = false) String mode,
                                   @RequestParam(name = "platform", required = false) String platform) {
        return analyzer.analyze(careers.getCareerProfile(battleTag, mode(mode), platform(platform)));
    }

    /** Missing → quick play, which every player has; competitive needs it unlocked. */
    static OverwatchMode mode(String mode) {
        if (mode == null || mode.isBlank()) {
            return OverwatchMode.QUICKPLAY;
        }
        OverwatchMode parsed = OverwatchMode.fromKey(mode);
        if (parsed == null) {
            throw new BadRequestException("mode should be quickplay or competitive, not \"" + mode + "\".");
        }
        return parsed;
    }

    /** Missing → null (both platforms combined). */
    static String platform(String platform) {
        if (platform == null || platform.isBlank()) {
            return null;
        }
        String p = platform.strip().toLowerCase(Locale.ROOT);
        if (!p.equals("pc") && !p.equals("console")) {
            throw new BadRequestException("platform should be pc or console, not \"" + platform + "\".");
        }
        return p;
    }
}
