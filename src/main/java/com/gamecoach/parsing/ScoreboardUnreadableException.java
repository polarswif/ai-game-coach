package com.gamecoach.parsing;

/**
 * The image arrived fine but no scoreboard could be read from it (not a scoreboard, too blurry,
 * cropped). ApiExceptionHandler makes it a 422 with this message.
 */
public class ScoreboardUnreadableException extends RuntimeException {

    public ScoreboardUnreadableException(String message) {
        super(message);
    }
}
