package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.SneakyThrows;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.ExternalPollDto;
import ru.trainithard.dunebot.service.messaging.dto.PollMessageDto;
import ru.trainithard.dunebot.service.telegram.ChatType;
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
    private NewCommandProcessor commandProcessor;
    @MockBean
    private MessagingService messagingService;

    private static final long USER_ID = 12345;
    private static final String POLL_ID = "12345";
    private static final int REPLY_ID = 23456;
    private static final int MESSAGE_ID = 100500;
    private static final long CHAT_ID = 9000L;
    private final CommandMessage pollCommandMessage = getCommandMessage(ModType.CLASSIC.getAlias());

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, '" + USER_ID + "', 9000, 'st_pl', 'name', '2010-10-10') ");

        doReturn(getCompletableFuturePollMessage()).when(messagingService).sendPollAsync(ArgumentMatchers.any(PollMessageDto.class));
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where player_id = 10000");
        jdbcTemplate.execute("delete from matches where matches.external_poll_id is null or external_poll_id = '" + POLL_ID + "'");
        jdbcTemplate.execute("delete from players where id = 10000");
    }

    @Test
    void shouldCreateNewMatch() {
        commandProcessor.process(pollCommandMessage);

        Long actualMatchId = jdbcTemplate.queryForObject("select id from matches " +
                "where id = (select match_id from match_players where player_id = 10000)", Long.class);

        assertNotNull(actualMatchId);
    }

    @Test
    void shouldCorrectlyFillNewMatch() {
        commandProcessor.process(pollCommandMessage);

        Boolean actualMatch = jdbcTemplate.queryForObject("select is_finished from matches where id = (select match_id " +
                "from match_players where player_id = 10000) and owner_id = 10000 and positive_answers_count = 0 and submits_count = 0 and external_chat_id = '" +
                CHAT_ID + "' and external_poll_id = '" + POLL_ID + "' and external_message_id = " + MESSAGE_ID + " and external_reply_id = '" + REPLY_ID + "'", Boolean.class);

        assertNotNull(actualMatch);
        assertFalse(actualMatch);
    }

    @Test
    void shouldCreateNewMatchPlayer() {
        commandProcessor.process(pollCommandMessage);

        Long actualMatchPlayerId = jdbcTemplate.queryForObject("select id from match_players " +
                "where player_id = 10000 and place is null and  candidate_place is null", Long.class);

        assertNotNull(actualMatchPlayerId);
    }

    @Test
    void shouldSendTelegramPoll() {
        commandProcessor.process(pollCommandMessage);

        ArgumentCaptor<PollMessageDto> pollCaptor = ArgumentCaptor.forClass(PollMessageDto.class);
        verify(messagingService).sendPollAsync(pollCaptor.capture());
        PollMessageDto actualPoll = pollCaptor.getValue();

        assertThat(actualPoll, allOf(
                hasProperty("allowMultipleAnswers", is(false)),
                hasProperty("anonymous", is(false)),
                hasProperty("text", is("Игрок st_pl (name) призывает всех на матч в " + ModType.CLASSIC.getModName())),
                hasProperty("options", contains("Да", "Нет", "Результат"))
        ));
    }

    @ParameterizedTest
    @MethodSource("chatIdSource")
    void shouldSetCorrectTelegramPollChatAndTopicIds(ModType modType, int expectedTopicId) {
        commandProcessor.process(getCommandMessage(modType.getAlias()));

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
            commandProcessor.process(pollCommandMessage);
        } catch (TelegramApiCallException ignored) {
        }

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(id) from matches where " +
                "id = (select match_id from match_players where player_id = 1 and external_poll_id = '" + POLL_ID + "')", Long.class);

        assertEquals(0, actualMatchesCount);
    }

    @Test
    void shouldThrowWhenUnsupportedMatchTypeArgumentProvided() {
        CommandMessage commandMessage = getCommandMessage("fake");
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> commandProcessor.process(commandMessage));
        assertEquals("Неподдерживаемый тип матча: fake", actualException.getMessage());
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
        return new CommandMessage(message);
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
