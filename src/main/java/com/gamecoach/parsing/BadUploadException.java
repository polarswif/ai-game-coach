package com.gamecoach.parsing;

/** The upload is unusable (empty, too large, wrong format, malformed score). ApiExceptionHandler makes it a 400. */
public class BadUploadException extends RuntimeException {

    public BadUploadException(String message) {
        super(message);
    }
}
