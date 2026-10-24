package com.gamecoach.controller;

import com.gamecoach.OverwatchMatchFixtures;
import com.gamecoach.analysis.OverwatchHeroCatalog;
import com.gamecoach.analysis.OverwatchMatchAnalyzer;
import com.gamecoach.parsing.OverwatchMatchReader;
import com.gamecoach.parsing.InMemoryOverwatchMatchStore;
import com.gamecoach.parsing.OverwatchMatchStore;
import com.gamecoach.parsing.OverwatchMatchValidator;
import com.gamecoach.parsing.ParsedOverwatchMatch;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real HTTP requests through Spring MVC for the Overwatch match endpoints: the multipart fields
 * (including several "personal" files), the JSON shape, and a GET-then-PUT round trip that proves
 * the match JSON (durations, nested records) reads back in. The reader is a stand-in.
 */
class OverwatchUploadHttpTest {

    private final OverwatchHeroCatalog heroes = new OverwatchHeroCatalog();
    private final List<Integer> personalCounts = new ArrayList<>();

    private final OverwatchMatchReader reader = (screens, matchId) -> {
        personalCounts.add(screens.personal().size());
        return new ParsedOverwatchMatch(OverwatchMatchFixtures.colosseo().withId(matchId), List.of());
    };

    private MockMvc mvc() {
        OverwatchMatchController controller = new OverwatchMatchController(Optional.of(reader), Optional.empty(),
                new InMemoryOverwatchMatchStore(), new OverwatchMatchValidator(heroes), new OverwatchMatchAnalyzer(heroes), heroes);
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler()).build();
    }

    private static MockMultipartFile png(String field) {
        return new MockMultipartFile(field, field + ".png", "image/png", new byte[] {1, 2, 3});
    }

    @Test
    void uploadThenReviewThenAnalysis() throws Exception {
        MockMvc mvc = mvc();
        String body = mvc.perform(multipart("/api/overwatch/matches").file(png("teams")).file(png("summary"))
                        .file(png("personal")).file(png("personal")).param("hero", "ramattra"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.match.players.length()").value(10))
                .andExpect(jsonPath("$.match.heroStats[0].stats[0].label").value("Weapon accuracy"))
                .andExpect(jsonPath("$.warnings.length()").value(0))
                .andReturn().getResponse().getContentAsString();
        assertEquals(List.of(2), personalCounts);
        String id = body.replaceAll("(?s).*\"matchId\":\"([^\"]+)\".*", "$1");

        String match = mvc.perform(get("/api/overwatch/matches/" + id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(put("/api/overwatch/matches/" + id).contentType(MediaType.APPLICATION_JSON).content(match))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match.map").value("Colosseo"))
                .andExpect(jsonPath("$.warnings.length()").value(0));

        mvc.perform(get("/api/overwatch/matches/" + id + "/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player").value("PLAYERONE"))
                .andExpect(jsonPath("$.stats.ELIMINATIONS.per10").value(38.2));
    }

    @Test
    void errorsHaveTheRightCodes() throws Exception {
        MockMvc mvc = mvc();
        mvc.perform(multipart("/api/overwatch/matches").file(png("summary")))
                .andExpect(status().isBadRequest());                       // no teams screen
        mvc.perform(multipart("/api/overwatch/matches").file(png("teams")).param("hero", "Daredevil"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/overwatch/matches/ow-nope")).andExpect(status().isNotFound());
        mvc.perform(post("/api/overwatch/matches/ow-nope/feedback")).andExpect(status().isServiceUnavailable());
    }
}
