package ru.trainithard.dunebot.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class MatchFinishingServiceTest extends TestContextMock {
    @Autowired
    private MatchFinishingService finishingService;
    @MockBean
    private MessagingService messagingService;

    private static final String MATCH_CHAT_ID = "12345";
    private static final int MATCH_TOPIC_REPLY_ID = 9000;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 11000, 12000, 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10001, 11001, 12001, 'st_pl2', 'name2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10002, 11002, 12002, 'st_pl3', 'name3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10003, 11003, 12003, 'st_pl4', 'name4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, reply_id, created_at) " +
                "values (10000, 'ExternalPollId', 10000, " + MATCH_CHAT_ID + ", '10000', " + MATCH_TOPIC_REPLY_ID + ", '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10001, 'ExternalMessageId', 10001, 10000, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, positive_answers_count, created_at) " +
                "values (15000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', 4, '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10002, 'ExternalMessageId', 10002, 11002, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10003, 'ExternalMessageId', 10003, 11003, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10004, 'ExternalMessageId', 10004, 11004, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10005, 'ExternalMessageId', 10005, 11005, '2020-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, candidate_place, created_at) " +
                "values (10000, 15000, 10000, 10002, 4, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, candidate_place, created_at) " +
                "values (10001, 15000, 10001, 10003, 2, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, candidate_place, created_at) " +
                "values (10002, 15000, 10002, 10004, 3, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, candidate_place, created_at) " +
                "values (10003, 15000, 10003, 10005, 1, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id in (15000)");
        jdbcTemplate.execute("delete from matches where id in (15000)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10005");
    }

    @Test
    void shouldFixCandidatePlacesOnMatchFinish() {
        finishingService.finishMatch(15000L);

        List<Integer> playersPlaces = jdbcTemplate.queryForList("select place from match_players where match_id = 15000 order by id", Integer.class);

        assertThat(playersPlaces, contains(4, 2, 3, 1));
    }

    @Test
    void shouldSetMatchFinishFlagOnMatchFinish() {
        finishingService.finishMatch(15000L);

        Boolean isMatchFinished = jdbcTemplate.queryForObject("select exists(select 1 from matches where id = 15000 and is_finished is true)", Boolean.class);

        assertNotNull(isMatchFinished);
        assertTrue(isMatchFinished);
    }

    @Test
    void shouldSendNotificationOnMatchFinish() {
        finishingService.finishMatch(15000L);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertEquals(MATCH_CHAT_ID, messageDto.getChatId());
        assertEquals(MATCH_TOPIC_REPLY_ID, messageDto.getReplyMessageId());
        assertEquals("""
                Матч 15000 завершился:
                1. st_pl4 (name4)
                2. st_pl2 (name2)
                3. st_pl3 (name3)
                4. st_pl1 (name1)""", messageDto.getText());
    }
}
