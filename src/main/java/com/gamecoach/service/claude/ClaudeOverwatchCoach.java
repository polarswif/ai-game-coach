package com.gamecoach.service.claude;

import com.gamecoach.analysis.OverwatchHeroNotesCatalog;
import com.gamecoach.analysis.OverwatchPlayerAnalysis;
import com.gamecoach.model.OverwatchMatch.HeroTime;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.service.CoachingArchive;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Overwatch coaching written by Claude: the same plumbing and answer tool as the Rivals coach, with
 * an Overwatch prompt. Saved per match version and player, so asking twice doesn't pay twice.
 */
@Service
@ConditionalOnProperty(name = "gamecoach.coach.provider", havingValue = "claude")
public class ClaudeOverwatchCoach implements OverwatchCoachingService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeOverwatchCoach.class);

    private final ClaudeClient client;
    private final ClaudeProperties properties;
    private final OverwatchHeroNotesCatalog heroNotes;
    private final CoachingArchive archive;

    public ClaudeOverwatchCoach(ClaudeClient client, ClaudeProperties properties, OverwatchHeroNotesCatalog heroNotes,
                                CoachingArchive archive) {
        this.client = client;
        this.properties = properties;
        this.heroNotes = heroNotes;
        this.archive = archive;
    }

    @Override
    public Feedback coach(OverwatchMatch match, OverwatchPlayerAnalysis analysis) {
        String who = analysis.player() != null ? "name:" + analysis.player().toLowerCase()
                : "row:" + analysis.team() + ":" + analysis.hero();
        Optional<Feedback> saved = archive.find(match.matchId(), match, who);
        if (saved.isPresent()) {
            return saved.get();
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
        archive.save(match.matchId(), match, who, feedback);
        return feedback;
    }

    /** Package-private so tests can check the request without sending it. */
    MessagesRequest buildRequest(OverwatchMatch match, OverwatchPlayerAnalysis analysis) {
        return new MessagesRequest(
                properties.model(),
                properties.maxTokens(),
                OverwatchCoachingPrompt.systemPrompt(),
                List.of(Message.user(OverwatchCoachingPrompt.userMessage(match, analysis, notesFor(analysis)))),
                List.of(CoachingPrompt.tool()),
                ToolChoice.auto());
    }

    /** Notes for every hero the coached player played, in play order; heroes without notes are left out. */
    private Map<String, List<String>> notesFor(OverwatchPlayerAnalysis analysis) {
        Map<String, List<String>> notes = new LinkedHashMap<>();
        for (HeroTime h : analysis.heroesPlayed()) {
            List<String> forHero = heroNotes.forHero(h.hero());
            if (!forHero.isEmpty()) {
                notes.putIfAbsent(h.hero(), forHero);
            }
        }
        return notes;
    }
}
