package ru.trainithard.dunebot.model;

import java.util.List;

/**
 * Status of <code>Match</code>.
 */
public enum MatchState {
    /**
     * Match is just created.
     */
    NEW,
    /**
     * Submit command was executed for the match, no screenshot uploaded yet
     */
    ON_SUBMIT,
    /**
     * Match is successfully finished.
     */
    FINISHED,
    /**
     * Match is unsuccessfully finished (without results).
     */
    FAILED,
    /**
     * Match is cancelled.
     */
    CANCELLED,
    /**
     * Match is expired.
     */
    EXPIRED;

    public static List<MatchState> getEndedMatchStates() {
        return List.of(FINISHED, FAILED, CANCELLED, EXPIRED);
    }

    public static List<MatchState> getSubmitStates() {
        return List.of(ON_SUBMIT);
    }
}
