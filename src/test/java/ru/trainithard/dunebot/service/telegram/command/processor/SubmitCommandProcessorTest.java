package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.TaskScheduler;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class SubmitCommandProcessorTest extends TestContextMock {
    private static final long CHAT_ID = 12000L;
    private static final long USER_ID = 11000L;
    private static final int FINISH_MATCH_TIMEOUT = 120;
    private static final String TIMEOUT_MATCH_FINISH_MESSAGE_TEXT =
            "*Матч 15000* завершен без результата, так как превышено максимальное количество попыток регистрации мест";
    private static final Instant NOW = LocalDate.of(2010, 10, 10).atTime(15, 0, 0)
            .toInstant(ZoneOffset.UTC);
    private final CommandMessage submitCommandMessage = getCommandMessage(USER_ID);

    @Autowired
    private SubmitCommandProcessor processor;
    @MockBean
    private Clock clock;
    @MockBean
    private TaskScheduler taskScheduler;
    @MockBean
    private MatchFinishingService finishingService;

    @BeforeEach
    void beforeEach() {
        doAnswer(new MockReplier()).when(messagingService).sendMessageAsync(any(MessageDto.class));
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(), any(Instant.class));

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
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) " +
                             "values (10000, '" + SettingKey.FINISH_MATCH_TIMEOUT + "', 120, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from settings where id = 10000");
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where chat_id between 12000 and 12006");
    }

    @ParameterizedTest
    @MethodSource("exceptionsSource")
    void shouldThrowOnUnsuitableMatchSubmit(String query, String expectedException) {
        jdbcTemplate.execute(query);

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> processor.process(submitCommandMessage));

        assertEquals(expectedException, actualException.getMessage());
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

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> processor.process(commandMessage));

        assertEquals("Вы не можете инициировать публикацию этого матча", actualException.getMessage());
    }

    @Test
    void shouldThrowOnNotExistentMatchSubmit() {
        jdbcTemplate.execute("delete from match_players where match_id = 15000");
        jdbcTemplate.execute("delete from matches where id = 15000");

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> processor.process(submitCommandMessage));

        assertEquals("Матча с таким ID не существует!", actualException.getMessage());
    }

    @Test
    void shouldSendMessagesToEveryMatchPlayer() {
        processor.process(submitCommandMessage);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(4)).sendMessageAsync(messageDtoCaptor.capture());
        List<MessageDto> actualSendMessages = messageDtoCaptor.getAllValues();

        assertThat(actualSendMessages, containsInAnyOrder(
                hasProperty("chatId", is("12000")),
                hasProperty("chatId", is("12001")),
                hasProperty("chatId", is("12002")),
                hasProperty("chatId", is("12003")))
        );
    }

    @Test
    void shouldSendCorrectSubmitMessage() {
        processor.process(submitCommandMessage);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(4)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getAllValues().get(0);
        List<List<ButtonDto>> linedButtons = actualMessageDto.getKeyboard();

        assertEquals("Выберите место, которое вы заняли в матче 15000:", actualMessageDto.getText());
        assertThat(linedButtons.get(0), contains(
                both(hasProperty("text", is("1"))).and(hasProperty("callback", is("15000__1"))),
                both(hasProperty("text", is("2"))).and(hasProperty("callback", is("15000__2"))))
        );
        assertThat(linedButtons.get(1), contains(
                both(hasProperty("text", is("3"))).and(hasProperty("callback", is("15000__3"))),
                both(hasProperty("text", is("4"))).and(hasProperty("callback", is("15000__4"))))
        );
        assertThat(linedButtons.get(2), contains(
                both(hasProperty("text", is("не участвовал(а)"))).and(hasProperty("callback", is("15000__0"))))
        );
    }

    @Test
    void shouldSendDeleteSubmitMessageWhenOldSubmitMessageExist() {
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10002, 'ExternalMessageId', 12345, " + CHAT_ID + ", '2020-10-10')");
        jdbcTemplate.execute("update match_players set external_submit_id = 10002 where id = 10000");

        processor.process(submitCommandMessage);

        ArgumentCaptor<ExternalMessageId> messageDtoCaptor = ArgumentCaptor.forClass(ExternalMessageId.class);
        verify(messagingService, times(1)).deleteMessageAsync(messageDtoCaptor.capture());
        ExternalMessageId actualDeleteDto = messageDtoCaptor.getValue();

        assertEquals(CHAT_ID, actualDeleteDto.getChatId());
        assertEquals(12345, actualDeleteDto.getMessageId());
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

        assertEquals(4, assignedIdsPlayerCount);
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

        assertNotNull(actualState);
        assertEquals(MatchState.ON_SUBMIT, actualState);
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

        assertEquals(4, assignedIdsPlayerCount);
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

        assertFalse(assignedIdsPlayers.contains(11004L));
    }

    @Test
    void shouldScheduleUnsuccessfullySubmittedMatchFinishTaskOnSubmit() {
        jdbcTemplate.execute("update matches set positive_answers_count = 0 where id = 10000");

        processor.process(getCommandMessage(USER_ID));

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler, times(1)).schedule(any(), instantCaptor.capture());
        Instant actualInstant = instantCaptor.getValue();

        assertEquals(NOW.plus(FINISH_MATCH_TIMEOUT, ChronoUnit.MINUTES), actualInstant);
    }

    @Test
    void shouldInvokeUnsuccessfullySubmittedMatchFinishOnScheduledTask() throws InterruptedException {
        jdbcTemplate.execute("update matches set positive_answers_count = 0 where id = 10000");

        processor.process(getCommandMessage(USER_ID));

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler, times(1)).schedule(runnableCaptor.capture(), any(Instant.class));
        Runnable actualRunnable = runnableCaptor.getValue();

        Thread thread = new Thread(actualRunnable);
        thread.start();
        thread.join();

        verify(finishingService, times(1)).finishNotSubmittedMatch(
                eq(15000L), argThat(message -> TIMEOUT_MATCH_FINISH_MESSAGE_TEXT.equals(message.getText())), anyInt());
    }

    @Test
    void shouldReturnSubmitCommand() {
        Command actualCommand = processor.getCommand();

        assertEquals(Command.SUBMIT, actualCommand);
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
