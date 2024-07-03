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
     * Submit in process
     */
    ON_SUBMIT,
    /**
     * Submit was finished
     */
    SUBMITTED,
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
    EXPIRED,
    /**
     * Match is not submitted.
     */
    NOT_SUBMITTED;

    public static List<MatchState> getEndedMatchStates() {
        return List.of(FINISHED, FAILED, CANCELLED, EXPIRED, NOT_SUBMITTED);
    }

    public static List<MatchState> getSubmitStates() {
        return List.of(ON_SUBMIT, SUBMITTED);
    }
}
