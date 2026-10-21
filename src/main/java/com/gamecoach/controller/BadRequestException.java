package com.gamecoach.controller;

/** A request parameter the user can fix (a misspelled mode, an unknown platform). Mapped to 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
