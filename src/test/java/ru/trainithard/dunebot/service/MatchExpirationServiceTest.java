package ru.trainithard.dunebot.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
class MatchExpirationServiceTest extends TestContextMock {
    private static final LocalDateTime NOW = LocalDateTime.of(2010, 10, 10, 13, 0, 0);
    @MockBean
    private Clock clock;
    @Autowired
    private MatchExpirationService expirationService;

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, 11000, 12000, 'st_pl1', 'f1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, 11001, 12001, 'st_pl2', 'f2', 'l2', 'e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, reply_id, created_at) " +
                             "values (10000, 'ExternalPollId', 10000, 12345, '10000', 9000, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, reply_id, created_at) " +
                             "values (10001, 'ExternalPollId', 10001, 12345, '10000', 9000, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, owner_id, mod_type, state, positive_answers_count, screenshot_path, created_at) " +
                             "values (10000, 10000, 10000, '" + ModType.CLASSIC + "', '" + MatchState.NEW + "', 1, 'photos/1.jpg', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, owner_id, mod_type, state, positive_answers_count, screenshot_path, created_at) " +
                             "values (10001, 10001, 10000, '" + ModType.CLASSIC + "', '" + MatchState.NEW + "', 1, 'photos/1.jpg', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, candidate_place, created_at) " +
                             "values (10000, 10000, 10000, null, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, candidate_place, created_at) " +
                             "values (10001, 10001, 10001, 2, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id between 10000 and 10001");
        jdbcTemplate.execute("delete from matches where id between 10000 and 10001");
        jdbcTemplate.execute("delete from players where id between 10000 and 10001");
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10001");
    }

    @Test
    void shouldExpireNewMatchesWhenTimeoutReached() {
        expirationService.expireUnusedMatches();

        List<MatchState> actualStates = jdbcTemplate
                .queryForList("select state from matches where id between 10000 and 10001", MatchState.class);

        assertThat(actualStates).containsExactly(MatchState.EXPIRED, MatchState.EXPIRED);
    }

    @Test
    void shouldNotExpireNewMatchesWhenTimeoutNotReached() {
        Clock fixedClock = Clock.fixed(NOW.minusHours(1).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        expirationService.expireUnusedMatches();

        List<MatchState> actualStates = jdbcTemplate
                .queryForList("select state from matches where id between 10000 and 10001", MatchState.class);

        assertThat(actualStates).containsExactly(MatchState.NEW, MatchState.NEW);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"NEW"})
    void shouldNotExpireMatchesWhenStateIsNotNew(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id between 10000 and 10001");

        expirationService.expireUnusedMatches();

        List<MatchState> actualStates = jdbcTemplate
                .queryForList("select state from matches where id between 10000 and 10001", MatchState.class);

        assertThat(actualStates).containsExactly(matchState, matchState);
    }
}
