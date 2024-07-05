package ru.trainithard.dunebot.service;

public interface MatchFinishingService {
    /**
     * Finishes match containing all places and leaders submitted.
     *
     * @param matchId ID of the match
     */
    void finishCompletelySubmittedMatch(long matchId);

    void finishPartialSubmitMatch(long matchId, boolean isFailedByResubmitsLimit);
}
