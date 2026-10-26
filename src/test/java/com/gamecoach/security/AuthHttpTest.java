package com.gamecoach.security;

import com.gamecoach.datasource.FakeMatchHistorySource;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.parsing.UploadedMatchStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sign-up, login and "your matches only", through the real security filter chain. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-test;DB_CLOSE_DELAY=-1",
        "gamecoach.auth.invite-codes=alpha-code, beta-code, gamma-code",
        "gamecoach.auth.jwt-secret=test-secret-that-is-long-enough-for-hs256",
        "gamecoach.auth.uploads-per-day=1"})
class AuthHttpTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UploadedMatchStore uploads;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String signup(String name, String code) throws Exception {
        String body = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + name + "\",\"password\":\"long-password\",\"inviteCode\":\"" + code + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(name))
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void anInviteCodeWorksOnceAndLoginGivesAToken() throws Exception {
        signup("alex", "alpha-code");
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sam\",\"password\":\"long-password\",\"inviteCode\":\"alpha-code\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("That invite code isn't valid or has already been used."));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sam\",\"password\":\"long-password\",\"inviteCode\":\"made-up\"}"))
                .andExpect(status().isBadRequest());

        String token = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ALEX\",\"password\":\"long-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alex"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alex\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Wrong username or password."));
    }

    @Test
    void theApiNeedsATokenButHeroListsAreOpen() throws Exception {
        mvc.perform(get("/api/matches")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/matches").header("Authorization", "Bearer not-a-real-token")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/rivals/heroes")).andExpect(status().isOk());
        mvc.perform(get("/api/overwatch/heroes")).andExpect(status().isOk());
    }

    @Test
    void eachAccountSeesOnlyItsOwnMatches() throws Exception {
        String beta = signup("beta", "beta-code");
        String gamma = signup("gamma", "gamma-code");

        MatchDetails match = new FakeMatchHistorySource().getMatchDetails("10378920354");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("beta", null, List.of()));
        try {
            uploads.save(new MatchDetails("upload-owned01", match.mode(), match.map(), match.duration(), match.result(),
                    match.replayId(), match.bans(), match.players(), match.score()));                  // saved as beta's
        } finally {
            SecurityContextHolder.clearContext();
        }

        mvc.perform(get("/api/matches").header("Authorization", "Bearer " + beta))
                .andExpect(jsonPath("$[?(@.matchId == 'upload-owned01')]").exists());
        mvc.perform(get("/api/matches").header("Authorization", "Bearer " + gamma))
                .andExpect(jsonPath("$[?(@.matchId == 'upload-owned01')]").doesNotExist());
        mvc.perform(get("/api/rivals/matches/upload-owned01").header("Authorization", "Bearer " + gamma))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/matches/upload-owned01").header("Authorization", "Bearer " + gamma))
                .andExpect(status().isNotFound());

        // beta has used the 1 upload allowed per day in this test: the next one is turned away first.
        mvc.perform(post("/api/rivals/matches").header("Authorization", "Bearer " + beta))
                .andExpect(status().isTooManyRequests());
    }
}
