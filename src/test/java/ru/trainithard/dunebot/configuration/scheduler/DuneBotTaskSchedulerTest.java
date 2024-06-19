package ru.trainithard.dunebot.configuration.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.scheduler.DuneBotTask;
import ru.trainithard.dunebot.model.scheduler.TaskStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@SpringBootTest
class DuneBotTaskSchedulerTest extends TestContextMock {
    private static final Instant NOW = LocalDate.of(2010, 10, 20).atStartOfDay().toInstant(ZoneOffset.UTC);
    private static final Instant SOON = LocalDate.of(2010, 10, 21).atStartOfDay().toInstant(ZoneOffset.UTC);
    private static final Runnable TASK = () -> {
    };
    private final Clock clock = mock(Clock.class);
    @Autowired
    private DuneBotTaskScheduler duneBotTaskScheduler;

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from dunebot_tasks where entity_id between 10000 and 10002");
    }

    @Test
    void shouldSaveNewTaskOnRescheduleWhenNewTaskProvided() {
        duneBotTaskScheduler.rescheduleSingleRunTask(TASK, new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L), SOON);

        DuneBotTask actualTask = jdbcTemplate
                .queryForObject("select * from dunebot_tasks where task_type = '" + DuneTaskType.START_MESSAGE + "'" +
                                "and entity_id = 10000", new BeanPropertyRowMapper<>(DuneBotTask.class));

        assertThat(actualTask)
                .extracting(DuneBotTask::getStartTime, DuneBotTask::getStatus)
                .containsExactly(SOON, TaskStatus.SCHEDULED);
    }

    @Test
    void shouldUpdateStartTimeForExistingTaskOnReschedule() {
        jdbcTemplate.execute("insert into dunebot_tasks (task_type, entity_id, start_time, status, created_at) values " +
                             "('" + DuneTaskType.START_MESSAGE + "', 10000, '" + NOW + "', '" + TaskStatus.SCHEDULED + "', '2010-10-10')");

        duneBotTaskScheduler.rescheduleSingleRunTask(TASK, new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L), SOON);

        DuneBotTask actualTask = jdbcTemplate
                .queryForObject("select * from dunebot_tasks where task_type = '" + DuneTaskType.START_MESSAGE + "'" +
                                "and entity_id = 10000", new BeanPropertyRowMapper<>(DuneBotTask.class));

        assertThat(actualTask)
                .extracting(DuneBotTask::getStartTime)
                .isEqualTo(SOON);
    }

    @Test
    void shouldUpdateStartTimeForExistingTaskOnRescheduleWhenMultipleTaskPresented() {
        jdbcTemplate.execute("insert into dunebot_tasks (task_type, entity_id, start_time, status, created_at) values " +
                             "('" + DuneTaskType.START_MESSAGE + "', 10000, '" + NOW + "', '" + TaskStatus.SCHEDULED + "', '2010-10-10')");
        jdbcTemplate.execute("insert into dunebot_tasks (task_type, entity_id, start_time, status, created_at) values " +
                             "('" + DuneTaskType.START_MESSAGE + "', 10001, '" + NOW + "', '" + TaskStatus.FINISHED + "', '2010-10-10')");
        jdbcTemplate.execute("insert into dunebot_tasks (task_type, entity_id, start_time, status, created_at) values " +
                             "('" + DuneTaskType.START_MESSAGE + "', 10002, '" + NOW + "', '" + TaskStatus.CANCELLED + "', '2010-10-10')");

        duneBotTaskScheduler.rescheduleSingleRunTask(TASK, new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L), SOON);

        List<Instant> actualTasks = jdbcTemplate
                .queryForList("select start_time from dunebot_tasks where entity_id between 10000 and 10002 order by entity_id", Instant.class);

        assertThat(actualTasks)
                .containsExactly(SOON, NOW, NOW);
    }

    @Test
    void shouldSetCancelledStateToTaskOnCancelTask() {
        jdbcTemplate.execute("insert into dunebot_tasks (task_type, entity_id, start_time, status, created_at) values " +
                             "('" + DuneTaskType.START_MESSAGE + "', 10000, '" + NOW + "', '" + TaskStatus.SCHEDULED + "', '2010-10-10')");

        duneBotTaskScheduler.cancelSingleRunTask(new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L));

        DuneBotTask actualTask = jdbcTemplate
                .queryForObject("select * from dunebot_tasks where task_type = '" + DuneTaskType.START_MESSAGE + "'" +
                                "and entity_id = 10000", new BeanPropertyRowMapper<>(DuneBotTask.class));

        assertThat(actualTask)
                .extracting(DuneBotTask::getStatus)
                .isEqualTo(TaskStatus.CANCELLED);
    }

    @Test
    void shouldSetCancelledToTheOnlyTaskWhenMultipleTasksPresented() {
        jdbcTemplate.execute("insert into dunebot_tasks (task_type, entity_id, start_time, status, created_at) values " +
                             "('" + DuneTaskType.START_MESSAGE + "', 10000, '" + NOW + "', '" + TaskStatus.SCHEDULED + "', '2010-10-10')");
        jdbcTemplate.execute("insert into dunebot_tasks (task_type, entity_id, start_time, status, created_at) values " +
                             "('" + DuneTaskType.START_MESSAGE + "', 10001, '" + NOW + "', '" + TaskStatus.FINISHED + "', '2010-10-10')");
        jdbcTemplate.execute("insert into dunebot_tasks (task_type, entity_id, start_time, status, created_at) values " +
                             "('" + DuneTaskType.START_MESSAGE + "', 10002, '" + NOW + "', '" + TaskStatus.CANCELLED + "', '2010-10-10')");

        duneBotTaskScheduler.cancelSingleRunTask(new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L));

        List<String> actualTasks = jdbcTemplate
                .queryForList("select status from dunebot_tasks where entity_id between 10001 and 10002 order by entity_id", String.class);

        assertThat(actualTasks)
                .containsExactly("FINISHED", "CANCELLED");
    }

    @ParameterizedTest
    @MethodSource("taskIdSource")
    void shouldCancelScheduledTask(DuneBotTaskId duneBotTaskId) {

        duneBotTaskScheduler.rescheduleSingleRunTask(System.out::println, duneBotTaskId, Instant.now(clock));
        duneBotTaskScheduler.cancelSingleRunTask(duneBotTaskId);

        ScheduledFuture<?> actualScheduledFuture = duneBotTaskScheduler.get(duneBotTaskId);

        assertThat((Future<?>) actualScheduledFuture).isNull();
    }

    @ParameterizedTest
    @MethodSource("taskIdSource")
    void shouldCancelScheduledTaskOnReschedule(DuneBotTaskId duneBotTaskId) {
        duneBotTaskScheduler.rescheduleSingleRunTask(System.out::println, duneBotTaskId, Instant.now(clock));

        ScheduledFuture<?> actualScheduledFuture1 = duneBotTaskScheduler.get(duneBotTaskId);

        duneBotTaskScheduler.rescheduleSingleRunTask(System.out::println, duneBotTaskId, Instant.now(clock));

        ScheduledFuture<?> actualScheduledFuture2 = duneBotTaskScheduler.get(duneBotTaskId);

        assertThat((Future<?>) actualScheduledFuture1).isNotNull();
        assertThat((Future<?>) actualScheduledFuture2).isNotNull().isNotSameAs(actualScheduledFuture1);
    }

    private static Stream<Arguments> taskIdSource() {
        return Stream.of(
                Arguments.of(new DuneBotTaskId(DuneTaskType.SHUTDOWN)),
                Arguments.of(new DuneBotTaskId(DuneTaskType.START_MESSAGE, 13L)),
                Arguments.of(new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT, 13L))
        );
    }

}
