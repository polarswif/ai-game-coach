package com.gamecoach.analysis;

import com.gamecoach.model.Role;

/** A hero the player used, with its role filled in from the role table. */
public record HeroPlayed(
        String hero,
        Role role,
        int kills,
        int deaths,
        int assists) {
}
