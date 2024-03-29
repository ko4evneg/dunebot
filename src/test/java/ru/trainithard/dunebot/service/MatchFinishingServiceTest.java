package ru.trainithard.dunebot.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class MatchFinishingServiceTest extends TestContextMock {
    private static final String MATCH_CHAT_ID = "12345";
    private static final int MATCH_TOPIC_REPLY_ID = 9000;
    private static final ExternalMessage UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE = new ExternalMessage()
            .appendBold("Матч 15000")
            .append(" завершен без результата, так как превышено максимальное количество попыток регистрации мест");
    private static final LocalDate FINISH_DATE = LocalDate.of(2012, 12, 12);

    @Autowired
    private MatchFinishingService finishingService;
    @MockBean
    private Clock clock;

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(FINISH_DATE.atTime(LocalTime.of(1, 0)).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                "values (10000, 11000, 12000, 'st_pl1', 'f1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                "values (10001, 11001, 12001, 'st_pl2', 'f2', 'l2', 'e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                "values (10002, 11002, 12002, 'st_pl3', 'f3', 'l3', 'e3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                "values (10003, 11003, 12003, 'st_pl4', 'f4', 'l4', 'e4', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                "values (10004, 11004, 12004, 'st_pl5', 'f5', 'l5', 'e5', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, reply_id, created_at) " +
                "values (10000, 'ExternalPollId', 10000, " + MATCH_CHAT_ID + ", '10000', " + MATCH_TOPIC_REPLY_ID + ", '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10001, 'ExternalMessageId', 10001, 10000, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, state, positive_answers_count, has_onsubmit_photo, created_at) " +
                "values (15000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', '" + MatchState.ON_SUBMIT + "', 3, true, '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10002, 'ExternalMessageId', 10002, 11002, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10003, 'ExternalMessageId', 10003, 11003, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10004, 'ExternalMessageId', 10004, 11004, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10005, 'ExternalMessageId', 10005, 11005, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10006, 'ExternalMessageId', 10006, 11006, '2020-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, candidate_place, created_at) " +
                "values (10000, 15000, 10000, 10002, null, '2010-10-10')");
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
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10006");
    }

    @Test
    void shouldPersistCandidatePlacesOnMatchFinish() {
        jdbcTemplate.execute("update matches set submits_count = 4 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 4 where id = 10000");

        finishingService.finishSubmittedMatch(15000L);

        List<Integer> playersPlaces = jdbcTemplate.queryForList("select place from match_players where match_id = 15000 order by id", Integer.class);

        assertThat(playersPlaces, contains(4, 2, 3, 1));
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.INCLUDE, names = {"FAILED", "FINISHED"})
    void shouldNotDoAnythingWithFinishedMatchOnUnsuccessfullySubmittedMatchFinish(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 15000");

        finishingService.finishNotSubmittedMatch(15000L, UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE);

        Boolean areMatchPlayersChanged = jdbcTemplate.queryForObject("select exists(select 1 from match_players where match_id = 15000 and place is not null)", Boolean.class);

        verify(messagingService, never()).sendMessageAsync(any());
        assertNotNull(areMatchPlayersChanged);
        assertFalse(areMatchPlayersChanged);
    }

    @Test
    void shouldNotPersistCandidatePlacesOnUnsuccessfullySubmittedMatchFinishWhenSubmitsAreMissing() {
        finishingService.finishNotSubmittedMatch(15000L, UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE);

        List<Integer> actualPersistedPlacesCount = jdbcTemplate
                .queryForList("select id from match_players where match_id = 15000 and place is not null", Integer.class);

        assertTrue(actualPersistedPlacesCount.isEmpty());
    }

    @Test
    void shouldPersistCandidatePlacesOnUnsuccessfullySubmittedMatchWhenOnlyNonParticipantSubmitsAreMissing() {
        jdbcTemplate.execute("update matches set submits_count = 4 where id = 15000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, candidate_place, created_at) " +
                "values (10004, 15000, 10004, 10006, 4, '2010-10-10')");

        finishingService.finishNotSubmittedMatch(15000L, UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE);

        List<Integer> playersPlaces = jdbcTemplate.queryForList("select place from match_players where match_id = 15000 order by id", Integer.class);

        assertThat(playersPlaces, contains(null, 2, 3, 1, 4));
    }

    @Test
    void shouldNotPersistCandidatePlacesOnUnsuccessfullySubmittedMatchWhenOnlyNonParticipantSubmitsAreMissingAndNoPhotoSubmitted() {
        jdbcTemplate.execute("update matches set submits_count = 4, has_onsubmit_photo = false where id = 15000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, candidate_place, created_at) " +
                "values (10004, 15000, 10004, 10006, 4, '2010-10-10')");

        finishingService.finishNotSubmittedMatch(15000L, UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE);

        Boolean wasAnyPlaceSaved = jdbcTemplate.queryForObject("select exists(select 1 from match_players where match_id = 15000 and place is not null)", Boolean.class);

        assertNotNull(wasAnyPlaceSaved);
        assertFalse(wasAnyPlaceSaved);
    }

    @Test
    void shouldSendNotificationOnUnsuccessfullySubmittedMatchFinish() {
        jdbcTemplate.execute("update matches set submits_count = 4 where id = 15000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, candidate_place, created_at) " +
                "values (10004, 15000, 10004, 10006, 4, '2010-10-10')");

        finishingService.finishNotSubmittedMatch(15000L, UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertEquals(MATCH_CHAT_ID, messageDto.getChatId());
        assertEquals(MATCH_TOPIC_REPLY_ID, messageDto.getTopicId());
        assertEquals("""
                *Матч 15000* завершился:
                                
                1️⃣ f4 \\(st\\_pl4\\) l4
                2️⃣ f2 \\(st\\_pl2\\) l2
                3️⃣ f3 \\(st\\_pl3\\) l3
                4️⃣ f5 \\(st\\_pl5\\) l5""", messageDto.getText());
    }

    @Test
    void shouldSetMatchFinishedStateOnMatchFinish() {
        jdbcTemplate.execute("update matches set submits_count = 4 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 4 where id = 10000");

        finishingService.finishSubmittedMatch(15000L);

        MatchState actualState = jdbcTemplate.queryForObject("select state from matches where id = 15000", MatchState.class);

        assertNotNull(actualState);
        assertEquals(MatchState.FINISHED, actualState);
    }

    @Test
    void shouldSetMatchFinishDateOnMatchFinish() {
        jdbcTemplate.execute("update matches set submits_count = 4 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 4 where id = 10000");

        finishingService.finishSubmittedMatch(15000L);

        LocalDate actualDate = jdbcTemplate.queryForObject("select finish_date from matches where id = 15000", LocalDate.class);

        assertNotNull(actualDate);
        assertEquals(FINISH_DATE, actualDate);
    }

    @Test
    void shouldSetMatchFailedStateOnUnsuccessfullySubmittedMatchFinish() {
        finishingService.finishNotSubmittedMatch(15000L, UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE);

        MatchState actualState = jdbcTemplate.queryForObject("select state from matches where id = 15000", MatchState.class);

        assertNotNull(actualState);
        assertEquals(MatchState.FAILED, actualState);
    }

    @Test
    void shouldSetMatchFinishDateOnUnsuccessfullySubmittedMatchFinish() {
        finishingService.finishNotSubmittedMatch(15000L, UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE);

        LocalDate actualDate = jdbcTemplate.queryForObject("select finish_date from matches where id = 15000", LocalDate.class);

        assertNotNull(actualDate);
        assertEquals(FINISH_DATE, actualDate);
    }

    @Test
    void shouldSendNotificationOnMatchFinish() {
        jdbcTemplate.execute("update matches set submits_count = 4 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 4 where id = 10000");

        finishingService.finishSubmittedMatch(15000L);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertEquals(MATCH_CHAT_ID, messageDto.getChatId());
        assertEquals(MATCH_TOPIC_REPLY_ID, messageDto.getTopicId());
        assertEquals("""
                *Матч 15000* завершился:
                                
                1️⃣ f4 \\(st\\_pl4\\) l4
                2️⃣ f2 \\(st\\_pl2\\) l2
                3️⃣ f3 \\(st\\_pl3\\) l3
                4️⃣ f1 \\(st\\_pl1\\) l1""", messageDto.getText());
    }

    @Test
    void shouldSendNotificationOnUnsuccessfullySubmittedMatchFinishWithoutResults() {
        finishingService.finishNotSubmittedMatch(15000L, UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertEquals(MATCH_CHAT_ID, messageDto.getChatId());
        assertEquals(MATCH_TOPIC_REPLY_ID, messageDto.getTopicId());
        assertEquals("*Матч 15000* завершен без результата, так как превышено максимальное количество попыток регистрации мест", messageDto.getText());
    }
}
