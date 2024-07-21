package ru.trainithard.dunebot.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JdbcRatingDateRepositoryTest extends TestContextMock {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RatingDateRepository dateRepository;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id,external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, 10000, '10000', 'st_pl1', 'name1', 'l1','e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id,external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, 10001, '10001', 'st_pl2', 'name2', 'l2','e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, state, created_at) " +
                             "values (10000, 10000, '" + ModType.CLASSIC + "', '" + MatchState.NEW + "', '2010-10-10') ");
        jdbcTemplate.execute("insert into leaders (id, name, short_name, mod_type, created_at) values " +
                             "(10000, 'la leader 1', 'la leader 1', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, short_name, mod_type, created_at) values " +
                             "(10001, 'la leader 2', 'la leader 2', '" + ModType.CLASSIC + "', '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from player_ratings where player_id between 10000 and 10001");
        jdbcTemplate.execute("delete from leader_ratings where leader_id between 10000 and 10001");
        jdbcTemplate.execute("delete from matches where id between 10000 and 10000");
        jdbcTemplate.execute("delete from leaders where id between 10000 and 10001");
        jdbcTemplate.execute("delete from players where id between 10000 and 10001");
    }

    @ParameterizedTest
    @MethodSource("classSource")
    void shouldReturnEmptyListWhenNoRecordsSet(Class<? extends AbstractRating> aClass) {
        List<RatingDate> latestRatings = dateRepository.findLatestRatings(aClass);

        assertThat(latestRatings).isEmpty();
    }

    @Test
    void shouldReturnOnlyLatestPlayerRatingDates() {
        jdbcTemplate.execute("insert into player_ratings (id, player_id, RATING_DATE, EFFICIENCY, WIN_RATE, " +
                             "FIRST_PLACE_COUNT, SECOND_PLACE_COUNT, THIRD_PLACE_COUNT, FOURTH_PLACE_COUNT, " +
                             "STRIKE_LENGTH, LAST_STRIKE_MATCH_ID, created_at) " +
                             "values (10000, 10000, '2010-10-2',0,0,0,0,0,0,0, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, RATING_DATE, EFFICIENCY, WIN_RATE, " +
                             "FIRST_PLACE_COUNT, SECOND_PLACE_COUNT, THIRD_PLACE_COUNT, FOURTH_PLACE_COUNT, " +
                             "STRIKE_LENGTH, LAST_STRIKE_MATCH_ID, created_at) " +
                             "values (10001, 10000, '2010-10-3',0,0,0,0,0,0,0, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, RATING_DATE, EFFICIENCY, WIN_RATE, " +
                             "FIRST_PLACE_COUNT, SECOND_PLACE_COUNT, THIRD_PLACE_COUNT, FOURTH_PLACE_COUNT, " +
                             "STRIKE_LENGTH, LAST_STRIKE_MATCH_ID, created_at) " +
                             "values (10002, 10000, '2010-10-1',0,0,0,0,0,0,0, 10000,'2010-10-10')");

        List<RatingDate> latestRatings = dateRepository.findLatestRatings(PlayerRating.class);

        assertThat(latestRatings)
                .hasSize(1)
                .containsExactly(new RatingDate(10000L, LocalDate.of(2010, 10, 3)));
    }

    @Test
    void shouldReturnOnlLeaderRatingDates() {
        jdbcTemplate.execute("insert into player_ratings (id, player_id, RATING_DATE, EFFICIENCY, WIN_RATE, " +
                             "FIRST_PLACE_COUNT, SECOND_PLACE_COUNT, THIRD_PLACE_COUNT, FOURTH_PLACE_COUNT, " +
                             "STRIKE_LENGTH, LAST_STRIKE_MATCH_ID, created_at) " +
                             "values (10000, 10000, '2010-10-3',0,0,0,0,0,0,0, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, RATING_DATE, EFFICIENCY, WIN_RATE, " +
                             "FIRST_PLACE_COUNT, SECOND_PLACE_COUNT, THIRD_PLACE_COUNT, FOURTH_PLACE_COUNT, " +
                             "STRIKE_LENGTH, LAST_STRIKE_MATCH_ID, created_at) " +
                             "values (10001, 10000, '2010-10-4',0,0,0,0,0,0,0, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, RATING_DATE, EFFICIENCY, WIN_RATE, " +
                             "FIRST_PLACE_COUNT, SECOND_PLACE_COUNT, THIRD_PLACE_COUNT, FOURTH_PLACE_COUNT, " +
                             "STRIKE_LENGTH, LAST_STRIKE_MATCH_ID, created_at) " +
                             "values (10002, 10000, '2010-10-5',0,0,0,0,0,0,0, 10000,'2010-10-10')");
        jdbcTemplate.execute("insert into leader_ratings (id, leader_id, RATING_DATE, EFFICIENCY, WIN_RATE, " +
                             "FIRST_PLACE_COUNT, SECOND_PLACE_COUNT, THIRD_PLACE_COUNT, FOURTH_PLACE_COUNT, created_at) " +
                             "values (10000, 10000, '2010-10-2',0,0,0,0,0,0,'2010-10-10')");
        jdbcTemplate.execute("insert into leader_ratings (id, leader_id, RATING_DATE, EFFICIENCY, WIN_RATE, " +
                             "FIRST_PLACE_COUNT, SECOND_PLACE_COUNT, THIRD_PLACE_COUNT, FOURTH_PLACE_COUNT, created_at) " +
                             "values (10001, 10000, '2010-10-1',0,0,0,0,0,0,'2010-10-10')");

        List<RatingDate> latestRatings = dateRepository.findLatestRatings(LeaderRating.class);

        assertThat(latestRatings)
                .hasSize(1)
                .containsExactly(new RatingDate(10000L, LocalDate.of(2010, 10, 2)));
    }

    @Test
    void shouldReturnRatingDatesForEveryPlayer() {
        jdbcTemplate.execute("insert into player_ratings (id, player_id, RATING_DATE, EFFICIENCY, WIN_RATE, " +
                             "FIRST_PLACE_COUNT, SECOND_PLACE_COUNT, THIRD_PLACE_COUNT, FOURTH_PLACE_COUNT, " +
                             "STRIKE_LENGTH, LAST_STRIKE_MATCH_ID, created_at) " +
                             "values (10000, 10000, '2010-10-2',0,0,0,0,0,0,0, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, RATING_DATE, EFFICIENCY, WIN_RATE, " +
                             "FIRST_PLACE_COUNT, SECOND_PLACE_COUNT, THIRD_PLACE_COUNT, FOURTH_PLACE_COUNT, " +
                             "STRIKE_LENGTH, LAST_STRIKE_MATCH_ID, created_at) " +
                             "values (10001, 10000, '2010-10-3',0,0,0,0,0,0,0, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, RATING_DATE, EFFICIENCY, WIN_RATE, " +
                             "FIRST_PLACE_COUNT, SECOND_PLACE_COUNT, THIRD_PLACE_COUNT, FOURTH_PLACE_COUNT, " +
                             "STRIKE_LENGTH, LAST_STRIKE_MATCH_ID, created_at) " +
                             "values (10002, 10001, '2010-10-1',0,0,0,0,0,0,0, 10000,'2010-10-10')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, RATING_DATE, EFFICIENCY, WIN_RATE, " +
                             "FIRST_PLACE_COUNT, SECOND_PLACE_COUNT, THIRD_PLACE_COUNT, FOURTH_PLACE_COUNT, " +
                             "STRIKE_LENGTH, LAST_STRIKE_MATCH_ID, created_at) " +
                             "values (10003, 10001, '2010-10-2',0,0,0,0,0,0,0, 10000,'2010-10-10')");

        List<RatingDate> latestRatings = dateRepository.findLatestRatings(PlayerRating.class);

        assertThat(latestRatings)
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        new RatingDate(10000L, LocalDate.of(2010, 10, 3)),
                        new RatingDate(10001L, LocalDate.of(2010, 10, 2))
                );
    }

    private static Stream<Arguments> classSource() {
        return Stream.of(Arguments.of(PlayerRating.class), Arguments.of(LeaderRating.class));
    }
}
