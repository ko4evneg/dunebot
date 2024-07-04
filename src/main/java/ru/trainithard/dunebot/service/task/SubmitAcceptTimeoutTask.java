package ru.trainithard.dunebot.service.task;

import lombok.extern.slf4j.Slf4j;
import ru.trainithard.dunebot.service.LogId;
import ru.trainithard.dunebot.service.MatchFinishingService;

@Slf4j
public class SubmitAcceptTimeoutTask implements DunebotRunnable {
    private final MatchFinishingService matchFinishingService;
    private final long matchId;

    public SubmitAcceptTimeoutTask(MatchFinishingService matchFinishingService, long matchId) {
        this.matchFinishingService = matchFinishingService;
        this.matchId = matchId;
    }

    @Override
    public void run() {
        int logId = LogId.get();
        log.debug("{}: ACCEPT_TIMEOUT match {} finishing started", logId, matchId);
        matchFinishingService.finishSubmittedMatch(matchId);
        log.debug("{}: ACCEPT_TIMEOUT match {} finishing ended", logId, matchId);
    }
}
