package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.trainithard.dunebot.configuration.SettingConstants.POSITIVE_POLL_OPTION_ID;

@SpringBootTest
class VoteCommandProcessorTest extends TestContextMock {

    @Autowired
    private VoteCommandProcessor commandProcessor;

    private static final String TELEGRAM_POLL_ID = "100500";
    private static final long TELEGRAM_USER_2_ID = 12346L;

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 12345, 12345, 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10001, " + TELEGRAM_USER_2_ID + ", 12346, 'st_pl2', 'name2', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_message_id, owner_id, mod_type, positive_answers_count, created_at) " +
                "values (10000, '" + TELEGRAM_POLL_ID + "', '123', 10000, '" + ModType.CLASSIC + "', 1, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10000, 10000, 10000, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id = 10000");
        jdbcTemplate.execute("delete from matches where id = 10000");
        jdbcTemplate.execute("delete from players where id between 10000 and 10003 or external_id = " + TELEGRAM_USER_2_ID);
    }

    @Test
    void shouldSaveNewMatchPlayerOnPositiveReplyRegistration() {
        commandProcessor.process(getCommandMessage(POSITIVE_POLL_OPTION_ID, TELEGRAM_USER_2_ID));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, containsInAnyOrder(10000L, 10001L));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotSaveNewMatchPlayerOnNonPositiveReplyRegistration(int optionId) {
        commandProcessor.process(getCommandMessage(optionId, TELEGRAM_USER_2_ID));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, containsInAnyOrder(10000L));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldDeleteMatchPlayerOnPositiveRegistrationRevocation(int optionId) {
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10001, 10000, 10001, '2010-10-10')");

        commandProcessor.process(getCommandMessage(optionId, TELEGRAM_USER_2_ID));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, contains(10000L));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotDeleteMatchRegisteredPlayerOnNonPositiveReplyRegistrationRevocation(int optionId) {
        commandProcessor.process(getCommandMessage(optionId, TELEGRAM_USER_2_ID));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, contains(10000L));
    }

    @Test
    void shouldIncreaseMatchRegisteredPlayersCountOnPositiveReplyRegistration() {
        commandProcessor.process(getCommandMessage(POSITIVE_POLL_OPTION_ID, TELEGRAM_USER_2_ID));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertEquals(2, actualPlayersCount);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotIncreaseMatchRegisteredPlayersCountOnNonPositiveReplyRegistration(int optionId) {
        commandProcessor.process(getCommandMessage(optionId, TELEGRAM_USER_2_ID));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @Test
    void shouldDecreaseMatchRegisteredPlayersCountOnPositiveReplyRegistrationRevocation() {
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10001, 10000, 10001, '2010-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 2 where id = 10000");

        commandProcessor.process(getCommandMessage(null, TELEGRAM_USER_2_ID));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotDecreaseMatchRegisteredPlayersCountOnNonPositiveReplyRegistrationRevocation(int optionId) {
        commandProcessor.process(getCommandMessage(optionId, TELEGRAM_USER_2_ID));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @Test
    @Disabled
    void shouldSendNotificationOnFourthPlayerRegistration() {
        fail();
    }

    private CommandMessage getCommandMessage(Integer optionId, long telegramUserid) {
        User user = new User();
        user.setId(telegramUserid);
        PollAnswer pollAnswer = new PollAnswer();
        pollAnswer.setUser(user);
        pollAnswer.setOptionIds(optionId == null ? Collections.emptyList() : Collections.singletonList(optionId));
        pollAnswer.setPollId(TELEGRAM_POLL_ID);
        return new CommandMessage(pollAnswer);
    }
}
