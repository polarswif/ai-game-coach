package com.gamecoach.service.claude;

import com.gamecoach.analysis.HeroNotesCatalog;
import com.gamecoach.analysis.HeroPlayed;
import com.gamecoach.analysis.PlayerAnalysis;
import com.gamecoach.model.Feedback;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.service.CoachingArchive;
import com.gamecoach.service.CoachingService;
import com.gamecoach.service.CoachingUnavailableException;
import com.gamecoach.service.claude.ClaudeApi.ContentBlock;
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
 * Coaching written by Claude. Sends the match and the Java-computed analysis, asks Claude to answer
 * through the submit_coaching tool, and turns that into Feedback.
 * Results are saved per match version and player (CoachingArchive, the database in the app), so
 * asking twice doesn't pay twice, even after a restart.
 * Active when application.properties has gamecoach.coach.provider=claude.
 */
@Service
@ConditionalOnProperty(name = "gamecoach.coach.provider", havingValue = "claude")
public class ClaudeCoachingService implements CoachingService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCoachingService.class);

    private final ClaudeClient client;
    private final ClaudeProperties properties;
    private final HeroNotesCatalog heroNotes;
    private final CoachingArchive archive;

    public ClaudeCoachingService(ClaudeClient client, ClaudeProperties properties, HeroNotesCatalog heroNotes,
                                 CoachingArchive archive) {
        this.client = client;
        this.properties = properties;
        this.heroNotes = heroNotes;
        this.archive = archive;
    }

    @Override
    public Feedback coach(MatchDetails match, PlayerAnalysis analysis) {
        String who = playerKey(analysis);
        Optional<Feedback> saved = archive.find(match.matchId(), match, who);
        if (saved.isPresent()) {
            return saved.get();
        }
        Feedback feedback = askClaude(match, analysis);
        archive.save(match.matchId(), match, who, feedback);
        return feedback;
    }

    /** Package-private so tests can check the exact request without sending it. */
    MessagesRequest buildRequest(MatchDetails match, PlayerAnalysis analysis) {
        return new MessagesRequest(
                properties.model(),
                properties.maxTokens(),
                CoachingPrompt.systemPrompt(),
                List.of(Message.user(CoachingPrompt.userMessage(match, analysis, notesFor(analysis)))),
                List.of(CoachingPrompt.tool()),
                ToolChoice.auto());
    }

    /**
     * Notes for every hero the player played, keyed by hero. LinkedHashMap keeps the order the heroes
     * were played in; heroes without notes are left out.
     */
    private Map<String, List<String>> notesFor(PlayerAnalysis analysis) {
        Map<String, List<String>> notes = new LinkedHashMap<>();
        for (HeroPlayed h : analysis.heroes()) {
            List<String> forHero = heroNotes.forHero(h.hero());
            if (!forHero.isEmpty()) {
                notes.putIfAbsent(h.hero(), forHero);
            }
        }
        return notes;
    }

    private Feedback askClaude(MatchDetails match, PlayerAnalysis analysis) {
        MessagesResponse response;
        try {
            response = client.send(buildRequest(match, analysis));
        } catch (ClaudeApiException e) {
            throw new CoachingUnavailableException(ClaudeErrors.explain(e), e);
        }
        if (response.usage() != null) {
            log.info("Coached match {} with {}: {} input + {} output tokens", match.matchId(), response.model(),
                    response.usage().inputTokens(), response.usage().outputTokens());
        }
        return readFeedback(response, properties.maxTokens());
    }

    /**
     * Finds the submit_coaching call in the reply and checks it's usable. Static and package-private
     * because the Overwatch coach answers through the same tool.
     */
    static Feedback readFeedback(MessagesResponse response, int maxTokens) {
        if ("max_tokens".equals(response.stopReason())) {
            // Log any text Claude wrote before (or instead of) the tool call: that's usually where
            // the budget went, and it shows whether the prompt or the limit needs changing.
            String text = response.content() == null ? "" : response.content().stream()
                    .filter(block -> "text".equals(block.type()) && block.text() != null)
                    .map(ContentBlock::text)
                    .collect(java.util.stream.Collectors.joining(" "));
            log.warn("Coaching reply hit max_tokens ({}). Text outside the tool call ({} chars): {}",
                    maxTokens, text.length(), text.length() > 500 ? text.substring(0, 500) + "..." : text);
            throw new CoachingUnavailableException("The coaching reply was cut off. Raise "
                    + "gamecoach.claude.max-tokens (now " + maxTokens + ").", null);
        }
        Feedback feedback = response.content() == null ? null : response.content().stream()
                .filter(block -> "tool_use".equals(block.type()) && CoachingPrompt.TOOL_NAME.equals(block.name()))
                .map(ContentBlock::input)
                .filter(input -> input != null)
                .map(ToolInput::repairLeakedParameters)
                .map(input -> new Feedback(ToolInput.strings(input, "strengths"), ToolInput.strings(input, "weaknesses"),
                        ToolInput.strings(input, "advice"), ToolInput.string(input, "matchContext")))
                .findFirst()
                .orElse(null);
        // Strengths or weaknesses may honestly be empty (a rough loss can have no top-half stat);
        // a reply with no advice, or with neither strengths nor weaknesses, is not usable.
        if (feedback == null || isEmpty(feedback.advice())
                || (isEmpty(feedback.strengths()) && isEmpty(feedback.weaknesses()))) {
            log.warn("Unusable coaching reply (stop reason {}): {}", response.stopReason(), response.content());
            throw new CoachingUnavailableException("Claude's reply didn't contain usable coaching "
                    + "(stop reason: " + response.stopReason() + ").", null);
        }
        return feedback;
    }

    private static boolean isEmpty(List<String> items) {
        return items == null || items.stream().allMatch(s -> s == null || s.isBlank());
    }

    /** Who was coached. Hidden names fall back to team plus last hero, unique enough within one match. */
    private static String playerKey(PlayerAnalysis a) {
        return a.player() != null ? "name:" + a.player().toLowerCase()
                : "row:" + a.team() + ":" + a.heroes().stream().filter(HeroPlayed::lastPlayed)
                        .map(HeroPlayed::hero).findFirst().orElse("?");
    }
}
