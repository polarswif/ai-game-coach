package com.gamecoach.service;

/**
 * Coaching couldn't be produced right now (the AI service failed, rejected the key, or gave an
 * unusable reply). ApiExceptionHandler turns it into a 503 response with this message.
 */
public class CoachingUnavailableException extends RuntimeException {

    public CoachingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
