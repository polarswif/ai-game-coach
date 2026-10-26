package com.gamecoach.service.claude;

import com.gamecoach.analysis.HeroRoleCatalog;
import com.gamecoach.analysis.TeamUpCatalog;
import com.gamecoach.model.MatchDetails;
import com.gamecoach.model.RoundScore;
import com.gamecoach.parsing.HeroDoubt;
import com.gamecoach.parsing.ParsedScoreboard;
import com.gamecoach.parsing.ScoreboardParser;
import com.gamecoach.parsing.ScoreboardUnreadableException;
import com.gamecoach.parsing.ScoreboardValidator;
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
 * Reads a scoreboard screenshot with Claude's vision: sends the image and the instructions, gets
 * the rows back through the submit_scoreboard tool, converts them to MatchDetails, and runs the
 * consistency checks. Active with gamecoach.coach.provider=claude, since it needs the same API key.
 */
@Service
@ConditionalOnProperty(name = "gamecoach.coach.provider", havingValue = "claude")
public class ClaudeScoreboardParser implements ScoreboardParser {

    private static final Logger log = LoggerFactory.getLogger(ClaudeScoreboardParser.class);

    /** Twelve rows of JSON need more room than a coaching reply. */
    static final int MAX_TOKENS = 16000;

    private final ClaudeClient client;
    private final ClaudeProperties properties;
    private final HeroRoleCatalog roles;
    private final ScoreboardValidator validator;
    private final HeroPortraitSheet portraits;
    private final TeamUpCatalog teamUps;

    public ClaudeScoreboardParser(ClaudeClient client, ClaudeProperties properties, HeroRoleCatalog roles,
                                  ScoreboardValidator validator, HeroPortraitSheet portraits, TeamUpCatalog teamUps) {
        this.client = client;
        this.properties = properties;
        this.roles = roles;
        this.validator = validator;
        this.portraits = portraits;
        this.teamUps = teamUps;
    }

    @Override
    public ParsedScoreboard parse(Screenshot screenshot, String matchId, RoundScore score) {
        MessagesResponse response;
        try {
            response = client.send(buildRequest(screenshot));
        } catch (ClaudeApiException e) {
            throw new CoachingUnavailableException(ClaudeErrors.explain(e), e);
        }
        if (response.usage() != null) {
            log.info("Read scoreboard {} with {}: {} input + {} output tokens{}", matchId, response.model(),
                    response.usage().inputTokens(), response.usage().outputTokens(), textNote(response));
        }
        if ("max_tokens".equals(response.stopReason())) {
            throw new CoachingUnavailableException("The scoreboard reply was cut off before it finished.", null);
        }

        Map<String, Object> input = toolInput(response);
        List<String> warnings = new ArrayList<>(ToolInput.strings(input, "warnings"));
        MatchDetails match = officialHeroNames(ScoreboardToolInput.toMatch(input, matchId, warnings));
        List<HeroDoubt> doubts = PortraitCheck.doubts(ToolInput.objects(input, "players"), roles::officialName);
        if (match.players().isEmpty()) {
            throw new ScoreboardUnreadableException("No scoreboard rows could be read from this image"
                    + (warnings.isEmpty() ? "." : ": " + ToolInput.firstSentence(warnings.get(0))));
        }
        if (score != null) {
            match = match.withScore(score);
        }
        warnings.addAll(validator.check(match));
        return new ParsedScoreboard(match, List.copyOf(warnings), doubts);
    }

    /** Package-private so tests can check the request without sending it. */
    /**
     * The screenshot, a zoomed copy of its portraits when it can be made, the labeled sheet of every
     * hero's portrait to compare against, then the instructions.
     */
    MessagesRequest buildRequest(Screenshot screenshot) {
        List<Object> parts = new ArrayList<>();
        parts.add(image(screenshot));
        PortraitZoom.leftStrip(screenshot, ScoreboardPrompt.ZOOM_FRACTION).ifPresent(zoom -> {
            parts.add(TextPart.of(ScoreboardPrompt.ZOOM_LABEL));
            parts.add(image(zoom));
        });
        portraits.image().ifPresent(sheet -> {
            parts.add(TextPart.of(ScoreboardPrompt.SHEET_LABEL));
            parts.add(image(sheet));
        });
        parts.add(TextPart.of(ScoreboardPrompt.instructions(roles.heroNames(), teamUps::partnersOf)));
        return new MessagesRequest(
                properties.model(),
                MAX_TOKENS,
                ScoreboardPrompt.systemPrompt(),
                List.of(new Message("user", parts)),
                List.of(ScoreboardPrompt.tool()),
                ToolChoice.auto());
    }

    /** "loki (Lady Loki costume)" → "Loki": one spelling per hero for the rest of the app. */
    private MatchDetails officialHeroNames(MatchDetails match) {
        return match.withPlayers(match.players().stream()
                .map(p -> roles.officialName(p.lastHero()).filter(n -> !n.equals(p.lastHero()))
                        .map(p::withLastHero).orElse(p))
                .toList());
    }

    /** ", plus N characters of text" when Claude wrote prose besides the tool call (it costs tokens). */
    private static String textNote(MessagesResponse response) {
        int chars = 0;
        if (response.content() != null) {
            for (ContentBlock block : response.content()) {
                if ("text".equals(block.type()) && block.text() != null) {
                    chars += block.text().length();
                }
            }
        }
        return chars == 0 ? "" : " (plus " + chars + " characters of text outside the tool call)";
    }

    private static ImagePart image(Screenshot screenshot) {
        return ImagePart.base64(screenshot.mediaType(), Base64.getEncoder().encodeToString(screenshot.bytes()));
    }

    private static Map<String, Object> toolInput(MessagesResponse response) {
        if (response.content() != null) {
            for (ContentBlock block : response.content()) {
                if ("tool_use".equals(block.type()) && ScoreboardPrompt.TOOL_NAME.equals(block.name())
                        && block.input() != null) {
                    return ToolInput.repairLeakedParameters(block.input());
                }
            }
        }
        throw new ScoreboardUnreadableException("Claude didn't return a scoreboard (stop reason: "
                + response.stopReason() + "). Try a clearer screenshot of the full scoreboard.");
    }
}
