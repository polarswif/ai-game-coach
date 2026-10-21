package com.gamecoach.datasource.overfast;

import com.gamecoach.datasource.overfast.OverFastApi.PlayerSummary;
import com.gamecoach.datasource.overfast.OverFastApi.SearchResult;
import com.gamecoach.datasource.overfast.OverFastApi.StatsSummary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Calls OverFast API over HTTP with Spring's RestClient. It needs no key, so it always exists. */
@Component
public class RestClientOverFastClient implements OverFastClient {

    private static final Pattern RETRY_AFTER = Pattern.compile("\"retry_after\"\\s*:\\s*(\\d+)");
    private static final Pattern ERROR = Pattern.compile("\"(?:error|detail)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final RestClient http;

    @Autowired
    public RestClientOverFastClient(OverFastProperties properties) {
        this(properties, RestClient.builder().requestFactory(withTimeouts(properties.timeout())));
    }

    /** For tests: a builder wired to MockRestServiceServer, so nothing leaves the machine. */
    RestClientOverFastClient(OverFastProperties properties, RestClient.Builder builder) {
        this.http = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public PlayerSummary summary(String playerId) {
        return call(() -> http.get().uri("/players/{id}/summary", playerId)
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(PlayerSummary.class));
    }

    @Override
    public StatsSummary statsSummary(String playerId, String gamemode, String platform) {
        // {id} placeholders are URL-encoded by RestClient, so odd characters in a name are safe.
        return call(() -> http.get()
                .uri(builder -> {
                    builder.path("/players/{id}/stats/summary").queryParam("gamemode", gamemode);
                    if (platform != null) {
                        builder.queryParam("platform", platform);
                    }
                    return builder.build(playerId);
                })
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(StatsSummary.class));
    }

    @Override
    public SearchResult search(String name) {
        return call(() -> http.get()
                .uri(builder -> builder.path("/players").queryParam("name", "{name}").build(name))
                .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                .body(SearchResult.class));
    }

    /** Runs one request, turning "no answer at all" into an OverFastException with status 0. */
    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException e) {
            throw new OverFastException(0, "Couldn't reach OverFast API: " + e.getMessage(), null, e);
        }
    }

    /**
     * Error bodies look like {"error":"Player not found","retry_after":600,...}. A regex reads the
     * two fields, so a malformed body can't cause a second failure.
     */
    private static OverFastException toException(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        Matcher error = ERROR.matcher(body);
        Matcher retry = RETRY_AFTER.matcher(body);
        Integer retryAfter = retry.find() ? Integer.valueOf(retry.group(1)) : headerSeconds(response);
        return new OverFastException(status, error.find() ? error.group(1) : "HTTP " + status, retryAfter, null);
    }

    private static Integer headerSeconds(ClientHttpResponse response) {
        String value = response.getHeaders().getFirst("Retry-After");
        try {
            return value == null ? null : Integer.valueOf(value.strip());
        } catch (NumberFormatException e) {
            return null;                       // an HTTP date instead of seconds: ignore it
        }
    }

    private static SimpleClientHttpRequestFactory withTimeouts(Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
