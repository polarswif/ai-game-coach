package com.gamecoach.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Who is making the current request, read from Spring Security's per-request context. Empty when
 * nobody is signed in, and in code that runs outside a web request (unit tests, startup).
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** The signed-in account's username key (lower case), the value stored as a match's owner. */
    public static Optional<String> username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.ofNullable(auth.getName()).map(AppUser::key);
    }

    /**
     * Whether the current user may see something owned by {@code owner}: their own things, things from
     * before accounts existed (no owner), and everything when no one is signed in (tests, startup).
     */
    public static boolean canSee(String owner) {
        Optional<String> me = username();
        return owner == null || me.isEmpty() || owner.equals(me.get());
    }
}
