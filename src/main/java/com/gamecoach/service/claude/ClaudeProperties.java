package com.gamecoach.service.claude;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings for the Claude coach, read from application.properties keys that start with
 * "gamecoach.claude." (for example gamecoach.claude.model). Spring fills in this record at startup.
 *
 * @param apiKey    the Anthropic API key. application.properties points this at the ANTHROPIC_API_KEY
 *                  environment variable, so the key never goes into the code or into Git
 * @param model     which Claude model to call; a cheaper one (claude-haiku-5-5) cuts cost
 * @param maxTokens the most output tokens one coaching reply may use. This caps the cost of each call
 * @param baseUrl   the API address; only tests change it
 * @param timeout   how long to wait for a reply before giving up
 */
@ConfigurationProperties(prefix = "gamecoach.claude")
public record ClaudeProperties(
        String apiKey,
        String model,
        Integer maxTokens,
        String baseUrl,
        Duration timeout) {

    public static final String DEFAULT_MODEL = "claude-sonnet-5-5";

    /** A "compact constructor": runs before the fields are set, so missing settings get defaults here. */
    public ClaudeProperties {
        if (model == null || model.isBlank()) {
            model = DEFAULT_MODEL;
        }
        if (maxTokens == null || maxTokens <= 0) {
            maxTokens = 4000;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.anthropic.com";
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(60);
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Records print every field by default, which would put the key in logs and error messages.
     * This version hides it.
     */
    @Override
    public String toString() {
        return "ClaudeProperties[apiKey=" + (hasApiKey() ? "(set)" : "(missing)") + ", model=" + model
                + ", maxTokens=" + maxTokens + ", baseUrl=" + baseUrl + ", timeout=" + timeout + "]";
    }
}
