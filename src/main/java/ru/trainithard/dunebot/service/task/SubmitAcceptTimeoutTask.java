package ru.trainithard.dunebot.service.task;

import lombok.extern.slf4j.Slf4j;
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
        matchFinishingService.finishCompletelySubmittedMatch(matchId);
    }
}
