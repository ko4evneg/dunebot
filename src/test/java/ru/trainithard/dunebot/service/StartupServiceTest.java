package ru.trainithard.dunebot.service;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class StartupServiceTest extends TestContextMock {

    @Autowired
    private StartupService startupService;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, 11000, 12000, 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, 11001, 12001, 'st_pl2', 'name2', 'l2', 'e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10002, 11002, 12002, 'st_pl3', 'name3', 'l3', 'e3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10003, 11003, 12003, 'st_pl4', 'name4', 'l4', 'e4', '2010-10-10') ");

        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                             "values (10000, 'ExternalPollId', 10000, 10001, 10002, 10003, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, owner_id, mod_type, state, finish_date, created_at) " +
                             "values (10000, 10000, 10000, '" + ModType.UPRISING_4 + "', '" + MatchState.FINISHED + "', '2010-10-01', '2010-10-10') ");

        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10000, 10000, 10000, 4, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10001, 10000, 10001, 2, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10002, 10000, 10002, 3, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10003, 10000, 10003, 1, '2010-10-10')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) values (10000, 'CHAT_ID', '100500', '2010-01-02')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from settings where id = 10000");
        jdbcTemplate.execute("delete from match_players where match_id between 10000 and 10001");
        jdbcTemplate.execute("delete from matches where id between 10000 and 10001");
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10001");
        jdbcTemplate.execute("delete from players where id between 10000 and 10003");
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.INCLUDE, names = {"FINISHED", "FAILED"})
    void shouldNotDeleteEndedMatchesOnStartup(MatchState state) {
        jdbcTemplate.execute("update matches set state = '" + state + "' where id = 10000");

        startupService.startUp();

        Boolean isMatchExist = jdbcTemplate.queryForObject("select exists(select 1 from matches where id = 10000)", Boolean.class);

        assertNotNull(isMatchExist);
        assertTrue(isMatchExist);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class)
    void shouldNotDeleteMatchPlayersOfEndedMatchesOnStartup(MatchState state) {
        jdbcTemplate.execute("update matches set state = '" + state + "' where id = 10000");

        startupService.startUp();

        Boolean isMatchExist = jdbcTemplate.queryForObject("select exists(select 1 from match_players where match_id = 10000)", Boolean.class);

        assertNotNull(isMatchExist);
        assertTrue(isMatchExist);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"FINISHED", "FAILED"})
    void shouldFailNotEndedMatchesOnStartup(MatchState state) {
        jdbcTemplate.execute("update matches set state = '" + state + "' where id = 10000");

        startupService.startUp();

        MatchState actualState = jdbcTemplate.queryForObject("select state from matches where id = 10000", MatchState.class);

        assertEquals(MatchState.FAILED, actualState);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"FINISHED", "FAILED"})
    void shouldSendMessageOnNotEndedMatchFail(MatchState state) {
        jdbcTemplate.execute("update matches set state = '" + state + "' where id = 10000");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                             "values (10001, 'ExternalPollId', 10500, 10501, 10502, 10503, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, owner_id, mod_type, state, created_at) " +
                             "values (10001, 10001, 10000, '" + ModType.CLASSIC + "', '" + state + "', '2010-10-10') ");

        startupService.startUp();

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(2)).sendMessageAsync(messageCaptor.capture());
        List<MessageDto> actualMessage = messageCaptor.getAllValues();

        MatcherAssert.assertThat(actualMessage, containsInAnyOrder(
                allOf(
                        hasProperty("chatId", is("100500")),
                        hasProperty("topicId", is(10002)),
                        hasProperty("text", is("Бот был перезапущен, незавершенные матчи \\(10000\\) завершены без регистрации результатов"))
                ), allOf(
                        hasProperty("chatId", is("100500")),
                        hasProperty("topicId", is(10502)),
                        hasProperty("text", is("Бот был перезапущен, незавершенные матчи \\(10001\\) завершены без регистрации результатов"))
                )
        ));
    }
}
