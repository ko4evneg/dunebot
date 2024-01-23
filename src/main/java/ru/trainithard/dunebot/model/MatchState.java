package ru.trainithard.dunebot.model;

/**
 * Status of <code>Match</code>.
 */
public enum MatchState {
    /**
     * Match is just created.
     */
    NEW,
    /**
     * Submit command was executed for the match.
     */
    ON_SUBMIT,
    /**
     * Match is successfully finished.
     */
    FINISHED,
    /**
     * Match is unsuccessfully finished (without results).
     */
    FAILED;
}
