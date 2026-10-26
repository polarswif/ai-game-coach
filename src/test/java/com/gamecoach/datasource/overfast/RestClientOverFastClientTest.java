package com.gamecoach.datasource.overfast;

import com.gamecoach.datasource.overfast.OverFastApi.StatsSummary;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The real HTTP code against MockRestServiceServer. stats-summary.json is OverFast's own documented
 * example reply, so this checks our records read their real JSON shape. Nothing leaves the machine.
 */
class RestClientOverFastClientTest {

    private static final String BASE = "https://overfast.test";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final RestClientOverFastClient client =
            new RestClientOverFastClient(new OverFastProperties(BASE, null), builder);

    private static String fixture(String name) throws IOException {
        return new ClassPathResource("overfast/" + name).getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void readsTheDocumentedStatsSummary() throws IOException {
        server.expect(requestTo(BASE + "/players/TeKrop-2217/stats/summary?gamemode=competitive&platform=pc"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(fixture("stats-summary.json"), MediaType.APPLICATION_JSON));

        StatsSummary s = client.statsSummary("TeKrop-2217", "competitive", "pc");

        assertEquals(12702, s.general().gamesPlayed());                     // snake_case → camelCase
        assertEquals(5868683L, s.general().timePlayed());
        assertEquals(7.19, s.general().average().deaths());
        assertEquals(1211, s.heroes().get("ana").gamesPlayed());            // heroes keyed by hero key
        assertEquals(6799.24, s.heroes().get("ana").average().healing());
        assertEquals(3, s.roles().size());
        server.verify();
    }

    @Test
    void leavesOutThePlatformWhenBothAreWanted() {
        server.expect(requestTo(BASE + "/players/Player-1234/stats/summary?gamemode=quickplay"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertNull(client.statsSummary("Player-1234", "quickplay", null).general());   // private profile shape
        server.verify();
    }

    @Test
    void readsRanksFromTheSummary() {
        server.expect(requestTo(BASE + "/players/Player-1234/summary"))
                .andRespond(withSuccess("""
                        {"username":"Player","avatar":null,"competitive":{"pc":{"season":22,
                         "tank":null,"damage":{"division":"gold","tier":3,"role_icon":"x","rank_icon":"y","tier_icon":"z"},
                         "support":null,"open":null},"console":null},"last_updated_at":1}""", MediaType.APPLICATION_JSON));
        OverFastApi.PlayerSummary summary = client.summary("Player-1234");
        assertEquals("gold", summary.competitive().pc().damage().division());
        assertEquals(22, summary.competitive().pc().season());
    }

    @Test
    void aNotFoundCarriesRetryAfter() {
        server.expect(requestTo(BASE + "/players/Nobody-1/summary"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Player not found\",\"retry_after\":600,\"next_check_at\":1,\"check_count\":1}"));
        OverFastException e = assertThrows(OverFastException.class, () -> client.summary("Nobody-1"));
        assertEquals(404, e.status());
        assertEquals("Player not found", e.getMessage());
        assertEquals(600, e.retryAfterSeconds());
    }

    @Test
    void aRateLimitReadsTheRetryAfterHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", "5");
        server.expect(requestTo(BASE + "/players?name=Player"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body("rate limited"));
        OverFastException e = assertThrows(OverFastException.class, () -> client.search("Player"));
        assertEquals(429, e.status());
        assertEquals(5, e.retryAfterSeconds());
    }
}
