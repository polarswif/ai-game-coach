package com.gamecoach.parsing;

import java.util.List;

/**
 * The Overwatch match report screens uploaded together.
 *
 * @param teams    the Teams tab: every player's row. Required.
 * @param summary  the Summary tab: result, final score, map, mode, length and your heroes; null if not sent
 * @param personal the Personal tab, one image per hero (or the All Heroes tab); empty if not sent
 */
public record OverwatchScreens(Screenshot teams, Screenshot summary, List<Screenshot> personal) {

    /** More hero tabs than this is almost certainly a mistake, and every image costs money to read. */
    public static final int MAX_PERSONAL = 4;

    public OverwatchScreens {
        if (teams == null) {
            throw new BadUploadException("The Teams screen is required: it's the scoreboard with every player.");
        }
        personal = personal == null ? List.of() : List.copyOf(personal);
        if (personal.size() > MAX_PERSONAL) {
            throw new BadUploadException("Send at most " + MAX_PERSONAL + " Personal screenshots (one per hero you played).");
        }
    }
}
