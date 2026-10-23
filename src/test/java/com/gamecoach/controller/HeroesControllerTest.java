package com.gamecoach.controller;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.OverwatchHeroCatalog;
import com.gamecoach.controller.HeroesController.HeroOption;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroesControllerTest {

    private final HeroesController controller = new HeroesController(new HeroRoleCatalog(), new OverwatchHeroCatalog());

    @Test
    void listsEveryHeroWithItsRole() {
        List<HeroOption> rivals = controller.rivalsHeroes();
        assertTrue(rivals.contains(new HeroOption("Daredevil", "DUELIST")));
        assertTrue(rivals.contains(new HeroOption("Deadpool (Strategist)", "STRATEGIST")));
        List<HeroOption> ow = controller.overwatchHeroes();
        assertTrue(ow.contains(new HeroOption("Ramattra", "TANK")));
        assertTrue(ow.contains(new HeroOption("Sombra", "SUPPORT")));
        assertTrue(ow.size() >= 50);
    }
}
