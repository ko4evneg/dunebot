package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.messaging.dto.ExternalPollDto;
import ru.trainithard.dunebot.service.messaging.dto.PollMessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class NewCommandProcessorTest extends TestContextMock {
    @Autowired
    private NewCommandProcessor processor;

    private static final long USER_ID = 12345;
    private static final String POLL_ID = "12345";
    private static final int REPLY_ID = 23456;
    private static final int MESSAGE_ID = 100500;
    private static final long CHAT_ID = 9000L;
    private final CommandMessage pollCommandMessage = getCommandMessage(ModType.CLASSIC.getAlias());

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                "values (10000, " + USER_ID + ", 9000, 'st_pl', 'name', 'l1', 'e1', '2010-10-10') ");

        doReturn(getCompletableFuturePollMessage()).when(messagingService).sendPollAsync(ArgumentMatchers.any(PollMessageDto.class));
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where player_id = 10000");
        jdbcTemplate.execute("delete from matches where external_poll_id is null or external_poll_id = " +
                "(select id from external_messages where chat_id = '" + CHAT_ID + "')");
        jdbcTemplate.execute("delete from players where id = 10000");
        jdbcTemplate.execute("delete from external_messages where message_id = " + MESSAGE_ID);
    }

    @Test
    void shouldCreateNewMatch() {
        processor.process(pollCommandMessage, mockLoggingId);

        Long actualMessageId = jdbcTemplate.queryForObject("select id from external_messages where poll_id = '" + POLL_ID + "'", Long.class);
        Boolean isMatchCreated = jdbcTemplate.queryForObject("select exists(select 1 from matches where external_poll_id = " + actualMessageId + ")", Boolean.class);

        assertNotNull(isMatchCreated);
        assertTrue(isMatchCreated);
    }

    @Test
    void shouldCorrectlyFillNewMatch() {
        processor.process(pollCommandMessage, mockLoggingId);

        Long actualMessageId = jdbcTemplate.queryForObject("select id from external_messages where chat_id = " +
                CHAT_ID + " and poll_id = '" + POLL_ID + "' and message_id = " + MESSAGE_ID + " and reply_id = " + REPLY_ID, Long.class);
        MatchState actualMatchState = jdbcTemplate.queryForObject("select state from matches where external_poll_id = " + actualMessageId +
                " and owner_id = 10000 and positive_answers_count = 0 and submits_count = 0 and submits_retry_count = 0", MatchState.class);

        assertNotNull(actualMatchState);
        assertEquals(MatchState.NEW, actualMatchState);
    }

    @Test
    void shouldNotCreateAnyMatchPlayer() {
        processor.process(pollCommandMessage, mockLoggingId);

        Long actualMessageId = jdbcTemplate.queryForObject("select id from external_messages where poll_id = '" + POLL_ID + "'", Long.class);
        Boolean isMatchPlayerExist = jdbcTemplate.queryForObject("select exists(select 1 from match_players " +
                "where match_id = (select id from matches where external_poll_id = " + actualMessageId + "))", Boolean.class);

        assertNotNull(isMatchPlayerExist);
        assertFalse(isMatchPlayerExist);
    }

    @Test
    void shouldSendTelegramPoll() {
        processor.process(pollCommandMessage, mockLoggingId);

        ArgumentCaptor<PollMessageDto> pollCaptor = ArgumentCaptor.forClass(PollMessageDto.class);
        verify(messagingService).sendPollAsync(pollCaptor.capture());
        PollMessageDto actualPoll = pollCaptor.getValue();

        assertThat(actualPoll, allOf(
                hasProperty("text", is("Игрок name (st_pl) l1 призывает всех на матч в " + ModType.CLASSIC.getModName())),
                hasProperty("options", contains("Да", "Нет", "Результат"))
        ));
    }

    @ParameterizedTest
    @MethodSource("chatIdSource")
    void shouldSetCorrectTelegramPollChatAndTopicIds(ModType modType, int expectedTopicId) {
        processor.process(getCommandMessage(modType.getAlias()), mockLoggingId);

        ArgumentCaptor<PollMessageDto> pollCaptor = ArgumentCaptor.forClass(PollMessageDto.class);
        verify(messagingService).sendPollAsync(pollCaptor.capture());
        PollMessageDto actualPoll = pollCaptor.getValue();

        assertThat(actualPoll,
                both(hasProperty("chatId", is(SettingConstants.CHAT_ID)))
                        .and(hasProperty("replyMessageId", is(expectedTopicId))));
    }

    private static Stream<Arguments> chatIdSource() {
        return Stream.of(
                Arguments.of(ModType.CLASSIC, SettingConstants.TOPIC_ID_CLASSIC),
                Arguments.of(ModType.UPRISING_4, SettingConstants.TOPIC_ID_UPRISING),
                Arguments.of(ModType.UPRISING_6, SettingConstants.TOPIC_ID_UPRISING)
        );
    }

    @Test
    void shouldNotCreateMatchWhenTelegramCallFails() {
        doThrow(new TelegramApiCallException("", new RuntimeException())).when(messagingService).sendPollAsync(ArgumentMatchers.any(PollMessageDto.class));

        try {
            processor.process(pollCommandMessage, mockLoggingId);
        } catch (TelegramApiCallException ignored) {
        }

        Boolean isMatchExist = jdbcTemplate.queryForObject("select exists(select * from matches where owner_id = " +
                "(select id from players where external_id = " + USER_ID + "))", Boolean.class);

        assertNotNull(isMatchExist);
        assertFalse(isMatchExist);
    }

    @Test
    void shouldThrowWhenUnsupportedMatchTypeArgumentProvided() {
        CommandMessage commandMessage = getCommandMessage("fake");
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> processor.process(commandMessage, mockLoggingId));
        assertEquals("Неподдерживаемый тип матча: fake", actualException.getMessage());
    }

    @Test
    void shouldReturnNewCommand() {
        Command actualCommand = processor.getCommand();

        assertEquals(Command.NEW, actualCommand);
    }

    private CommandMessage getCommandMessage(String modNameString) {
        Message message = new Message();
        message.setMessageId(MESSAGE_ID);
        message.setText("/new " + modNameString);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        message.setChat(chat);
        Message replyMessage = new Message();
        replyMessage.setMessageId(REPLY_ID);
        message.setReplyToMessage(replyMessage);
        User user = new User();
        user.setId(USER_ID);
        message.setFrom(user);
        return CommandMessage.getMessageInstance(message);
    }

    private CompletableFuture<ExternalPollDto> getCompletableFuturePollMessage() {
        Poll poll = new Poll();
        poll.setId(POLL_ID);
        Message message = new Message();
        message.setPoll(poll);
        message.setMessageId(MESSAGE_ID);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        message.setChat(chat);
        Message replyMessage = new Message();
        replyMessage.setMessageId(REPLY_ID);
        message.setReplyToMessage(replyMessage);
        CompletableFuture<Message> completableFuture = new CompletableFuture<>();
        completableFuture.complete(message);
        return CompletableFuture.completedFuture(new ExternalPollDto(message));
    }
}
