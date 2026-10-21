package com.gamecoach.analysis;

import com.gamecoach.model.OverwatchPlayerLine;

import java.util.function.ToIntFunction;

/** A Teams-screen column the analyzer compares across players. */
public enum OverwatchStat {
    ELIMINATIONS(OverwatchPlayerLine::eliminations, true),
    ASSISTS(OverwatchPlayerLine::assists, true),
    DEATHS(OverwatchPlayerLine::deaths, false),   // fewer is better
    DAMAGE(OverwatchPlayerLine::damage, true),
    HEALING(OverwatchPlayerLine::healing, true),
    MITIGATION(OverwatchPlayerLine::mitigation, true);

    private final ToIntFunction<OverwatchPlayerLine> reader;
    private final boolean higherIsBetter;

    OverwatchStat(ToIntFunction<OverwatchPlayerLine> reader, boolean higherIsBetter) {
        this.reader = reader;
        this.higherIsBetter = higherIsBetter;
    }

    int read(OverwatchPlayerLine player) {
        return reader.applyAsInt(player);
    }

    boolean higherIsBetter() {
        return higherIsBetter;
    }
}
