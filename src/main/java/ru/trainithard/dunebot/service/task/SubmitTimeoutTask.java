package ru.trainithard.dunebot.service.task;

import lombok.extern.slf4j.Slf4j;
import ru.trainithard.dunebot.service.MatchFinishingService;

@Slf4j
public class SubmitTimeoutTask implements DunebotRunnable {
    private final MatchFinishingService matchFinishingService;
    private final long matchId;

    public SubmitTimeoutTask(MatchFinishingService matchFinishingService, long matchId) {
        this.matchFinishingService = matchFinishingService;
        this.matchId = matchId;
    }

    @Override
    public void run() {
        log.debug("TIMEOUT match {} finishing started", matchId);
        matchFinishingService.finishNotSubmittedMatch(matchId, false);
    }
}
