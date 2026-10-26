package com.gamecoach.security;

/** A wrong username or password. Mapped to 401. */
public class LoginFailedException extends RuntimeException {
    public LoginFailedException(String message) {
        super(message);
    }
}
