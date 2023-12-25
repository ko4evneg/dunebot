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
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.DubeBotException;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.dto.TelegramUserMessageDto;

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
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TextCommandProcessor textCommandProcessor;

    private static final String POLL_ID = "12345";
    private static final int MESSAGE_ID = 100500;
    private static final long CHAT_ID = 9000L;
    private final Player player1 = new Player();
    public static final TelegramUserMessageDto telegramUserMessage = new TelegramUserMessageDto(MESSAGE_ID, CHAT_ID, POLL_ID);

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, telegram_id, telegram_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 12345, 9000, 'st_pl', 'name', '2010-10-10') ");

        player1.setId(10000L);
        player1.setSteamName("st_AKos");
        player1.setFirstName("tg_AKos");

        Poll poll = new Poll();
        poll.setId(POLL_ID);
        Message message = new Message();
        message.setPoll(poll);
        message.setMessageId(MESSAGE_ID);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        message.setChat(chat);
        CompletableFuture<Message> completableFuture = new CompletableFuture<>();
        completableFuture.complete(message);
        doReturn(completableFuture).when(telegramBot).executeAsync(ArgumentMatchers.any(SendPoll.class));
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where player_id = 10000");
        jdbcTemplate.execute("delete from matches where telegram_poll_id is null or telegram_poll_id = '" + POLL_ID + "'");
        jdbcTemplate.execute("delete from players where id = 10000");
    }

    @Test
    void shouldCreateNewMatch() {
        textCommandProcessor.registerNewMatch(12345L, ModType.CLASSIC);

        Long actualMatchId = jdbcTemplate.queryForObject("select id from matches " +
                "where id = (select match_id from match_players where player_id = 10000 and telegram_poll_id = '" + POLL_ID + "' and telegram_chat_id = '" + CHAT_ID + "')", Long.class);

        assertNotNull(actualMatchId);
    }

    @Test
    void shouldCorrectlyFillNewMatch() {
        textCommandProcessor.registerNewMatch(12345L, ModType.CLASSIC);

        Match actualMatch = jdbcTemplate.queryForObject("select telegram_message_id, telegram_poll_id, telegram_chat_id from matches where id = " +
                        "(select match_id from match_players where player_id = 10000 and owner_id = 10000 and registered_players_count = 0)",
                new BeanPropertyRowMapper<>(Match.class));

        assertThat(actualMatch, allOf(
                hasProperty("telegramPollId", is(POLL_ID)),
                hasProperty("telegramMessageId", is(MESSAGE_ID)),
                hasProperty("telegramChatId", is(CHAT_ID)),
                hasProperty("finished", is(false))
        ));
    }

    @Test
    void shouldCreateNewMatchPlayer() {
        textCommandProcessor.registerNewMatch(12345L, ModType.CLASSIC);

        Long actualMatchPlayerId = jdbcTemplate.queryForObject("select id from match_players " +
                "where player_id = 10000 and place is null", Long.class);

        assertNotNull(actualMatchPlayerId);
    }

    @Test
    void shouldSendTelegramPoll() throws TelegramApiException {
        textCommandProcessor.registerNewMatch(12345L, ModType.CLASSIC);

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
        textCommandProcessor.registerNewMatch(12345L, modType);

        ArgumentCaptor<SendPoll> pollCaptor = ArgumentCaptor.forClass(SendPoll.class);
        verify(telegramBot).executeAsync(pollCaptor.capture());
        SendPoll actualPoll = pollCaptor.getValue();

        assertThat(actualPoll,
                both(hasProperty("chatId", is(SettingConstants.CHAT_ID)))
                        .and(hasProperty("messageThreadId", is(expectedTopicId))));
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
                "id = (select match_id from match_players where player_id = 1 and telegram_poll_id = '" + POLL_ID + "')", Long.class);

        assertEquals(0, actualMatchesCount);
    }

    @Test
    void shouldThrowWhenTelegramCallFails() throws TelegramApiException {
        doThrow(new TelegramApiCallException("", new TelegramApiException())).when(telegramBot).executeAsync(ArgumentMatchers.any(SendPoll.class));

        assertThrows(DubeBotException.class, () -> textCommandProcessor.registerNewMatch(12345L, ModType.CLASSIC));
    }
}
