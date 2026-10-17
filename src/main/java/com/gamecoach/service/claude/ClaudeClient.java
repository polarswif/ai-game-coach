package com.gamecoach.service.claude;

import com.gamecoach.service.claude.ClaudeApi.MessagesRequest;
import com.gamecoach.service.claude.ClaudeApi.MessagesResponse;

/**
 * Sends one request to the Claude Messages API. It's an interface so ClaudeCoachingService doesn't
 * care how the request travels: RestClientClaudeClient does real HTTP, and tests pass in a fake that
 * returns a canned reply, so tests never need a key or spend money.
 */
public interface ClaudeClient {

    /** Throws ClaudeApiException when the API returns an error or can't be reached. */
    MessagesResponse send(MessagesRequest request);
}
