package ru.trainithard.dunebot.service;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.model.scheduler.TaskStatus;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.task.DuneScheduledTaskFactory;
import ru.trainithard.dunebot.service.task.DunebotRunnable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class StartupServiceImplTest extends TestContextMock {
    private static final LocalDateTime SCHEDULED_TIME = LocalDateTime.of(2020, 10, 10, 8, 0);
    private final DunebotRunnable runnableMock = mock(DunebotRunnable.class);

    @Autowired
    private StartupService startupService;
    @MockBean
    private DuneBotTaskScheduler taskScheduler;
    @MockBean
    private DuneScheduledTaskFactory taskFactory;
    @MockBean
    private Clock clock;

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(SCHEDULED_TIME.minusMinutes(1).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        doReturn(runnableMock).when(taskFactory).createInstance(any());

        jdbcTemplate.execute("insert into dunebot_tasks (id, task_type, entity_id, status, start_time, created_at) " +
                             "values (10000, '" + DuneTaskType.START_MESSAGE + "', 10000, '" + TaskStatus.SCHEDULED + "', '" + SCHEDULED_TIME + "', '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) values (10000, 'CHAT_ID', '100500', '2010-01-02')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) values (10001, 'TOPIC_ID_CLASSIC', '10001', '2010-01-02')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) values (10002, 'TOPIC_ID_UPRISING', '10002', '2010-01-02')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from app_settings where id between 10000 and 10002");
        jdbcTemplate.execute("delete from dunebot_tasks where id = 10000");
    }

    @ParameterizedTest
    @CsvSource({"START_MESSAGE, SCHEDULED", "START_MESSAGE, RUN", "SUBMIT_TIMEOUT, SCHEDULED", "SUBMIT_TIMEOUT, RUN"})
    void shouldRescheduleFutureScheduledTask(DuneTaskType taskType, TaskStatus taskStatus) {
        jdbcTemplate.execute("update dunebot_tasks set status = '" + taskStatus + "', task_type = '" + taskType + "' where id = 10000");

        startupService.startUp();

        Instant expectedInstant = SCHEDULED_TIME.toInstant(ZoneOffset.UTC);
        DuneBotTaskId expectedTaskId = new DuneBotTaskId(taskType, 10000L);
        verify(taskScheduler).rescheduleSingleRunTask(same(runnableMock), eq(expectedTaskId), eq(expectedInstant));
    }

    @ParameterizedTest
    @CsvSource({"START_MESSAGE, SCHEDULED", "START_MESSAGE, RUN", "SUBMIT_TIMEOUT, SCHEDULED", "SUBMIT_TIMEOUT, RUN"})
    void shouldRescheduleExpiredScheduledTaskInOneMinute(DuneTaskType taskType, TaskStatus taskStatus) {
        jdbcTemplate.execute("update dunebot_tasks set status = '" + taskStatus + "', task_type = '" + taskType + "' where id = 10000");
        LocalDateTime RESTART_TIME = SCHEDULED_TIME.plusMinutes(1);
        Clock fixedClock = Clock.fixed(RESTART_TIME.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        startupService.startUp();

        DuneBotTaskId expectedTaskId = new DuneBotTaskId(taskType, 10000L);
        verify(taskScheduler).rescheduleSingleRunTask(same(runnableMock), eq(expectedTaskId), eq(RESTART_TIME.plusMinutes(1).toInstant(ZoneOffset.UTC)));
    }

    @ParameterizedTest
    @CsvSource({"START_MESSAGE, FAILED", "START_MESSAGE, FINISHED", "START_MESSAGE, CANCELLED",
            "SUBMIT_TIMEOUT, FAILED", "SUBMIT_TIMEOUT, FINISHED", "SUBMIT_TIMEOUT, CANCELLED"})
    void shouldNotRescheduleFinishedTask(DuneTaskType taskType, TaskStatus taskStatus) {
        jdbcTemplate.execute("update dunebot_tasks set status = '" + taskStatus + "', task_type = '" + taskType + "' where id = 10000");
        LocalDateTime RESTART_TIME = SCHEDULED_TIME.plusMinutes(1);
        Clock fixedClock = Clock.fixed(RESTART_TIME.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        startupService.startUp();

        verify(taskScheduler, never()).rescheduleSingleRunTask(any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, mode = EnumSource.Mode.INCLUDE, names = {"RUN", "SCHEDULED"})
    void shouldNotRescheduleShutdownTask(TaskStatus taskStatus) {
        jdbcTemplate.execute("update dunebot_tasks set status = '" + taskStatus + "', task_type = '" + DuneTaskType.SHUTDOWN + "' where id = 10000");

        startupService.startUp();

        verify(taskScheduler, never()).rescheduleSingleRunTask(any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, mode = EnumSource.Mode.INCLUDE, names = {"RUN", "SCHEDULED"})
    void shouldSetFinishStatusForShutdownTask(TaskStatus taskStatus) {
        jdbcTemplate.execute("update dunebot_tasks set status = '" + taskStatus + "', task_type = '" + DuneTaskType.SHUTDOWN + "' where id = 10000");

        startupService.startUp();

        TaskStatus actualStatus = jdbcTemplate.queryForObject("select status from dunebot_tasks where id = 10000", TaskStatus.class);
        assertThat(actualStatus).isEqualTo(TaskStatus.FINISHED);
    }

    @Test
    void shouldSendMessageOnBotRestartToAllTopics() {
        startupService.startUp();

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(2)).sendMessageAsync(messageCaptor.capture());
        List<MessageDto> actualMessage = messageCaptor.getAllValues();

        assertThat(actualMessage)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getText)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("100500", 10001, "Бот был перезапущен, возможны задержки до двух минут в обработке команд или отправке сообщений бота\\."),
                        Tuple.tuple("100500", 10002, "Бот был перезапущен, возможны задержки до двух минут в обработке команд или отправке сообщений бота\\.")
                );
    }

    @Test
    void shouldSendTheOnlyMessageOnBotRestartWhenBothTopicsAreTheSame() {
        jdbcTemplate.execute("update app_settings set value = '10001' where id = 10002");

        startupService.startUp();

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageCaptor.capture());
        List<MessageDto> actualMessage = messageCaptor.getAllValues();

        assertThat(actualMessage)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getText)
                .containsExactly(
                        Tuple.tuple("100500", 10001, "Бот был перезапущен, возможны задержки до двух минут в обработке команд или отправке сообщений бота\\.")
                );
    }

    @Test
    void shouldSendTheOnlyMessageOnBotRestartWhenOnlyOneTopicPresented() {
        jdbcTemplate.execute("delete from app_settings where id = 10001");

        startupService.startUp();

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageCaptor.capture());
        List<MessageDto> actualMessage = messageCaptor.getAllValues();

        assertThat(actualMessage)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getText)
                .containsExactly(
                        Tuple.tuple("100500", 10002, "Бот был перезапущен, возможны задержки до двух минут в обработке команд или отправке сообщений бота\\.")
                );
    }

    @Test
    void shouldNotSendMessagesWhenChatSettingMissing() {
        jdbcTemplate.execute("delete from app_settings where id = 10000");

        startupService.startUp();

        verifyNoInteractions(messagingService);
    }
}
