package ru.trainithard.dunebot.service;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.ModType;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static ru.trainithard.dunebot.configuration.SettingConstants.CHAT_ID;

@SpringBootTest
class MatchMakingServiceCancelPollTest extends TestContextMock {
    @Autowired
    private MatchCommandProcessor matchCommandProcessor;

    private static final int MESSAGE_ID = 100500;
    private static final long TELEGRAM_USER_ID = 12345L;

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        CompletableFuture<Boolean> completableFuture = CompletableFuture.completedFuture(true);
        doReturn(completableFuture).when(telegramBot).executeAsync(ArgumentMatchers.any(DeleteMessage.class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 12345, 9000, 'st_pl', 'name', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_message_id, external_chat_id, owner_id, mod_type, positive_answers_count, created_at) " +
                "values (10000, '12346', '" + MESSAGE_ID + "', '" + CHAT_ID + "', 10000, '" + ModType.CLASSIC + "', 1, '2010-10-10') ");
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
        matchCommandProcessor.cancelMatch(TELEGRAM_USER_ID);

        ArgumentCaptor<DeleteMessage> deleteMessageCaptor = ArgumentCaptor.forClass(DeleteMessage.class);
        verify(telegramBot).executeAsync(deleteMessageCaptor.capture());
        DeleteMessage actualDeleteMessage = deleteMessageCaptor.getValue();

        assertEquals(MESSAGE_ID, actualDeleteMessage.getMessageId());
        assertEquals(CHAT_ID, actualDeleteMessage.getChatId());
    }

    @Test
    void shouldDeleteMatch() {
        matchCommandProcessor.cancelMatch(TELEGRAM_USER_ID);

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(*) from matches where id = 10000", Long.class);

        assertEquals(0, actualMatchesCount);
    }

    @Test
    void shouldDeleteMatchPlayers() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10001, 12346, 9000, 'st_pl2', 'name2', '2010-10-10') ");

        matchCommandProcessor.cancelMatch(TELEGRAM_USER_ID);

        Long actualMatchPlayersCount = jdbcTemplate.queryForObject("select count(*) from match_players where player_id = 10000", Long.class);

        assertEquals(0, actualMatchPlayersCount);
    }

    @Test
    @Disabled
        // TODO:
    void shouldThrowOnFailedCancel() throws TelegramApiException {
        doThrow(new TelegramApiException()).when(telegramBot).executeAsync(ArgumentMatchers.any(DeleteMessage.class));

        assertThrows(TelegramApiException.class, () -> matchCommandProcessor.cancelMatch(TELEGRAM_USER_ID));
    }

    @Test
    void shouldNotDeleteMatchAndMatchPlayersOnFailedCancel() {
        try {
            doThrow(new TelegramApiCallException("", new TelegramApiException())).when(telegramBot).executeAsync(ArgumentMatchers.any(DeleteMessage.class));
            matchCommandProcessor.cancelMatch(TELEGRAM_USER_ID);
        } catch (TelegramApiCallException | TelegramApiException ignored) {
        }

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(*) from matches where id = 10000", Long.class);
        Long actualMatchPlayersCount = jdbcTemplate.queryForObject("select count(*) from match_players where player_id = 10000", Long.class);

        assertEquals(1, actualMatchesCount);
        assertEquals(1, actualMatchPlayersCount);
    }

    @Test
    void shouldNotDeleteMatchAndMatchPlayersOnFinishedMatchCancelRequest() {
        jdbcTemplate.execute("update matches set is_finished = true where id = 10000");

        try {
            matchCommandProcessor.cancelMatch(TELEGRAM_USER_ID);
        } catch (Exception ignored) {
        }

        Long actualMatchesCount = jdbcTemplate.queryForObject("select count(*) from matches where id = 10000", Long.class);
        Long actualMatchPlayersCount = jdbcTemplate.queryForObject("select count(*) from match_players where player_id = 10000", Long.class);

        assertEquals(1, actualMatchesCount);
        assertEquals(1, actualMatchPlayersCount);
    }

    @Test
    void shouldNotSendTelegramDeleteRequestOnFinishedMatchCancelRequest() {
        jdbcTemplate.execute("update matches set is_finished = true where id = 10000");

        try {
            matchCommandProcessor.cancelMatch(TELEGRAM_USER_ID);
        } catch (Exception ignored) {
        }

        verifyNoInteractions(telegramBot);
    }

    @Test
    void shouldThrowOnFinishedMatchCancelRequest() {
        jdbcTemplate.execute("update matches set is_finished = true where id = 10000");

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> matchCommandProcessor.cancelMatch(TELEGRAM_USER_ID));
        assertEquals("Запрещено отменять завершенные матчи!", actualException.getMessage());
    }
}
