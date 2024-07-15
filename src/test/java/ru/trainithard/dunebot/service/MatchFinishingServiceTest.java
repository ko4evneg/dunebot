package ru.trainithard.dunebot.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class MatchFinishingServiceTest extends TestContextMock {
    private static final String MATCH_CHAT_ID = "12345";
    private static final int MATCH_TOPIC_REPLY_ID = 9000;
    private static final LocalDate FINISH_DATE = LocalDate.of(2012, 12, 12);
    private final ExternalMessage externalMessage = new ExternalMessage("finishtxt");

    @Autowired
    private MatchFinishingService finishingService;
    @MockBean
    private Clock clock;
    @SpyBean
    private MatchPlayerRepository matchPlayerRepository;
    @SpyBean
    private TransactionTemplate transactionTemplate;
    @SpyBean
    private ExternalMessageFactory messageFactory;

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(FINISH_DATE.atTime(LocalTime.of(1, 0)).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        doReturn(externalMessage).when(messageFactory).getMatchSuccessfulFinishMessage(argThat(match -> match.getId() == 15000L));

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
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, state, positive_answers_count, created_at) " +
                             "values (15000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', '" + MatchState.ON_SUBMIT + "', 3, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10000, 15000, 10000, 4, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10001, 15000, 10001, 2, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10002, 15000, 10002, 3, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10003, 15000, 10003, 1, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id in (15000)");
        jdbcTemplate.execute("delete from matches where id in (15000)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10001");
    }

    @Test
    void shouldSetMatchFinishedStateOnSubmittedMatchFinish() {
        jdbcTemplate.execute("update matches set state = '" + MatchState.SUBMITTED + "' where id = 15000");

        finishingService.finishCompletelySubmittedMatch(15000L);

        MatchState actualState = jdbcTemplate.queryForObject("select state from matches where id = 15000", MatchState.class);

        assertThat(actualState).isNotNull().isEqualTo(MatchState.FINISHED);
    }

    @Test
    void shouldSetMatchFinishDateOnSubmittedMatchFinish() {
        jdbcTemplate.execute("update matches set state = '" + MatchState.SUBMITTED + "' where id = 15000");

        finishingService.finishCompletelySubmittedMatch(15000L);

        LocalDate actualDate = jdbcTemplate.queryForObject("select finish_date from matches where id = 15000", LocalDate.class);

        assertThat(actualDate).isNotNull().isEqualTo(FINISH_DATE);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"SUBMITTED"})
    void shouldDoNothingOnWrongStateMatchCompletedSubmitFinish(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "', submitter_id = 10002 where id = 15000");

        finishingService.finishCompletelySubmittedMatch(15000L);

        verifyNoInteractions(matchPlayerRepository);
        verify(transactionTemplate, never()).executeWithoutResult(any());
        verifyNoInteractions(messagingService);
    }

    @Test
    void shouldSendNotificationOnSubmittedMatchFinish() {
        jdbcTemplate.execute("update matches set state = '" + MatchState.SUBMITTED + "' where id = 15000");

        finishingService.finishCompletelySubmittedMatch(15000L);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertThat(messageDto)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getText)
                .containsExactly(MATCH_CHAT_ID, MATCH_TOPIC_REPLY_ID, "finishtxt");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldResetMatchPlayersDataOnPartiallySubmitFinish(boolean isFailedByResubmitLimit) {
        jdbcTemplate.execute("update matches set state = '" + MatchState.ON_SUBMIT + "', submitter_id = 10002 where id = 15000");

        finishingService.finishPartiallySubmittedMatch(15000, isFailedByResubmitLimit);

        Boolean hasSubmitData = jdbcTemplate
                .queryForObject("select exists(select 1 from match_players where id between 10000 and 10003 and " +
                                "(leader is not null or place is not null))", Boolean.class);

        assertThat(hasSubmitData).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldSetMatchFinishDateOnPartiallySubmitFinish(boolean isFailedByResubmitLimit) {
        jdbcTemplate.execute("update matches set state = '" + MatchState.ON_SUBMIT + "', submitter_id = 10002 where id = 15000");

        finishingService.finishPartiallySubmittedMatch(15000L, isFailedByResubmitLimit);

        LocalDate actualDate = jdbcTemplate.queryForObject("select finish_date from matches where id = 15000", LocalDate.class);

        assertThat(actualDate).isNotNull().isEqualTo(FINISH_DATE);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldSetMatchFailedStateOnPartiallySubmitFinish(boolean isFailedByResubmitLimit) {
        jdbcTemplate.execute("update matches set state = '" + MatchState.ON_SUBMIT + "', submitter_id = 10002 where id = 15000");

        finishingService.finishPartiallySubmittedMatch(15000L, isFailedByResubmitLimit);

        MatchState actualState = jdbcTemplate.queryForObject("select state from matches where id = 15000", MatchState.class);

        assertThat(actualState).isEqualTo(MatchState.FAILED);
    }

    @Test
    void shouldSendNotificationWhenResubmitLimitExceededOnPartiallySubmitFinish() {
        jdbcTemplate.execute("update matches set state = '" + MatchState.ON_SUBMIT + "', submitter_id = 10002 where id = 15000");

        finishingService.finishPartiallySubmittedMatch(15000L, true);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertThat(messageDto)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getText)
                .containsExactly(MATCH_CHAT_ID, MATCH_TOPIC_REPLY_ID,
                        "*Матч 15000* завершен без результата, так как превышено максимальное количество попыток регистрации результатов\\.");
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"ON_SUBMIT"})
    void shouldDoNothingOnWrongStateMatchPartiallySubmitFinish(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "', submitter_id = 10002 where id = 15000");

        finishingService.finishPartiallySubmittedMatch(15000L, false);

        verifyNoInteractions(matchPlayerRepository);
        verifyNoInteractions(transactionTemplate);
        verifyNoInteractions(messagingService);
    }

    @Test
    void shouldSendNotificationWhenSubmitTimeoutOnPartiallySubmitFinish() {
        jdbcTemplate.execute("update matches set state = '" + MatchState.ON_SUBMIT + "', submitter_id = 10002 where id = 15000");

        finishingService.finishPartiallySubmittedMatch(15000L, false);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertThat(messageDto)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getText)
                .containsExactly(MATCH_CHAT_ID, MATCH_TOPIC_REPLY_ID,
                        "*Матч 15000* завершен без результата, так как игрок [@e3](tg://user?id=11002) не закончил регистрацию результата\\.");
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"ON_SUBMIT"})
    void shouldDoNothingOnWrongStateWhenSubmitTimeoutOnPartiallySubmitFinish(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "', submitter_id = 10002 where id = 15000");

        finishingService.finishPartiallySubmittedMatch(15000L, true);

        verifyNoInteractions(matchPlayerRepository);
        verifyNoInteractions(transactionTemplate);
        verifyNoInteractions(messagingService);
    }
}
