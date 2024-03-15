package ru.trainithard.dunebot.service;

import ru.trainithard.dunebot.service.messaging.ExternalMessage;

public interface MatchFinishingService {
    void finishNotSubmittedMatch(long matchId, ExternalMessage reasonMessage, int loggingId);

    void finishSubmittedMatch(long matchId, int loggingId);
}
