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
import ru.trainithard.dunebot.TestConstants;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest
class CancelCommandProcessorTest extends TestContextMock {
    private static final Integer REPLY_ID = 9000;
    private static final int MESSAGE_ID = 100500;
    private static final long TELEGRAM_USER_ID = 12345L;
    private final CommandMessage commandMessage = getCommandMessage();

    @Autowired
    private CancelCommandProcessor processor;

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        doNothing().when(messagingService).deleteMessageAsync(ArgumentMatchers.any(ExternalMessageId.class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, 12345, 9000, 'st_pl', 'name', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                             "values (10000, 'ExternalPollId'," + MESSAGE_ID + ", " + TestConstants.CHAT_ID + ", " + REPLY_ID + ", '12346', '2020-10-10')");
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
        processor.process(commandMessage);

        ArgumentCaptor<ExternalMessageId> messageIdCaptor = ArgumentCaptor.forClass(ExternalMessageId.class);
        verify(messagingService).deleteMessageAsync(messageIdCaptor.capture());
        ExternalMessageId actualMessageId = messageIdCaptor.getValue();

        assertThat(actualMessageId)
                .extracting(ExternalMessageId::getMessageId, ExternalMessageId::getChatId, ExternalMessageId::getReplyId)
                .containsExactly(MESSAGE_ID, Long.parseLong(TestConstants.CHAT_ID), REPLY_ID);
    }

    @Test
    void shouldSetMatchCancelledState() {
        processor.process(commandMessage);

        MatchState actualMatchState = jdbcTemplate.queryForObject("select state from matches where id = 10000", MatchState.class);

        assertThat(actualMatchState).isEqualTo(MatchState.CANCELLED);
    }

    @Test
    void shouldThrowOnFailedCancel() {
        doThrow(new TelegramApiCallException("", new TelegramApiException())).when(messagingService).deleteMessageAsync(ArgumentMatchers.any(ExternalMessageId.class));

        assertThatThrownBy(() -> processor.process(commandMessage))
                .isInstanceOf(TelegramApiCallException.class);
    }

    @Test
    void shouldNotDeleteMatchAndMatchPlayersOnFailedCancel() {
        try {
            doThrow(new TelegramApiCallException("", new TelegramApiException())).when(messagingService).deleteMessageAsync(ArgumentMatchers.any(ExternalMessageId.class));
            processor.process(commandMessage);
        } catch (TelegramApiCallException ignored) {
        }

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(*) from matches where id = 10000", Long.class);
        Long actualMatchPlayersCount = jdbcTemplate.queryForObject("select count(*) from match_players where player_id = 10000", Long.class);

        assertThat(actualMatchesCount).isEqualTo(1);
        assertThat(actualMatchPlayersCount).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.INCLUDE, names = {"FAILED", "FINISHED"})
    void shouldNotDeleteMatchAndMatchPlayersOnFinishedMatchCancelRequest(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 10000");

        try {
            processor.process(commandMessage);
        } catch (AnswerableDuneBotException ignored) {
        }

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(*) from matches where id = 10000", Long.class);
        Long actualMatchPlayersCount = jdbcTemplate.queryForObject("select count(*) from match_players where player_id = 10000", Long.class);

        assertThat(actualMatchesCount).isEqualTo(1);
        assertThat(actualMatchPlayersCount).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.INCLUDE, names = {"FAILED", "FINISHED"})
    void shouldNotSendTelegramDeleteRequestOnFinishedMatchCancelRequest(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 10000");

        try {
            processor.process(commandMessage);
        } catch (AnswerableDuneBotException ignored) {
        }

        verifyNoInteractions(messagingService);
    }

    @Test
    void shouldSendMessageOnInvalidIdMatchCancelRequest() {
        jdbcTemplate.execute("delete from match_players where match_id = (select id from matches where id in (10000, 10001))");
        jdbcTemplate.execute("delete from matches where id = 10000");

        processor.process(commandMessage);

        verify(messagingService).sendMessageAsync(argThat((MessageDto message) ->
                TestConstants.CHAT_ID.equals(message.getChatId())
                && REPLY_ID.equals(message.getReplyMessageId())
                && "Не найдены матчи, которые можно завершить\\!".equals(message.getText())));
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.INCLUDE, names = {"FAILED", "FINISHED"})
    void shouldThrowOnFinishedMatchCancelRequest(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 10000");

        assertThatThrownBy(() -> processor.process(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Запрещено отменять завершенные матчи!");
    }

    @Test
    void shouldReturnCancelCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.CANCEL);
    }

    private CommandMessage getCommandMessage() {
        Chat chat = new Chat();
        chat.setId(Long.parseLong(TestConstants.CHAT_ID));
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
