package ru.trainithard.dunebot.service.telegram.command.task;

import org.junit.jupiter.api.Test;
import ru.trainithard.dunebot.service.MatchFinishingService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SubmitTimeoutTaskTest {
    private final MatchFinishingService matchFinishingService = mock(MatchFinishingService.class);
    private final SubmitTimeoutTask submitTimeoutTask = new SubmitTimeoutTask(matchFinishingService, 10000L);

    @Test
    void shouldCallNotSubmittedMatchFinishWhenRun() {
        submitTimeoutTask.run();

        verify(matchFinishingService).finishNotSubmittedMatch(10000L, false);
    }
}
