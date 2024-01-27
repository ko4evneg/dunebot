package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static ru.trainithard.dunebot.configuration.SettingConstants.CHAT_ID;

@SpringBootTest
class CancelCommandProcessorTest extends TestContextMock {
    @Autowired
    private CancelCommandProcessor processor;

    private static final Integer REPLY_ID = 9000;
    private static final int MESSAGE_ID = 100500;
    private static final long TELEGRAM_USER_ID = 12345L;
    private final CommandMessage commandMessage = getCommandMessage();

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        doNothing().when(messagingService).deleteMessageAsync(ArgumentMatchers.any(ExternalMessageId.class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                "values (10000, 12345, 9000, 'st_pl', 'name', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                "values (10000, 'ExternalPollId'," + MESSAGE_ID + ", " + CHAT_ID + ", " + REPLY_ID + ", '12346', '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, owner_id, mod_type, state, positive_answers_count, created_at) " +
                "values (10000, 10000, 10000, '" + ModType.CLASSIC + "', '" + MatchState.NEW + "', 1, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10000, 10000, 10000, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id = (select id from matches where id in (10000, 10001))");
        jdbcTemplate.execute("delete from matches where id = 10000");
        jdbcTemplate.execute("delete from players where id in (10000, 10001)");
        jdbcTemplate.execute("delete from external_messages where id = 10000");
    }

    @Test
    void shouldSendCorrectDeleteMessageRequest() {
        processor.process(commandMessage, mockLoggingId);

        ArgumentCaptor<ExternalMessageId> messageIdCaptor = ArgumentCaptor.forClass(ExternalMessageId.class);
        verify(messagingService).deleteMessageAsync(messageIdCaptor.capture());
        ExternalMessageId actualMessageId = messageIdCaptor.getValue();

        assertEquals(MESSAGE_ID, actualMessageId.getMessageId());
        assertEquals(CHAT_ID, actualMessageId.getChatId().toString());
        assertEquals(REPLY_ID, actualMessageId.getReplyId());
    }

    @Test
    void shouldDeleteMatch() {
        processor.process(commandMessage, mockLoggingId);

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(*) from matches where id = 10000", Long.class);

        assertEquals(0, actualMatchesCount);
    }

    @Test
    void shouldDeleteMatchPlayers() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                "values (10001, 12346, 9000, 'st_pl2', 'name2', 'l1', 'e1', '2010-10-10') ");

        processor.process(commandMessage, mockLoggingId);

        Long actualMatchPlayersCount = jdbcTemplate.queryForObject("select count(*) from match_players where player_id = 10000", Long.class);

        assertEquals(0, actualMatchPlayersCount);
    }

    @Test
    void shouldThrowOnFailedCancel() {
        doThrow(new TelegramApiCallException("", new TelegramApiException())).when(messagingService).deleteMessageAsync(ArgumentMatchers.any(ExternalMessageId.class));

        assertThrows(TelegramApiCallException.class, () -> processor.process(commandMessage, mockLoggingId));
    }

    @Test
    void shouldNotDeleteMatchAndMatchPlayersOnFailedCancel() {
        try {
            doThrow(new TelegramApiCallException("", new TelegramApiException())).when(messagingService).deleteMessageAsync(ArgumentMatchers.any(ExternalMessageId.class));
            processor.process(commandMessage, mockLoggingId);
        } catch (TelegramApiCallException ignored) {
        }

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(*) from matches where id = 10000", Long.class);
        Long actualMatchPlayersCount = jdbcTemplate.queryForObject("select count(*) from match_players where player_id = 10000", Long.class);

        assertEquals(1, actualMatchesCount);
        assertEquals(1, actualMatchPlayersCount);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.INCLUDE, names = {"FAILED", "FINISHED"})
    void shouldNotDeleteMatchAndMatchPlayersOnFinishedMatchCancelRequest(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 10000");

        try {
            processor.process(commandMessage, mockLoggingId);
        } catch (AnswerableDuneBotException ignored) {
        }

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(*) from matches where id = 10000", Long.class);
        Long actualMatchPlayersCount = jdbcTemplate.queryForObject("select count(*) from match_players where player_id = 10000", Long.class);

        assertEquals(1, actualMatchesCount);
        assertEquals(1, actualMatchPlayersCount);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.INCLUDE, names = {"FAILED", "FINISHED"})
    void shouldNotSendTelegramDeleteRequestOnFinishedMatchCancelRequest(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 10000");

        try {
            processor.process(commandMessage, mockLoggingId);
        } catch (AnswerableDuneBotException ignored) {
        }

        verifyNoInteractions(messagingService);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.INCLUDE, names = {"FAILED", "FINISHED"})
    void shouldThrowOnFinishedMatchCancelRequest(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 10000");

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> processor.process(commandMessage, mockLoggingId));

        assertEquals("Запрещено отменять завершенные матчи!", actualException.getMessage());
    }

    @Test
    void shouldReturnCancelCommand() {
        Command actualCommand = processor.getCommand();

        assertEquals(Command.CANCEL, actualCommand);
    }

    private CommandMessage getCommandMessage() {
        Chat chat = new Chat();
        chat.setId(Long.parseLong(CHAT_ID));
        chat.setType(ChatType.PRIVATE.getValue());
        Message reply = new Message();
        reply.setMessageId(REPLY_ID);
        User user = new User();
        user.setId(TELEGRAM_USER_ID);
        Message message = new Message();
        message.setMessageId(MESSAGE_ID);
        message.setChat(chat);
        message.setFrom(user);
        message.setReplyToMessage(reply);
        return CommandMessage.getMessageInstance(message);
    }
}
