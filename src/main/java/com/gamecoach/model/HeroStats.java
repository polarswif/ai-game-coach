package com.gamecoach.model;

/** One hero a player used during a match, with the K/D/A they got on that hero. */
public record HeroStats(
        String hero,
        int kills,
        int deaths,
        int assists) {
}
