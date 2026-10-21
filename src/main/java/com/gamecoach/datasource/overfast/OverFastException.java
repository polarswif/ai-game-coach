package com.gamecoach.datasource.overfast;

/**
 * OverFast answered with an error, or didn't answer at all.
 *
 * @see #status() 0 when there was no HTTP answer (offline, timeout)
 * @see #retryAfterSeconds() from a 404's "retry_after" or a 429's Retry-After header; null if not given
 */
public class OverFastException extends RuntimeException {

    private final int status;
    private final Integer retryAfterSeconds;

    public OverFastException(int status, String message, Integer retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int status() {
        return status;
    }

    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
