package com.gamecoach.analysis;

import com.gamecoach.model.PlayerLine;

import java.util.function.ToIntFunction;

/** A scoreboard number the analyzer compares across players. */
public enum Stat {
    KILLS(PlayerLine::kills, true),
    DEATHS(PlayerLine::deaths, false),   // fewer is better
    ASSISTS(PlayerLine::assists, true),
    FINAL_HITS(PlayerLine::finalHits, true),
    DAMAGE(PlayerLine::damage, true),
    BLOCKED(PlayerLine::blocked, true),
    HEALING(PlayerLine::healing, true);

    private final ToIntFunction<PlayerLine> reader;
    private final boolean higherIsBetter;

    Stat(ToIntFunction<PlayerLine> reader, boolean higherIsBetter) {
        this.reader = reader;
        this.higherIsBetter = higherIsBetter;
    }

    int read(PlayerLine player) {
        return reader.applyAsInt(player);
    }

    boolean higherIsBetter() {
        return higherIsBetter;
    }
}
