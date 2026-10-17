package com.gamecoach.service.claude;

import com.gamecoach.model.Feedback;
import com.gamecoach.service.claude.ClaudeApi.Message;
import com.gamecoach.service.claude.ClaudeApi.MessagesRequest;
import com.gamecoach.service.claude.ClaudeApi.MessagesResponse;
import com.gamecoach.service.claude.ClaudeApi.ToolChoice;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Checks the real HTTP code against MockRestServiceServer, a fake server inside the test: it verifies
 * the URL, headers and JSON we send, and plays back a reply. Nothing leaves the machine.
 */
class RestClientClaudeClientTest {

    private static final String BASE = "https://api.anthropic.test";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final RestClientClaudeClient client =
            new RestClientClaudeClient(new ClaudeProperties("test-key", null, null, BASE, null), builder);

    private static MessagesRequest request() {
        return new MessagesRequest("claude-sonnet-5-5", 2000, "system prompt",
                List.of(Message.user("match data")), List.of(CoachingPrompt.tool()),
                ToolChoice.auto());
    }

    @Test
    void sendsTheRightRequestAndReadsTheToolCall() {
        server.expect(requestTo(BASE + "/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(jsonPath("$.model").value("claude-sonnet-5-5"))
                .andExpect(jsonPath("$.max_tokens").value(2000))                 // snake_case names on the wire
                .andExpect(jsonPath("$.tool_choice.type").value("auto"))
                .andExpect(jsonPath("$.tool_choice.name").doesNotExist())       // null name left out
                .andExpect(jsonPath("$.tools[0].input_schema.type").value("object"))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andRespond(withSuccess("""
                        {"id":"msg_1","type":"message","role":"assistant","model":"claude-sonnet-5-5",
                         "content":[{"type":"tool_use","id":"toolu_1","name":"submit_coaching",
                           "input":{"strengths":["Good healing"],"weaknesses":["Died early"],"advice":["Use cover"]}}],
                         "stop_reason":"tool_use","stop_sequence":null,
                         "usage":{"input_tokens":1200,"output_tokens":150,"cache_read_input_tokens":0}}
                        """, MediaType.APPLICATION_JSON));

        MessagesResponse response = client.send(request());

        server.verify();
        assertEquals("tool_use", response.stopReason());
        assertEquals(new Feedback(List.of("Good healing"), List.of("Died early"), List.of("Use cover")),
                response.content().get(0).input());
        assertEquals(1200, response.usage().inputTokens());   // unknown fields like cache_read_input_tokens are ignored
    }

    @Test
    void errorRepliesBecomeClaudeApiException() {
        server.expect(requestTo(BASE + "/v1/messages"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}
                        """));

        ClaudeApiException e = assertThrows(ClaudeApiException.class, () -> client.send(request()));
        assertEquals(401, e.status());
        assertEquals("authentication_error", e.errorType());
        assertEquals("invalid x-api-key", e.getMessage());
    }

    @Test
    void refusesToStartWithoutAKey() {
        assertThrows(IllegalStateException.class,
                () -> new RestClientClaudeClient(new ClaudeProperties("", null, null, BASE, null), RestClient.builder()));
    }
}
