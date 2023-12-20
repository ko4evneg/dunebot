package ru.trainithard.dunebot.service;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static ru.trainithard.dunebot.configuration.SettingConstants.CHAT_ID;

@SpringBootTest
class MatchServiceCancelPollTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MatchService matchService;
    @MockBean
    private TelegramBot telegramBot;

    private static final int MESSAGE_ID = 100500;
    private static final long TELEGRAM_USER_ID = 12345L;

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        completableFuture.complete(true);
        doReturn(completableFuture).when(telegramBot).executeAsync(ArgumentMatchers.any(DeleteMessage.class));

        jdbcTemplate.execute("insert into players (id, telegram_id, steam_name, first_name, created_at) " +
                "values (10000, 12345, 'st_pl', 'name', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, telegram_poll_id, telegram_message_id, owner_id, mod_type, created_at) " +
                "values (10000, '12346', '" + MESSAGE_ID + "', 10000, '" + ModType.CLASSIC + "', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10000, 10000, 10000, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id = (select id from matches where id in (10000, 10001))");
        jdbcTemplate.execute("delete from matches where id = 10000");
        jdbcTemplate.execute("delete from players where id in (10000, 10001)");
    }

    @Test
    void shouldSendCorrectDeleteMessageRequest() throws TelegramApiException {
        matchService.cancelMatch(TELEGRAM_USER_ID);

        ArgumentCaptor<DeleteMessage> deleteMessageCaptor = ArgumentCaptor.forClass(DeleteMessage.class);
        verify(telegramBot).executeAsync(deleteMessageCaptor.capture());
        DeleteMessage actualDeleteMessage = deleteMessageCaptor.getValue();

        assertEquals(MESSAGE_ID, actualDeleteMessage.getMessageId());
        assertEquals(CHAT_ID, actualDeleteMessage.getChatId());
    }

    @Test
    void shouldDeleteMatch() throws TelegramApiException {
        matchService.cancelMatch(TELEGRAM_USER_ID);

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(*) from matches where id = 10000", Long.class);

        assertEquals(0, actualMatchesCount);
    }

    @Test
    void shouldDeleteMatchPlayers() throws TelegramApiException {
        jdbcTemplate.execute("insert into players (id, telegram_id, steam_name, first_name, created_at) " +
                "values (10001, 12346, 'st_pl2', 'name2', '2010-10-10') ");

        matchService.cancelMatch(TELEGRAM_USER_ID);

        Long actualMatchPlayersCount = jdbcTemplate.queryForObject("select count(*) from match_players where player_id = 10000", Long.class);

        assertEquals(0, actualMatchPlayersCount);
    }

    @Test
    void shouldThrowOnFailedCancel() throws TelegramApiException {
        doThrow(new TelegramApiException()).when(telegramBot).executeAsync(ArgumentMatchers.any(DeleteMessage.class));

        assertThrows(TelegramApiException.class, () -> matchService.cancelMatch(TELEGRAM_USER_ID));
    }

    @Test
    void shouldNotDeleteMatchAndMatchPlayersOnFailedCancel() {
        try {
            doThrow(new TelegramApiException()).when(telegramBot).executeAsync(ArgumentMatchers.any(DeleteMessage.class));
            matchService.cancelMatch(TELEGRAM_USER_ID);
        } catch (TelegramApiException ignored) {
        }

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(*) from matches where id = 10000", Long.class);
        Long actualMatchPlayersCount = jdbcTemplate.queryForObject("select count(*) from match_players where player_id = 10000", Long.class);

        assertEquals(1, actualMatchesCount);
        assertEquals(1, actualMatchPlayersCount);
    }

    @Test
    void shouldNotDeleteMatchAndMatchPlayersOnFinishedMatchCancelRequest() throws TelegramApiException {
        jdbcTemplate.execute("update matches set is_finished = true where id = 10000");

        matchService.cancelMatch(TELEGRAM_USER_ID);

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(*) from matches where id = 10000", Long.class);
        Long actualMatchPlayersCount = jdbcTemplate.queryForObject("select count(*) from match_players where player_id = 10000", Long.class);

        assertEquals(1, actualMatchesCount);
        assertEquals(1, actualMatchPlayersCount);
    }

    @Test
    void shouldNotSendTelegramDeleteRequestOnFinishedMatchCancelRequest() throws TelegramApiException {
        jdbcTemplate.execute("update matches set is_finished = true where id = 10000");

        matchService.cancelMatch(TELEGRAM_USER_ID);

        verifyNoInteractions(telegramBot);
    }
}
