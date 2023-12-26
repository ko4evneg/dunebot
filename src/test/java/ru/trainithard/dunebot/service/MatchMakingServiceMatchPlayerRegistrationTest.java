package ru.trainithard.dunebot.service;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.dto.TelegramUserPollDto;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class MatchMakingServiceMatchPlayerRegistrationTest extends TestContextMock {
    @Autowired
    private MatchCommandProcessor matchCommandProcessor;

    private static final String TELEGRAM_POLL_ID = "100500";
    private static final long TELEGRAM_USER_ID = 12349L;
    private static final TelegramUserPollDto POLL_MESSAGE_DTO = new TelegramUserPollDto(12349L, "100500");

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, telegram_id, telegram_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 12345, 12345, 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, telegram_id, telegram_chat_id, steam_name, first_name, created_at) " +
                "values (10001, " + TELEGRAM_USER_ID + ", 12345, 'st_pl2', 'name2', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, telegram_poll_id, telegram_message_id, owner_id, mod_type, registered_players_count, created_at) " +
                "values (10000, '" + TELEGRAM_POLL_ID + "', '123', 10000, '" + ModType.CLASSIC + "', 1, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10000, 10000, 10000, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id = (select id from matches where id = 10000)");
        jdbcTemplate.execute("delete from matches where id = 10000");
        jdbcTemplate.execute("delete from players where id between 10000 and 10002 or telegram_id = " + TELEGRAM_USER_ID);
    }

    @Test
    void shouldSaveNewMatchPlayerOnRegistration() {
        matchCommandProcessor.registerMathPlayer(POLL_MESSAGE_DTO);

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, containsInAnyOrder(10000L, 10001L));
    }

    @Test
    void shouldIncreaseMatchRegisteredPlayersCountOnRegistration() {
        matchCommandProcessor.registerMathPlayer(POLL_MESSAGE_DTO);

        Long actualPlayersCount = jdbcTemplate.queryForObject("select registered_players_count from matches where id = 10000", Long.class);

        assertEquals(2, actualPlayersCount);
    }

    @Test
    void shouldSendNotificationOnFourthPlayerRegistration() {
        // TODO:
    }

    @Test
    void shouldDeleteMatchPlayerOnRegistrationRevocation() {
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10001, 10000, 10001, '2010-10-10')");

        matchCommandProcessor.unregisterMathPlayer(POLL_MESSAGE_DTO);

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, contains(10000L));
    }

    @Test
    void shouldDecreaseMatchRegisteredPlayersCountOnRegistrationRevocation() {
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10001, 10000, 10001, '2010-10-10')");
        jdbcTemplate.execute("update matches set registered_players_count = 2");

        matchCommandProcessor.unregisterMathPlayer(POLL_MESSAGE_DTO);

        Long actualPlayersCount = jdbcTemplate.queryForObject("select registered_players_count from matches where id = 10000", Long.class);

        assertEquals(1, actualPlayersCount);
    }
}
