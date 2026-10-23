package com.gamecoach.controller;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.OverwatchHeroCatalog;
import com.gamecoach.model.OverwatchRole;
import com.gamecoach.model.Role;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Every hero of each game with its role, for the website's review screen: when the reader guesses
 * a portrait wrong, the user picks the right hero from these lists.
 */
@RestController
public class HeroesController {

    /** One hero and its role, e.g. ("Luna Snow", "STRATEGIST") or ("Ramattra", "TANK"). */
    public record HeroOption(String name, String role) {
    }

    private final HeroRoleCatalog rivals;
    private final OverwatchHeroCatalog overwatch;

    public HeroesController(HeroRoleCatalog rivals, OverwatchHeroCatalog overwatch) {
        this.rivals = rivals;
        this.overwatch = overwatch;
    }

    @GetMapping("/api/rivals/heroes")
    public List<HeroOption> rivalsHeroes() {
        return rivals.heroNames().stream()
                .map(name -> new HeroOption(name, rivals.roleOf(name).name()))
                .filter(h -> !Role.UNKNOWN.name().equals(h.role()))
                .toList();
    }

    @GetMapping("/api/overwatch/heroes")
    public List<HeroOption> overwatchHeroes() {
        return overwatch.heroNames().stream()
                .map(name -> {
                    OverwatchRole role = overwatch.roleOfName(name);
                    return new HeroOption(name, role == null ? "UNKNOWN" : role.name());
                })
                .toList();
    }
}
