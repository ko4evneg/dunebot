package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.TaskScheduler;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.telegram.ChatType;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class ResubmitCommandProcessorTest extends TestContextMock {
    private static final long CHAT_ID = 12000L;
    private static final long USER_ID = 11000L;
    private static final Instant NOW = LocalDate.of(2010, 10, 10).atTime(15, 0, 0)
            .toInstant(ZoneOffset.UTC);
    private static final String RESUBMIT_LIMIT_EXCEED_FINISH_MESSAGE = "Матч 15000 завершен без результата, так как превышено максимальное количество попыток регистрации мест (3)";
    private final CommandMessage resubmitCommandMessage = getCommandMessage(USER_ID);

    @Autowired
    private ResubmitCommandProcessor resubmitProcessor;
    @MockBean
    private SubmitCommandProcessor submitProcessor;
    @MockBean
    private Clock clock;
    @MockBean
    private TaskScheduler taskScheduler;
    @MockBean
    private MatchFinishingService finishingService;

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(), any(Instant.class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, " + USER_ID + ", " + CHAT_ID + ", 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10001, 11001, 12001, 'st_pl2', 'name2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10002, 11002, 12002, 'st_pl3', 'name3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10003, 11003, 12003, 'st_pl4', 'name4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, created_at) " +
                "values (10000, 'ExternalPollId', 10000, " + CHAT_ID + ", '10000', '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10001, 'ExternalMessageId', 10000, " + CHAT_ID + ", '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, positive_answers_count, submits_retry_count, created_at) " +
                "values (15000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', 4, 1, '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10002, 'ExternalMessageId', 10012, 10022, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10003, 'ExternalMessageId', 10013, 10023, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10004, 'ExternalMessageId', 10014, 10024, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10005, 'ExternalMessageId', 10015, 10025, '2020-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                "values (10000, 15000, 10000, 10002, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, candidate_place, created_at) " +
                "values (10001, 15000, 10001, 10003, 1, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, candidate_place, created_at) " +
                "values (10002, 15000, 10002, 10004, 2, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                "values (10003, 15000, 10003, 10005, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where chat_id between 12000 and 12006 or id between 10000 and 10005");
    }

    @ParameterizedTest
    @MethodSource("exceptionsSource")
    void shouldThrowOnUnsuitableMatchResubmit(String query, String expectedException) {
        jdbcTemplate.execute(query);

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> resubmitProcessor.process(resubmitCommandMessage));

        assertEquals(expectedException, actualException.getMessage());
    }

    private static Stream<Arguments> exceptionsSource() {
        return Stream.of(
                Arguments.of("update matches set is_finished = true where id = 15000", "Запрещено регистрировать результаты завершенных матчей"),
                Arguments.of("update matches set is_onsubmit = true where id = 15000", "Запрос на публикацию этого матча уже сделан"),
                Arguments.of("update matches set positive_answers_count = 3 where id = 15000", "В опросе участвует меньше игроков чем нужно для матча. Все игроки должны войти в опрос")
        );
    }

    @Test
    void shouldThrowOnAlienMatchResubmit() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10004, 11004, 12004, 'st_pl5', 'name5', '2010-10-10') ");
        CommandMessage commandMessage = getCommandMessage(11004L);

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> resubmitProcessor.process(commandMessage));

        assertEquals("Вы не можете инициировать публикацию этого матча", actualException.getMessage());
    }

    @Test
    void shouldThrowOnNotExistentMatchSubmit() {
        jdbcTemplate.execute("delete from match_players where match_id = 15000");
        jdbcTemplate.execute("delete from matches where id = 15000");

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> resubmitProcessor.process(resubmitCommandMessage));

        assertEquals("Матча с таким ID не существует!", actualException.getMessage());
    }

    @Test
    void shouldIncreaseMatchResubmitCounterOnResubmit() {
        resubmitProcessor.process(resubmitCommandMessage);

        Integer actualResubmits = jdbcTemplate.queryForObject("select submits_retry_count from matches where id = 15000", Integer.class);

        assertEquals(2, actualResubmits);
    }

    @Test
    void shouldInvokeSubmitProcessorOnResubmit() {
        resubmitProcessor.process(resubmitCommandMessage);

        verify(submitProcessor, times(1))
                .process(argThat((Match match) -> {
                    List<Long> matchPlayerIds = match.getMatchPlayers().stream().map(matchPlayer -> matchPlayer.getPlayer().getId()).sorted().toList();
                    return match.getId().equals(15000L) && matchPlayerIds.equals(List.of(10000L, 10001L, 10002L, 10003L));
                }));
    }

    @Test
    void shouldResetMatchSubmitsCountOnResubmit() {
        jdbcTemplate.execute("update matches set submits_count = 4 where id = 15000");

        resubmitProcessor.process(resubmitCommandMessage);

        Integer actualResubmits = jdbcTemplate.queryForObject("select submits_count from matches where id = 15000", Integer.class);

        assertEquals(0, actualResubmits);
    }

    @Test
    void shouldResetMatchPlayersCandidatePlaceOnResubmit() {
        resubmitProcessor.process(resubmitCommandMessage);

        Boolean isAnyExternalMessageIdExist = jdbcTemplate.queryForObject("select exists" +
                "(select 1 from match_players where match_id = 15000 and candidate_place is not null)", Boolean.class);

        assertNotNull(isAnyExternalMessageIdExist);
        assertFalse(isAnyExternalMessageIdExist);
    }

    @Test
    void shouldDeleteOldMatchPlayersExternalMessagesOnResubmit() {
        resubmitProcessor.process(resubmitCommandMessage);

        Boolean isAnyExternalMessageExist = jdbcTemplate.queryForObject(
                "select exists(select 1 from external_messages where id between 10002 and 10005)", Boolean.class);

        assertNotNull(isAnyExternalMessageExist);
        assertFalse(isAnyExternalMessageExist);
    }

    // TODO:  finish match in service, check call invocation when resubmit limit reached
    @Test
    void shouldInvokeUnsuccessfulSubmitMatchFinishOnResubmitExceedingMessage() {
        jdbcTemplate.execute("update matches set positive_answers_count = 4, submits_retry_count = 3 where id = 15000");

        resubmitProcessor.process(resubmitCommandMessage);

        verify(finishingService, times(1)).finishUnsuccessfullySubmittedMatch(eq(15000L), eq(RESUBMIT_LIMIT_EXCEED_FINISH_MESSAGE));
    }

    private CommandMessage getCommandMessage(long userId) {
        User user = new User();
        user.setId(userId);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(100_500);
        message.setFrom(user);
        message.setChat(chat);
        message.setText("/" + Command.SUBMIT.name() + " 15000");
        return new CommandMessage(message);
    }
}