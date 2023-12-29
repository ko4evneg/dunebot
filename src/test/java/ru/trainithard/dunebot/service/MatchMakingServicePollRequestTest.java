package ru.trainithard.dunebot.service;

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
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.ModType;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class MatchMakingServicePollRequestTest extends TestContextMock {
    @Autowired
    private MatchCommandProcessor matchCommandProcessor;

    private static final String POLL_ID = "12345";
    private static final int REPLY_ID = 23456;
    private static final int MESSAGE_ID = 100500;
    private static final long CHAT_ID = 9000L;

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 12345, 9000, 'st_pl', 'name', '2010-10-10') ");

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
        doReturn(completableFuture).when(telegramBot).executeAsync(ArgumentMatchers.any(SendPoll.class));
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where player_id = 10000");
        jdbcTemplate.execute("delete from matches where matches.external_poll_id is null or external_poll_id = '" + POLL_ID + "'");
        jdbcTemplate.execute("delete from players where id = 10000");
    }

    @Test
    void shouldCreateNewMatch() {
        matchCommandProcessor.registerNewMatch(12345L, ModType.CLASSIC);

        Long actualMatchId = jdbcTemplate.queryForObject("select id from matches " +
                "where id = (select match_id from match_players where player_id = 10000)", Long.class);

        assertNotNull(actualMatchId);
    }

    @Test
    void shouldCorrectlyFillNewMatch() {
        matchCommandProcessor.registerNewMatch(12345L, ModType.CLASSIC);

        Boolean actualMatch = jdbcTemplate.queryForObject("select is_finished from matches where id = (select match_id " +
                "from match_players where player_id = 10000) and owner_id = 10000 and positive_answers_count = 0 and submits_count = 0 and external_chat_id = '" +
                CHAT_ID + "' and external_poll_id = '" + POLL_ID + "' and external_message_id = " + MESSAGE_ID + " and external_reply_id = '" + REPLY_ID + "'", Boolean.class);

        assertNotNull(actualMatch);
        assertFalse(actualMatch);
    }

    @Test
    void shouldCreateNewMatchPlayer() {
        matchCommandProcessor.registerNewMatch(12345L, ModType.CLASSIC);

        Long actualMatchPlayerId = jdbcTemplate.queryForObject("select id from match_players " +
                "where player_id = 10000 and place is null", Long.class);

        assertNotNull(actualMatchPlayerId);
    }

    @Test
    void shouldSendTelegramPoll() throws TelegramApiException {
        matchCommandProcessor.registerNewMatch(12345L, ModType.CLASSIC);

        ArgumentCaptor<SendPoll> pollCaptor = ArgumentCaptor.forClass(SendPoll.class);
        verify(telegramBot).executeAsync(pollCaptor.capture());
        SendPoll actualPoll = pollCaptor.getValue();

        assertThat(actualPoll, allOf(
                hasProperty("allowMultipleAnswers", is(false)),
                hasProperty("isAnonymous", is(false)),
                hasProperty("question", is("Игрок st_pl (name) призывает всех на матч в " + ModType.CLASSIC.getModName())),
                hasProperty("options", contains("Да", "Нет", "Результат"))
        ));
    }

    @ParameterizedTest
    @MethodSource("chatIdSource")
    void shouldSetCorrectTelegramPollChatAndTopicIds(ModType modType, int expectedTopicId) throws TelegramApiException {
        matchCommandProcessor.registerNewMatch(12345L, modType);

        ArgumentCaptor<SendPoll> pollCaptor = ArgumentCaptor.forClass(SendPoll.class);
        verify(telegramBot).executeAsync(pollCaptor.capture());
        SendPoll actualPoll = pollCaptor.getValue();

        assertThat(actualPoll,
                both(hasProperty("chatId", is(SettingConstants.CHAT_ID)))
                        .and(hasProperty("replyToMessageId", is(expectedTopicId))));
    }

    private static Stream<Arguments> chatIdSource() {
        return Stream.of(
                Arguments.of(ModType.CLASSIC, SettingConstants.TOPIC_ID_CLASSIC),
                Arguments.of(ModType.UPRISING_4, SettingConstants.TOPIC_ID_UPRISING),
                Arguments.of(ModType.UPRISING_6, SettingConstants.TOPIC_ID_UPRISING)
        );
    }

    @Test
    void shouldNotCreateMatchWhenTelegramCallFails() throws TelegramApiException {
        doThrow(new TelegramApiException()).when(telegramBot).executeAsync(ArgumentMatchers.any(SendPoll.class));

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(id) from matches where " +
                "id = (select match_id from match_players where player_id = 1 and external_poll_id = '" + POLL_ID + "')", Long.class);

        assertEquals(0, actualMatchesCount);
    }

    @Test
    void shouldThrowWhenTelegramCallFails() throws TelegramApiException {
        doThrow(new TelegramApiCallException("", new TelegramApiException())).when(telegramBot).executeAsync(ArgumentMatchers.any(SendPoll.class));

        assertThrows(TelegramApiCallException.class, () -> matchCommandProcessor.registerNewMatch(12345L, ModType.CLASSIC));
    }
}
