package com.gamecoach.service.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gamecoach.model.Feedback;

import java.util.List;
import java.util.Map;

/**
 * The JSON shapes of the Claude Messages API (POST /v1/messages), as Java records.
 * Spring's JSON library (Jackson) turns these records into JSON for the request and turns the
 * response JSON back into records, matching fields by name. Only the fields this app uses are here.
 * Reference: https://platform.claude.com/docs/en/api/messages
 */
public final class ClaudeApi {

    private ClaudeApi() {
        // Only a holder for the records below; never instantiated.
    }

    // ---- Request ----

    /**
     * @param maxTokens  the API calls it "max_tokens"; @JsonProperty sets the JSON name, so the
     *                   Java name can follow Java's camelCase style
     * @param toolChoice how Claude may use the tools (see ToolChoice)
     */
    public record MessagesRequest(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            String system,
            List<Message> messages,
            List<Tool> tools,
            @JsonProperty("tool_choice") ToolChoice toolChoice) {
    }

    /** One turn of the conversation. Here there's only one: the user message with the match data. */
    public record Message(String role, String content) {

        public static Message user(String content) {
            return new Message("user", content);
        }
    }

    /**
     * A tool Claude can call. inputSchema is a JSON Schema describing the tool's input. Here the
     * "tool" is just a structured way to hand back the coaching, so the reply always has the same shape.
     */
    public record Tool(
            String name,
            String description,
            @JsonProperty("input_schema") Map<String, Object> inputSchema) {
    }

    /**
     * How Claude may use the tools. {"type": "auto"} lets Claude decide; the system prompt tells it to
     * always answer through submit_coaching, and ClaudeCoachingService rejects a reply that doesn't.
     * Forcing a tool ({"type": "tool", "name": "..."}) would be stricter, but newer models such as
     * claude-sonnet-5-5 reject it with a 400 error, so it is kept only for models that allow it.
     * NON_NULL leaves "name" out of the JSON when it's null, since "auto" takes no name.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolChoice(String type, String name) {

        public static ToolChoice auto() {
            return new ToolChoice("auto", null);
        }

        public static ToolChoice force(String toolName) {
            return new ToolChoice("tool", toolName);
        }
    }

    // ---- Response ----

    /**
     * The reply. ignoreUnknown means new fields Anthropic adds later won't break parsing.
     *
     * @param stopReason why Claude stopped: "tool_use" is the normal case here; "max_tokens" means the
     *                   reply was cut off before it finished
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MessagesResponse(
            String id,
            String model,
            List<ContentBlock> content,
            @JsonProperty("stop_reason") String stopReason,
            Usage usage) {
    }

    /**
     * One block of the reply. A "tool_use" block has the tool's name and its input; since the
     * submit_coaching tool's input has the same fields as Feedback, Jackson reads it straight into
     * a Feedback. A "text" block has text instead.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text, String name, Feedback input) {
    }

    /** Tokens used, which is what the call costs. Logged so you can watch spending. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens) {
    }
}
