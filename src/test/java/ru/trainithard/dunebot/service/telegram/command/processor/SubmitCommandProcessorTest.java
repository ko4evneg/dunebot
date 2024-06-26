package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class SubmitCommandProcessorTest extends TestContextMock {
    private static final long CHAT_ID = 12000L;
    private static final long USER_ID = 11000L;
    private static final int FINISH_MATCH_TIMEOUT = 120;
    private static final Instant NOW = LocalDate.of(2010, 10, 10).atTime(15, 0, 0)
            .toInstant(ZoneOffset.UTC);
    private final CommandMessage submitCommandMessage = getCommandMessage(USER_ID);

    @Autowired
    private SubmitCommandProcessor processor;
    @MockBean
    private Clock clock;
    @MockBean
    private DuneBotTaskScheduler taskScheduler;
    @MockBean
    private MatchFinishingService finishingService;

    @BeforeEach
    void beforeEach() {
        doAnswer(new MockReplier()).when(messagingService).sendMessageAsync(any(MessageDto.class));
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture).when(taskScheduler).rescheduleSingleRunTask(any(), any(), any(Instant.class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + USER_ID + ", " + CHAT_ID + ", 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, 11001, 12001, 'st_pl2', 'name2', 'l2', 'e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10002, 11002, 12002, 'st_pl3', 'name3', 'l3', 'e3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10003, 11003, 12003, 'st_pl4', 'name4', 'l4', 'e4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, created_at) " +
                             "values (10000, 'ExternalPollId', 10000, " + CHAT_ID + ", '10000', '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 10000, " + CHAT_ID + ", '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, state, positive_answers_count, created_at) " +
                             "values (15000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', '" + MatchState.NEW + "', 4, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10000, 15000, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10001, 15000, 10001, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10002, 15000, 10002, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 15000, 10003, '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) " +
                             "values (10000, '" + AppSettingKey.FINISH_MATCH_TIMEOUT + "', 120, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from app_settings where id = 10000");
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where chat_id between 12000 and 12006");
    }

    @ParameterizedTest
    @MethodSource("exceptionsSource")
    void shouldThrowOnUnsuitableMatchSubmit(String query, String expectedException) {
        jdbcTemplate.execute(query);

        assertThatThrownBy(() -> processor.process(submitCommandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(expectedException);
    }

    private static Stream<Arguments> exceptionsSource() {
        return Stream.of(
                Arguments.of("update matches set state = '" + MatchState.FAILED + "' where id = 15000", "Запрещено регистрировать результаты завершенных матчей"),
                Arguments.of("update matches set state = '" + MatchState.FINISHED + "' where id = 15000", "Запрещено регистрировать результаты завершенных матчей"),
                Arguments.of("update matches set state = '" + MatchState.ON_SUBMIT + "' where id = 15000", "Запрос на публикацию этого матча уже сделан"),
                Arguments.of("update matches set positive_answers_count = 3 where id = 15000", "В опросе участвует меньше игроков чем нужно для матча. Все игроки должны войти в опрос")
        );
    }

    @Test
    void shouldThrowOnAlienMatchSubmit() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10004, 11004, 12004, 'st_pl5', 'name5', 'l5', 'e5', '2010-10-10') ");
        CommandMessage commandMessage = getCommandMessage(11004L);

        assertThatThrownBy(() -> processor.process(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Вы не можете инициировать публикацию этого матча");
    }

    @Test
    void shouldThrowOnNotExistentMatchSubmit() {
        jdbcTemplate.execute("delete from match_players where match_id = 15000");
        jdbcTemplate.execute("delete from matches where id = 15000");

        assertThatThrownBy(() -> processor.process(submitCommandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Матча с таким ID не существует!");
    }

    @Test
    void shouldSendMessagesToEveryMatchPlayer() {
        processor.process(submitCommandMessage);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(4)).sendMessageAsync(messageDtoCaptor.capture());
        List<MessageDto> actualSendMessages = messageDtoCaptor.getAllValues();

        assertThat(actualSendMessages)
                .flatExtracting(MessageDto::getChatId)
                .containsExactlyInAnyOrder("12000", "12001", "12002", "12003");
    }

    @Test
    void shouldSendCorrectSubmitMessage() {
        processor.process(submitCommandMessage);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(4)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getAllValues().get(0);
        List<List<ButtonDto>> linedButtons = actualMessageDto.getKeyboard();

        assertThat(actualMessageDto.getText()).isEqualTo("Выберите место, которое вы заняли в матче 15000:");
        assertThat(linedButtons)
                .flatExtracting(buttonDtos -> buttonDtos)
                .extracting(ButtonDto::getText, ButtonDto::getCallback)
                .containsExactly(
                        tuple("1", "15000__1"),
                        tuple("2", "15000__2"),
                        tuple("3", "15000__3"),
                        tuple("4", "15000__4"),
                        tuple("не участвовал(а)", "15000__0")
                );
    }

    @Test
    void shouldSendCorrectSubmitMessageWhenMatchHasMorePlayersThanAllowed() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10004, 11004, 12004, 'st_pl5', 'name5', 'l5', 'e5', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10004, 15000, 10004, '2010-10-10')");

        processor.process(submitCommandMessage);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(5)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getAllValues().get(0);
        List<List<ButtonDto>> linedButtons = actualMessageDto.getKeyboard();

        assertThat(actualMessageDto.getText()).isEqualTo("Выберите место, которое вы заняли в матче 15000:");
        assertThat(linedButtons)
                .flatExtracting(buttonDtos -> buttonDtos)
                .extracting(ButtonDto::getText, ButtonDto::getCallback)
                .containsExactly(
                        tuple("1", "15000__1"),
                        tuple("2", "15000__2"),
                        tuple("3", "15000__3"),
                        tuple("4", "15000__4"),
                        tuple("не участвовал(а)", "15000__0")
                );
    }

    @Test
    void shouldSaveSubmitMessageIdsToMatchPlayers() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(111001);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        Message message = new Message();
        message.setMessageId(111000);
        message.setReplyToMessage(replyMessage);
        message.setChat(chat);

        doReturn(CompletableFuture.completedFuture(new ExternalMessageDto(message))).when(messagingService).sendMessageAsync(any(MessageDto.class));

        processor.process(submitCommandMessage);

        Long assignedIdsPlayerCount = jdbcTemplate.queryForObject("select count(*) from match_players where external_submit_id in " +
                                                                  "(select id from external_messages where chat_id = " + CHAT_ID + " and message_id = 111000 and reply_id = 111001)", Long.class);

        assertThat(assignedIdsPlayerCount).isEqualTo(4);
    }

    @Test
    void shouldSetMatchOnSubmitState() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(111001);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        Message message = new Message();
        message.setMessageId(111000);
        message.setReplyToMessage(replyMessage);
        message.setChat(chat);

        doReturn(CompletableFuture.completedFuture(new ExternalMessageDto(message))).when(messagingService).sendMessageAsync(any(MessageDto.class));

        processor.process(submitCommandMessage);

        MatchState actualState = jdbcTemplate.queryForObject("select state from matches where id = 15000", MatchState.class);

        assertThat(actualState).isNotNull().isEqualTo(MatchState.ON_SUBMIT);
    }

    @Test
    void shouldNotSaveSubmitMessageReplyIdToMatchPlayerFromPrivateChatSubmit() {
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        Message message = new Message();
        message.setMessageId(111000);
        message.setChat(chat);

        doReturn(CompletableFuture.completedFuture(new ExternalMessageDto(message))).when(messagingService).sendMessageAsync(any(MessageDto.class));

        processor.process(submitCommandMessage);

        Long assignedIdsPlayerCount = jdbcTemplate.queryForObject("select count(*) from match_players where external_submit_id in " +
                                                                  "(select id from external_messages where chat_id = " + CHAT_ID + " and message_id = 111000 and reply_id is null)", Long.class);

        assertThat(assignedIdsPlayerCount).isEqualTo(4);
    }

    @Test
    void shouldNotSaveSubmitMessageIdsForOtherMatchPlayer() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10004, 11004, 12004, 'st_pl5', 'name5', 'l5', 'e5', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10004, 15000, 10004, '2010-10-10')");

        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        Message message = new Message();
        message.setMessageId(111000);
        message.setChat(chat);

        doReturn(CompletableFuture.completedFuture(new ExternalMessageDto(message))).when(messagingService).sendMessageAsync(any(MessageDto.class));

        processor.process(submitCommandMessage);

        List<Long> assignedIdsPlayers = jdbcTemplate.queryForList("select id from match_players where external_submit_id in " +
                                                                  "(select id from external_messages where chat_id = " + CHAT_ID + " and message_id = 111000 and reply_id is null)", Long.class);

        assertThat(assignedIdsPlayers).isNotEmpty().doesNotContain(11004L);
    }

    @Test
    void shouldScheduleUnsuccessfullySubmittedMatchFinishTaskOnFirstSubmit() {
        jdbcTemplate.execute("update matches set submits_count = 0 where id = 15000");

        processor.process(getCommandMessage(USER_ID));

        DuneBotTaskId expectedTaskId = new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT, 15000L);
        Instant expectedInstant = NOW.plus(FINISH_MATCH_TIMEOUT, ChronoUnit.MINUTES);
        verify(taskScheduler).rescheduleSingleRunTask(any(), eq(expectedTaskId), eq(expectedInstant));
    }

    @ParameterizedTest
    @CsvSource({"ON_SUBMIT, 1", "ON_SUBMIT, 2", "ON_SUBMIT, 3"})
    void shouldNotRescheduleUnsuccessfullySubmittedMatchFinishTaskOnNotFirstSubmit(MatchState state, int submitsCount) {
        jdbcTemplate.execute("update matches set state = '" + state + "', submits_count = " + submitsCount + " where id = 15000");

        try {
            processor.process(getCommandMessage(USER_ID));
        } catch (Exception ignored) {
        }

        verifyNoInteractions(taskScheduler);
    }

    @Test
    void shouldRescheduleUnsuccessfullySubmittedMatchFinishTaskOnResubmit() {
        DuneBotTaskId taskId = new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT, 15000L);
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).get(taskId);
        doReturn(208L).when(future).getDelay(any());

        processor.process(submitCommandMessage);

        Instant expectedInstant = NOW.plus(208 + 420, ChronoUnit.SECONDS);
        verify(taskScheduler).rescheduleSingleRunTask(any(), eq(taskId), eq(expectedInstant));
    }

    @Test
    void shouldReturnSubmitCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.SUBMIT);
    }

    private CommandMessage getCommandMessage(long userId) {
        User user = new User();
        user.setId(userId);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(10000);
        message.setFrom(user);
        message.setChat(chat);
        message.setText("/" + Command.SUBMIT.name() + " 15000");
        return CommandMessage.getMessageInstance(message);
    }

    private static class MockReplier implements Answer<CompletableFuture<ExternalMessageDto>> {
        private int externalId = 11000;
        private long chatId = CHAT_ID;

        @Override
        public CompletableFuture<ExternalMessageDto> answer(InvocationOnMock invocationOnMock) {
            Chat chat = new Chat();
            chat.setId(chatId++);
            Message message = new Message();
            message.setMessageId(externalId++);
            message.setChat(chat);
            return CompletableFuture.completedFuture(new ExternalMessageDto(message));
        }
    }
}
