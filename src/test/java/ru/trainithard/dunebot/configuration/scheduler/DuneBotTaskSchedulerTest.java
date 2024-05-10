package ru.trainithard.dunebot.configuration.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class DuneBotTaskSchedulerTest {
    private static final Instant NOW = LocalDate.of(2010, 10, 20).atStartOfDay().toInstant(ZoneOffset.UTC);
    private final Clock clock = mock(Clock.class);
    private final DuneBotTaskScheduler duneBotTaskScheduler = new DuneBotTaskScheduler(clock);

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
        duneBotTaskScheduler.initialize();
    }

    @ParameterizedTest
    @MethodSource("taskIdSource")
    void shouldCancelScheduledTask(DuneTaskId duneTaskId) {

        duneBotTaskScheduler.reschedule(System.out::println, duneTaskId, Instant.now(clock));
        duneBotTaskScheduler.cancel(duneTaskId);

        ScheduledFuture<?> actualScheduledFuture = duneBotTaskScheduler.get(duneTaskId);

        assertThat((Future<?>) actualScheduledFuture).isNull();
    }

    @ParameterizedTest
    @MethodSource("taskIdSource")
    void shouldCancelScheduledTaskOnReschedule(DuneTaskId duneTaskId) {
        duneBotTaskScheduler.reschedule(System.out::println, duneTaskId, Instant.now(clock));

        ScheduledFuture<?> actualScheduledFuture1 = duneBotTaskScheduler.get(duneTaskId);

        duneBotTaskScheduler.reschedule(System.out::println, duneTaskId, Instant.now(clock));

        ScheduledFuture<?> actualScheduledFuture2 = duneBotTaskScheduler.get(duneTaskId);

        assertThat((Future<?>) actualScheduledFuture1).isNotNull();
        assertThat((Future<?>) actualScheduledFuture2).isNotNull().isNotSameAs(actualScheduledFuture1);
    }

    private static Stream<Arguments> taskIdSource() {
        return Stream.of(
                Arguments.of(new DuneTaskId(DuneTaskType.SHUTDOWN)),
                Arguments.of(new DuneTaskId(DuneTaskType.START_MESSAGE, 13L)),
                Arguments.of(new DuneTaskId(DuneTaskType.SUBMIT_TIMEOUT, 13L))
        );
    }

}
