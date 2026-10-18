package com.gamecoach.service.claude;

/** Turns Claude API failures into messages a user can act on. Shared by the coach and the screenshot reader. */
final class ClaudeErrors {

    private ClaudeErrors() {
    }

    /** A message by HTTP status. Never includes the key itself. */
    static String explain(ClaudeApiException e) {
        return switch (e.status()) {
            case 0 -> "Couldn't reach the Claude API. Check the internet connection and try again.";
            case 401 -> "The Claude API rejected the API key. Check ANTHROPIC_API_KEY.";
            case 403 -> "The API key isn't allowed to use this model: " + e.getMessage();
            case 413 -> "The request was too large for the Claude API: " + e.getMessage();
            case 429 -> "Too many requests to the Claude API (rate or spend limit). Wait a bit and try again.";
            case 529 -> "The Claude API is overloaded right now. Try again shortly.";
            default -> "The Claude API returned an error (" + e.status() + " " + e.errorType() + "): " + e.getMessage();
        };
    }
}
