package com.gamecoach.controller;

import com.gamecoach.OverwatchFixtures;
import com.gamecoach.analysis.CareerAnalyzer;
import com.gamecoach.datasource.CareerStatsSource;
import com.gamecoach.datasource.CareerStatsUnavailableException;
import com.gamecoach.model.CareerProfile;
import com.gamecoach.model.CareerProfile.CareerStats;
import com.gamecoach.model.CareerProfile.HeroCareer;
import com.gamecoach.model.OverwatchMode;
import com.gamecoach.model.OverwatchRole;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real HTTP requests through Spring MVC (MockMvc) with a stand-in career source. */
class OverwatchHttpTest {

    private static final CareerStats SUPPORT =
            OverwatchFixtures.stats(30, 18, Duration.ofHours(5), 3.0, 500, 800, 250, 150000, 400000);
    private static final CareerStats ANA =
            OverwatchFixtures.stats(10, 4, Duration.ofHours(2), 2.0, 150, 300, 140, 50000, 160000);

    private final CareerStatsSource source = new CareerStatsSource() {
        @Override
        public CareerProfile getCareerProfile(String battleTag, OverwatchMode mode, String platform) {
            if (battleTag.equals("Down-1")) {
                throw new CareerStatsUnavailableException("OverFast API is limiting requests right now.", null);
            }
            if (!battleTag.equals("Swift-1234")) {
                throw new NoSuchElementException("No Overwatch profile found for " + battleTag + ".");
            }
            return new CareerProfile("Swift-1234", "Swift", mode, platform, null, SUPPORT,
                    Map.of(OverwatchRole.SUPPORT, SUPPORT), List.of(new HeroCareer("ana", "Ana", OverwatchRole.SUPPORT, ANA)));
        }

        @Override
        public List<PlayerMatch> searchPlayers(String name) {
            return List.of(new PlayerMatch("Swift-1234", "Swift", null, null, null, true));
        }
    };

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new OverwatchController(source, new CareerAnalyzer()))
            .setControllerAdvice(new ApiExceptionHandler()).build();

    @Test
    void analysisComparesHeroesWithinTheirRole() throws Exception {
        mvc.perform(get("/api/overwatch/players/Swift-1234/analysis").param("mode", "competitive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("COMPETITIVE"))
                .andExpect(jsonPath("$.mainRole").value("SUPPORT"))
                .andExpect(jsonPath("$.heroes[0].hero").value("Ana"))
                .andExpect(jsonPath("$.heroes[0].vsOtherRoleHeroes.otherGamesPlayed").value(20));
    }

    @Test
    void searchFindsTheBattleTag() throws Exception {
        mvc.perform(get("/api/overwatch/players").param("name", "Swift"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerId").value("Swift-1234"));
    }

    @Test
    void errorsBecomeClearStatusCodes() throws Exception {
        mvc.perform(get("/api/overwatch/players/Swift-1234/profile").param("mode", "arcade"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/overwatch/players/Nobody-1/profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No Overwatch profile found for Nobody-1."));
        mvc.perform(get("/api/overwatch/players/Down-1/profile"))
                .andExpect(status().isServiceUnavailable());
    }
}
