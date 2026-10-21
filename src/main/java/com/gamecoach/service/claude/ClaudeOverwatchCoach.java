package com.gamecoach.service.claude;

import com.gamecoach.analysis.OverwatchPlayerAnalysis;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.service.CoachingUnavailableException;
import com.gamecoach.service.OverwatchCoachingService;
import com.gamecoach.service.claude.ClaudeApi.Message;
import com.gamecoach.service.claude.ClaudeApi.MessagesRequest;
import com.gamecoach.service.claude.ClaudeApi.MessagesResponse;
import com.gamecoach.service.claude.ClaudeApi.ToolChoice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Overwatch coaching written by Claude: the same plumbing and answer tool as the Rivals coach, with
 * an Overwatch prompt. Cached per match version and player, so asking twice doesn't pay twice.
 */
@Service
@ConditionalOnProperty(name = "gamecoach.coach.provider", havingValue = "claude")
public class ClaudeOverwatchCoach implements OverwatchCoachingService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeOverwatchCoach.class);
    private static final int CACHE_SIZE = 500;

    private final ClaudeClient client;
    private final ClaudeProperties properties;

    private final Map<String, Feedback> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Feedback> eldest) {
                    return size() > CACHE_SIZE;
                }
            });

    public ClaudeOverwatchCoach(ClaudeClient client, ClaudeProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public Feedback coach(OverwatchMatch match, OverwatchPlayerAnalysis analysis) {
        // hashCode covers the whole match record, so a corrected match gets fresh coaching.
        String key = match.matchId() + "#" + Integer.toHexString(match.hashCode()) + "|"
                + (analysis.player() != null ? analysis.player().toLowerCase() : analysis.team() + ":" + analysis.hero());
        Feedback cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        MessagesResponse response;
        try {
            response = client.send(buildRequest(match, analysis));
        } catch (ClaudeApiException e) {
            throw new CoachingUnavailableException(ClaudeErrors.explain(e), e);
        }
        if (response.usage() != null) {
            log.info("Coached Overwatch match {} with {}: {} input + {} output tokens", match.matchId(),
                    response.model(), response.usage().inputTokens(), response.usage().outputTokens());
        }
        Feedback feedback = ClaudeCoachingService.readFeedback(response, properties.maxTokens());
        cache.put(key, feedback);
        return feedback;
    }

    /** Package-private so tests can check the request without sending it. */
    MessagesRequest buildRequest(OverwatchMatch match, OverwatchPlayerAnalysis analysis) {
        return new MessagesRequest(
                properties.model(),
                properties.maxTokens(),
                OverwatchCoachingPrompt.systemPrompt(),
                List.of(Message.user(OverwatchCoachingPrompt.userMessage(match, analysis))),
                List.of(CoachingPrompt.tool()),
                ToolChoice.auto());
    }
}
