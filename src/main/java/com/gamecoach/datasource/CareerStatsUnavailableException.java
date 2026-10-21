package com.gamecoach.datasource;

/** The career stats provider couldn't answer (offline, rate-limited, broken). Mapped to 503. */
public class CareerStatsUnavailableException extends RuntimeException {

    public CareerStatsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
