package com.gamecoach.service.claude;

import com.gamecoach.analysis.OverwatchHeroCatalog;
import com.gamecoach.model.OverwatchMatch;
import com.gamecoach.parsing.OverwatchMatchReader;
import com.gamecoach.parsing.OverwatchMatchValidator;
import com.gamecoach.parsing.OverwatchScreens;
import com.gamecoach.parsing.ParsedOverwatchMatch;
import com.gamecoach.parsing.ScoreboardUnreadableException;
import com.gamecoach.parsing.Screenshot;
import com.gamecoach.service.CoachingUnavailableException;
import com.gamecoach.service.claude.ClaudeApi.ContentBlock;
import com.gamecoach.service.claude.ClaudeApi.ImagePart;
import com.gamecoach.service.claude.ClaudeApi.Message;
import com.gamecoach.service.claude.ClaudeApi.MessagesRequest;
import com.gamecoach.service.claude.ClaudeApi.MessagesResponse;
import com.gamecoach.service.claude.ClaudeApi.TextPart;
import com.gamecoach.service.claude.ClaudeApi.ToolChoice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Reads Overwatch match report screenshots with Claude's vision. All screens go in one request,
 * each preceded by a label, so Claude can cross-check them: the Summary's totals pick out the
 * uploader's row and name their hero in text. Active with gamecoach.coach.provider=claude.
 */
@Service
@ConditionalOnProperty(name = "gamecoach.coach.provider", havingValue = "claude")
public class ClaudeOverwatchReader implements OverwatchMatchReader {

    private static final Logger log = LoggerFactory.getLogger(ClaudeOverwatchReader.class);

    /** Ten rows plus hero tiles; the same room the Rivals reader gets. */
    static final int MAX_TOKENS = 8000;

    private final ClaudeClient client;
    private final ClaudeProperties properties;
    private final OverwatchHeroCatalog heroes;
    private final OverwatchMatchValidator validator;

    public ClaudeOverwatchReader(ClaudeClient client, ClaudeProperties properties, OverwatchHeroCatalog heroes,
                                 OverwatchMatchValidator validator) {
        this.client = client;
        this.properties = properties;
        this.heroes = heroes;
        this.validator = validator;
    }

    @Override
    public ParsedOverwatchMatch read(OverwatchScreens screens, String matchId) {
        MessagesResponse response;
        try {
            response = client.send(buildRequest(screens));
        } catch (ClaudeApiException e) {
            throw new CoachingUnavailableException(ClaudeErrors.explain(e), e);
        }
        if (response.usage() != null) {
            log.info("Read Overwatch match {} ({} images) with {}: {} input + {} output tokens", matchId,
                    1 + (screens.summary() == null ? 0 : 1) + screens.personal().size(), response.model(),
                    response.usage().inputTokens(), response.usage().outputTokens());
        }
        if ("max_tokens".equals(response.stopReason())) {
            throw new CoachingUnavailableException("The screenshot reply was cut off before it finished.", null);
        }

        Map<String, Object> input = toolInput(response);
        List<String> warnings = new ArrayList<>(ToolInput.strings(input, "warnings"));
        OverwatchMatch match = OverwatchToolInput.toMatch(input, matchId, warnings);
        if (match.players().isEmpty()) {
            throw new ScoreboardUnreadableException("No scoreboard rows could be read from the Teams screen"
                    + (warnings.isEmpty() ? "." : ": " + ToolInput.firstSentence(warnings.get(0))));
        }
        warnings.addAll(validator.check(match));
        return new ParsedOverwatchMatch(match, List.copyOf(warnings));
    }

    /** Label, image, label, image..., then the instructions. Package-private for tests. */
    MessagesRequest buildRequest(OverwatchScreens screens) {
        List<Object> parts = new ArrayList<>();
        addImage(parts, "TEAMS screen:", screens.teams());
        if (screens.summary() != null) {
            addImage(parts, "SUMMARY screen:", screens.summary());
        }
        for (int i = 0; i < screens.personal().size(); i++) {
            addImage(parts, "PERSONAL screen " + (i + 1) + ":", screens.personal().get(i));
        }
        parts.add(TextPart.of(OverwatchReaderPrompt.instructions(heroes.heroNames(), screens.summary() != null,
                screens.personal().size())));
        return new MessagesRequest(
                properties.model(),
                MAX_TOKENS,
                OverwatchReaderPrompt.systemPrompt(),
                List.of(new Message("user", parts)),
                List.of(OverwatchReaderPrompt.tool()),
                ToolChoice.auto());
    }

    private static void addImage(List<Object> parts, String label, Screenshot image) {
        parts.add(TextPart.of(label));
        parts.add(ImagePart.base64(image.mediaType(), Base64.getEncoder().encodeToString(image.bytes())));
    }

    private static Map<String, Object> toolInput(MessagesResponse response) {
        if (response.content() != null) {
            for (ContentBlock block : response.content()) {
                if ("tool_use".equals(block.type()) && OverwatchReaderPrompt.TOOL_NAME.equals(block.name())
                        && block.input() != null) {
                    return ToolInput.repairLeakedParameters(block.input());
                }
            }
        }
        throw new ScoreboardUnreadableException("Claude didn't return a match (stop reason: "
                + response.stopReason() + "). Try clearer screenshots of the match report.");
    }
}
