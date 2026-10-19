package com.gamecoach.controller;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.MatchAnalyzer;
import com.gamecoach.analysis.TeamUpCatalog;
import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.parsing.ParsedScoreboard;
import com.gamecoach.parsing.ScoreboardParser;
import com.gamecoach.parsing.ScoreboardUnreadableException;
import com.gamecoach.parsing.ScoreboardValidator;
import com.gamecoach.parsing.UploadedMatchStore;
import com.gamecoach.service.FakeCoachingService;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sends real HTTP requests through Spring MVC (MockMvc, no server or network): a multipart upload,
 * the JSON that comes back, and the error codes. The reader is a stand-in, so nothing calls Claude.
 */
class UploadHttpTest {

    private final FakeMatchHistorySource source = new FakeMatchHistorySource();
    private final UploadedMatchStore uploads = new UploadedMatchStore();
    private final ScoreboardValidator validator = new ScoreboardValidator(new HeroRoleCatalog(), new TeamUpCatalog());

    private MockMvc mvc(Optional<ScoreboardParser> reader) {
        RivalsController controller = new RivalsController(source,
                new MatchAnalyzer(new HeroRoleCatalog(), new TeamUpCatalog()), new FakeCoachingService(),
                uploads, validator, reader, new HeroRoleCatalog());
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler()).build();
    }

    private final ScoreboardParser readsPlayer29 = (screenshot, matchId, score) -> new ParsedScoreboard(
            source.getMatchDetails("10378920354").withPlayers(source.getMatchDetails("10378920354").players()), List.of());

    private static MockMultipartFile file(String type) {
        return new MockMultipartFile("screenshot", "scoreboard", type, new byte[] {1, 2, 3});
    }

    @Test
    void uploadReturns201WithTheMatchAndWarnings() throws Exception {
        mvc(Optional.of(readsPlayer29)).perform(multipart("/api/rivals/matches").file(file("image/png")).param("score", "3-2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.match.players.length()").value(12))
                .andExpect(jsonPath("$.warnings").isArray());
    }

    @Test
    void theHeroFieldIsAppliedToYourRow() throws Exception {
        mvc(Optional.of(readsPlayer29)).perform(multipart("/api/rivals/matches").file(file("image/png")).param("hero", "Luna Snow"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.match.players[?(@.uploader == true)].lastHero").value("Luna Snow"));
    }

    @Test
    void aBadUploadIs400() throws Exception {
        mvc(Optional.of(readsPlayer29)).perform(multipart("/api/rivals/matches").file(file("image/heic")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("HEIC")));
        mvc(Optional.of(readsPlayer29)).perform(multipart("/api/rivals/matches").file(file("image/png")).param("score", "won"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnreadableImageIs422() throws Exception {
        ScoreboardParser cat = (screenshot, matchId, score) -> {
            throw new ScoreboardUnreadableException("No scoreboard rows could be read from this image: it shows a cat.");
        };
        mvc(Optional.of(cat)).perform(multipart("/api/rivals/matches").file(file("image/png")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("cat")));
    }

    @Test
    void withoutTheClaudeProviderUploadsAre503() throws Exception {
        mvc(Optional.empty()).perform(multipart("/api/rivals/matches").file(file("image/png")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void feedbackWorksOnAnUnknownIdAs404() throws Exception {
        mvc(Optional.of(readsPlayer29)).perform(post("/api/rivals/matches/upload-nope/feedback"))
                .andExpect(status().isNotFound());
    }
}
