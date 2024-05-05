package ru.trainithard.dunebot.service;

public interface MatchFinishingService {
    void finishNotSubmittedMatch(long matchId, boolean isFailedByResubmitsLimit);

    void finishSubmittedMatch(long matchId);
}
