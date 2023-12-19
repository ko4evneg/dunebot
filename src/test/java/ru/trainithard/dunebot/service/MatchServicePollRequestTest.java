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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class MatchServicePollRequestTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MatchService matchService;
    @MockBean
    private TelegramBot telegramBot;

    private static final String POLL_ID = "12345";
    private static final Integer MESSAGE_ID = 100500;
    private final Player player1 = new Player();

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        player1.setId(1L);
        player1.setSteamName("st_AKos");
        player1.setUserName("tg_AKos");

        Poll poll = new Poll();
        poll.setId(POLL_ID);
        Message message = new Message();
        message.setPoll(poll);
        message.setMessageId(MESSAGE_ID);
        CompletableFuture<Message> completableFuture = new CompletableFuture<>();
        completableFuture.complete(message);
        doReturn(completableFuture).when(telegramBot).executeAsync(ArgumentMatchers.any(SendPoll.class));
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from player_matches where player_id between 1 and 4");
        jdbcTemplate.execute("delete from matches where telegram_poll_id is null or telegram_poll_id = '" + POLL_ID + "'");
    }

    @Test
    void shouldCreateNewMatch() throws TelegramApiException {
        matchService.requestNewMatch(player1, ModType.CLASSIC);

        Long actualMatchId = jdbcTemplate.queryForObject("select id from matches " +
                "where id = (select match_id from player_matches where player_id = 1)", Long.class);

        assertNotNull(actualMatchId);
    }

    @Test
    void shouldCorrectlyFillNewMatch() throws TelegramApiException {
        matchService.requestNewMatch(player1, ModType.CLASSIC);

        Match actualMatch = jdbcTemplate.queryForObject("select telegram_message_id, telegram_poll_id from matches where " +
                "id = (select match_id from player_matches where player_id = 1 and owner_id = 1)", new BeanPropertyRowMapper<>(Match.class));

        assertThat(actualMatch,
                both(hasProperty("telegramPollId", is(POLL_ID)))
                        .and(hasProperty("telegramMessageId", is(MESSAGE_ID)))
        );
    }

    @Test
    void shouldCreateNewMatchPlayerWith() throws TelegramApiException {
        matchService.requestNewMatch(player1, ModType.CLASSIC);

        Long actualMatchPlayerId = jdbcTemplate.queryForObject("select id from player_matches where player_id = 1 and place is null", Long.class);

        assertNotNull(actualMatchPlayerId);
    }

    @Test
    void shouldSendTelegramPoll() throws TelegramApiException {
        matchService.requestNewMatch(player1, ModType.CLASSIC);

        ArgumentCaptor<SendPoll> pollCaptor = ArgumentCaptor.forClass(SendPoll.class);
        verify(telegramBot).executeAsync(pollCaptor.capture());
        SendPoll actualPoll = pollCaptor.getValue();

        assertThat(actualPoll, allOf(
                hasProperty("allowMultipleAnswers", is(false)),
                hasProperty("isAnonymous", is(false)),
                hasProperty("question", is("Игрок st_AKos (tg_AKos) призывает всех на матч в " + ModType.CLASSIC.getModName())),
                hasProperty("options", contains("Да", "Нет", "Результат"))
        ));
    }

    @ParameterizedTest
    @MethodSource("chatIdSource")
    void shouldSetCorrectTelegramPollChatAndTopicIds(ModType modType, int expectedTopicId) throws TelegramApiException {
        matchService.requestNewMatch(player1, modType);

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
                "id = (select match_id from player_matches where player_id = 1 and telegram_poll_id = '" + POLL_ID + "')", Long.class);

        assertEquals(0, actualMatchesCount);
    }

    @Test
    void shouldThrowWhenTelegramCallFails() throws TelegramApiException {
        doThrow(new TelegramApiException()).when(telegramBot).executeAsync(ArgumentMatchers.any(SendPoll.class));

        assertThrows(TelegramApiException.class, () -> matchService.requestNewMatch(player1, ModType.CLASSIC));
    }
}
