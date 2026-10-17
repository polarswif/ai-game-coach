package com.gamecoach.service.claude;

import com.gamecoach.service.claude.ClaudeApi.MessagesRequest;
import com.gamecoach.service.claude.ClaudeApi.MessagesResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calls the Claude Messages API over HTTP using Spring's RestClient.
 * Created only when gamecoach.coach.provider=claude, and refuses to start without an API key, so a
 * missing key shows up immediately at startup instead of on the first coaching request.
 */
@Component
@ConditionalOnProperty(name = "gamecoach.coach.provider", havingValue = "claude")
public class RestClientClaudeClient implements ClaudeClient {

    /** The API version this code was written against, sent with every request. */
    static final String API_VERSION = "2023-06-01";

    private static final Pattern ERROR_TYPE = Pattern.compile("\"type\"\\s*:\\s*\"([a-z_]+_error)\"");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final RestClient http;

    /** The constructor Spring uses (@Autowired picks it when a class has more than one). */
    @Autowired
    public RestClientClaudeClient(ClaudeProperties properties) {
        this(properties, RestClient.builder().requestFactory(withTimeouts(properties.timeout())));
    }

    /**
     * Tests use this one to pass in a builder wired to a mock server (MockRestServiceServer), so no
     * real request leaves the machine.
     */
    RestClientClaudeClient(ClaudeProperties properties, RestClient.Builder builder) {
        if (!properties.hasApiKey()) {
            throw new IllegalStateException("gamecoach.coach.provider=claude needs an API key. Set the "
                    + "ANTHROPIC_API_KEY environment variable, or switch the provider back to fake.");
        }
        this.http = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-api-key", properties.apiKey())
                .defaultHeader("anthropic-version", API_VERSION)
                .build();
    }

    @Override
    public MessagesResponse send(MessagesRequest request) {
        try {
            return http.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)                                   // the record becomes the JSON body
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, response) -> {
                        throw toException(response);                 // 4xx/5xx: turn the error JSON into our exception
                    })
                    .body(MessagesResponse.class);                   // the JSON reply becomes a record
        } catch (ResourceAccessException e) {
            // No HTTP answer at all: offline, DNS failure, or the timeout ran out.
            throw new ClaudeApiException(0, "network_error", "Couldn't reach the Claude API: " + e.getMessage(), e);
        }
    }

    /**
     * Error replies look like {"type":"error","error":{"type":"authentication_error","message":"..."}}.
     * A small regex pulls out the two fields, so a malformed error body can't cause a second failure.
     */
    private static ClaudeApiException toException(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        String type = firstGroup(ERROR_TYPE, body, "api_error");
        String message = firstGroup(ERROR_MESSAGE, body, "HTTP " + status);
        return new ClaudeApiException(status, type, message, null);
    }

    private static String firstGroup(Pattern pattern, String text, String fallback) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : fallback;
    }

    private static SimpleClientHttpRequestFactory withTimeouts(Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
