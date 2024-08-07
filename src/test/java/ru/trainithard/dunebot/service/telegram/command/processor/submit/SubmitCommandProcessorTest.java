package ru.trainithard.dunebot.service.telegram.command.processor.submit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.validator.SubmitMatchValidator;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class SubmitCommandProcessorTest extends TestContextMock {
    private static final long CHAT_ID = 12000L;
    private static final Long USER_ID_1 = 11000L;
    private static final long USER_ID_2 = 11001L;
    private static final int FINISH_MATCH_TIMEOUT = 120;
    private static final Instant NOW = LocalDate.of(2010, 10, 10).atTime(15, 0, 0)
            .toInstant(ZoneOffset.UTC);
    private final CommandMessage submitCommandMessage = getCommandMessage(USER_ID_1);

    @Autowired
    private SubmitCommandProcessor processor;
    @MockBean
    private Clock clock;
    @MockBean
    private DuneBotTaskScheduler taskScheduler;
    @MockBean
    private SubmitMatchValidator validator;
    @SpyBean
    private MatchRepository matchRepository;

    @BeforeEach
    void beforeEach() {
        doAnswer(new MockReplier()).when(messagingService).sendMessageAsync(any(MessageDto.class));
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture).when(taskScheduler).rescheduleSingleRunTask(any(), any(), any(Instant.class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + USER_ID_1 + ", " + USER_ID_1 + ", 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, " + USER_ID_2 + ", 10001, 'st_pl2', 'name2', 'l2', 'e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10002, 11002, 10002, 'st_pl3', 'name3', 'l3', 'e3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10003, 11003, 10003, 'st_pl4', 'name4', 'l4', 'e4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, created_at) " +
                             "values (10000, 'ExternalPollId', 10000, " + CHAT_ID + ", '10000', '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 10000, " + CHAT_ID + ", '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, state, positive_answers_count, submits_retry_count, created_at) " +
                             "values (15000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', '" + MatchState.NEW + "', 4, 0, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10000, 15000, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10001, 15000, 10001, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10002, 15000, 10002, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 15000, 10003, '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, short_name, mod_type, created_at) values " +
                             "(10200, 'la leader 1', 'la leader 1', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, short_name, mod_type, created_at) values " +
                             "(10201, 'la leader 2', 'la leader 2', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, short_name, mod_type, created_at) values " +
                             "(10202, 'la leader 3', 'la leader 3', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, short_name, mod_type, created_at) values " +
                             "(10203, 'la leader 4', 'la leader 4', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) " +
                             "values (10000, '" + AppSettingKey.SUBMIT_TIMEOUT + "', 120, '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) " +
                             "values (10001, '" + AppSettingKey.SUBMIT_TIMEOUT_WARNING_NOTIFICATION + "', 9, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from app_settings where id between 10000 and 10001");
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from leaders where id between 10200 and 10203");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where chat_id between 12000 and 12006");
    }

    @Test
    void shouldInvokeMatchValidation() {
        processor.process(submitCommandMessage);

        verify(validator).validateSubmitMatch(eq(submitCommandMessage), argThat(match -> 15000L == match.getId()));
    }

    @Test
    void shouldInvokeMatchValidationBeforeMatchSave() {
        processor.process(submitCommandMessage);

        InOrder inOrder = inOrder(validator, matchRepository);
        inOrder.verify(validator).validateSubmitMatch(any(), argThat(match -> 15000L == match.getId()));
        inOrder.verify(matchRepository).save(argThat(match -> 15000L == match.getId()));
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
    void shouldSendMessageToSubmitInitiator() {
        processor.process(submitCommandMessage);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();

        assertThat(actualMessageDto.getChatId()).isEqualTo(USER_ID_1.toString());
    }

    @Test
    void shouldSetMatchSubmitterOnSubmit() {
        processor.process(submitCommandMessage);

        Long actualSubmittedId = jdbcTemplate.queryForObject("select submitter_id from matches where id = 15000", Long.class);

        assertThat(actualSubmittedId).isEqualTo(10000);
    }

    @Test
    void shouldSendCorrectSubmitMessage() {
        processor.process(submitCommandMessage);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();
        List<List<ButtonDto>> linedButtons = actualMessageDto.getKeyboard();

        assertThat(actualMessageDto.getText()).isEqualTo("""
                Регистрация результатов для *матча 15000*\\. \
                Нажмите по очереди кнопки с именами участвовавших игроков, \
                начиная от победителя и заканчивая последним местом\\.""");
        assertThat(linedButtons)
                .flatExtracting(buttonDtos -> buttonDtos)
                .extracting(ButtonDto::getText, ButtonDto::getCallback)
                .containsExactlyInAnyOrder(
                        tuple("name1 (st_pl1) l1", "15000_SP_10000"),
                        tuple("name2 (st_pl2) l2", "15000_SP_10001"),
                        tuple("name3 (st_pl3) l3", "15000_SP_10002"),
                        tuple("name4 (st_pl4) l4", "15000_SP_10003")
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
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();
        List<List<ButtonDto>> linedButtons = actualMessageDto.getKeyboard();

        assertThat(actualMessageDto.getText()).isEqualTo("""
                Регистрация результатов для *матча 15000*\\. \
                Нажмите по очереди кнопки с именами участвовавших игроков, \
                начиная от победителя и заканчивая последним местом\\.""");
        assertThat(linedButtons)
                .flatExtracting(buttonDtos -> buttonDtos)
                .extracting(ButtonDto::getText, ButtonDto::getCallback)
                .containsExactlyInAnyOrder(
                        tuple("name1 (st_pl1) l1", "15000_SP_10000"),
                        tuple("name2 (st_pl2) l2", "15000_SP_10001"),
                        tuple("name3 (st_pl3) l3", "15000_SP_10002"),
                        tuple("name4 (st_pl4) l4", "15000_SP_10003"),
                        tuple("name5 (st_pl5) l5", "15000_SP_10004")
                );
    }

    @Test
    void shouldSetSubmitMatchState() {
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
    void shouldScheduleUnsuccessfullySubmittedMatchFinishTaskOnFirstSubmit() {
        processor.process(submitCommandMessage);

        DuneBotTaskId expectedTaskId = new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT, 15000L);
        Instant expectedInstant = NOW.plus(FINISH_MATCH_TIMEOUT, ChronoUnit.MINUTES);
        verify(taskScheduler).rescheduleSingleRunTask(any(), eq(expectedTaskId), eq(expectedInstant));
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
        chat.setId(userId);
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
