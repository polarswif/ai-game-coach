package com.gamecoach.parsing;

import java.util.List;

/**
 * A row whose hero the reader wasn't sure of. Portraits are the hardest part of a screenshot (small,
 * alternate costumes, heroes newer than the model), and a wrong hero ruins the coaching, so the
 * review page highlights these rows and offers the alternatives as one-click fixes.
 *
 * @param index        position of the row in the match's player list (what the review page edits)
 * @param player       the player's name, for messages; null when hidden
 * @param hero         the hero the reader picked
 * @param alternatives other heroes it could be, most likely first, all valid hero names; may be empty
 * @param seen         what the reader saw in the portrait, in a few words; null when not given
 */
public record HeroDoubt(int index, String player, String hero, List<String> alternatives, String seen) {

    public HeroDoubt {
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
    }

    /** The doubts without one row's: used when the player told us their own hero. */
    public static List<HeroDoubt> without(List<HeroDoubt> doubts, int index) {
        return doubts.stream().filter(d -> d.index() != index).toList();
    }
}
