package com.gamecoach.service.claude;

/**
 * The Claude API returned an error, or couldn't be reached.
 *
 * @see <a href="https://platform.claude.com/docs/en/api/errors">API errors</a>
 */
public class ClaudeApiException extends RuntimeException {

    private final int status;
    private final String errorType;

    /**
     * @param status    the HTTP status, e.g. 401 for a bad key or 429 for rate limits; 0 when the
     *                  request never got an answer (network problem or timeout)
     * @param errorType the API's error type, e.g. "authentication_error"
     */
    public ClaudeApiException(int status, String errorType, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorType = errorType;
    }

    public int status() {
        return status;
    }

    public String errorType() {
        return errorType;
    }
}
