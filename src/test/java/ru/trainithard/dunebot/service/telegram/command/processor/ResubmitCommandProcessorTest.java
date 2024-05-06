package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.FileSystemUtils;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ResubmitCommandProcessorTest extends TestContextMock {
    private static final long CHAT_ID = 12000L;
    private static final long USER_ID = 11000L;
    private static final Instant NOW = LocalDate.of(2010, 10, 10).atTime(15, 0, 0)
            .toInstant(ZoneOffset.UTC);
    private final CommandMessage resubmitCommandMessage = getCommandMessage(USER_ID);

    @Autowired
    private ResubmitCommandProcessor processor;
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

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + USER_ID + ", " + CHAT_ID + ", 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, 11001, 12001, 'st_pl2', 'name2', 'l2', 'e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10002, 11002, 12002, 'st_pl3', 'name3', 'l3', 'e3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10003, 11003, 12003, 'st_pl4', 'name4', 'l4', 'e4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, created_at) " +
                             "values (10000, 'ExternalPollId', 10000, " + CHAT_ID + ", '10000', '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 10000, " + CHAT_ID + ", '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, state, positive_answers_count, submits_retry_count, created_at) " +
                             "values (15000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', '" + MatchState.ON_SUBMIT + "', 4, 1, '2010-10-10') ");
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
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) " +
                             "values (10000, '" + SettingKey.RESUBMITS_LIMIT + "', 3, '2010-10-10')");
    }

    @AfterEach
    void afterEach() throws IOException {
        jdbcTemplate.execute("delete from settings where id = 10000");
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where chat_id between 12000 and 12006 or id between 10000 and 10005");
        jdbcTemplate.execute("delete from leaders where id = 10000");
        FileSystemUtils.deleteRecursively(Path.of("photos"));
    }

    @ParameterizedTest
    @MethodSource("exceptionsSource")
    void shouldThrowOnUnsuitableMatchResubmit(String query, String expectedException) {
        jdbcTemplate.execute(query);

        assertThatThrownBy(() -> processor.process(resubmitCommandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(expectedException);
    }

    private static Stream<Arguments> exceptionsSource() {
        return Stream.of(
                Arguments.of("update matches set state = '" + MatchState.FAILED + "' where id = 15000", "Запрещено регистрировать результаты завершенных матчей"),
                Arguments.of("update matches set state = '" + MatchState.FINISHED + "' where id = 15000", "Запрещено регистрировать результаты завершенных матчей"),
                Arguments.of("update matches set positive_answers_count = 3 where id = 15000", "В опросе участвует меньше игроков чем нужно для матча. Все игроки должны войти в опрос")
        );
    }

    @Test
    void shouldThrowOnAlienMatchResubmit() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10004, 11004, 12004, 'st_pl5', 'name5', 'l5', 'e5', '2010-10-10') ");
        CommandMessage commandMessage = getCommandMessage(11004L);

        assertThatThrownBy(() -> processor.process(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Вы не можете инициировать публикацию этого матча");
    }

    @Test
    void shouldThrowOnNotExistentMatchSubmit() {
        jdbcTemplate.execute("delete from match_players where match_id = 15000");
        jdbcTemplate.execute("delete from matches where id = 15000");

        assertThatThrownBy(() -> processor.process(resubmitCommandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Матча с таким ID не существует!");
    }

    @Test
    void shouldIncreaseMatchResubmitCounterOnResubmit() {
        processor.process(resubmitCommandMessage);

        Integer actualResubmits = jdbcTemplate.queryForObject("select submits_retry_count from matches where id = 15000", Integer.class);

        assertThat(actualResubmits).isEqualTo(2);
    }

    @Test
    void shouldInvokeSubmitProcessorOnResubmit() {
        processor.process(resubmitCommandMessage);

        verify(submitProcessor, times(1))
                .process(argThat((Match match) -> {
                    List<Long> matchPlayerIds = match.getMatchPlayers().stream().map(matchPlayer -> matchPlayer.getPlayer().getId()).sorted().toList();
                    return match.getId().equals(15000L) && matchPlayerIds.equals(List.of(10000L, 10001L, 10002L, 10003L));
                }));
    }

    @Test
    void shouldResetMatchSubmitsCountOnResubmit() {
        jdbcTemplate.execute("update matches set submits_count = 4 where id = 15000");

        processor.process(resubmitCommandMessage);

        Integer actualResubmits = jdbcTemplate.queryForObject("select submits_count from matches where id = 15000", Integer.class);

        assertThat(actualResubmits).isZero();
    }

    @Test
    void shouldResetMatchStateOnResubmitWhenScreenshotWasUploaded() {
        jdbcTemplate.execute("update matches set submits_count = 4, screenshot_path = 'photos/1.jpg', state = '" + MatchState.ON_SUBMIT_SCREENSHOTTED + "' where id = 15000");

        processor.process(resubmitCommandMessage);

        MatchState actualMatchState = jdbcTemplate.queryForObject("select state from matches where id = 15000", MatchState.class);

        assertThat(actualMatchState).isSameAs(MatchState.ON_SUBMIT);
    }

    @Test
    void shouldResetMatchScreenshotPathOnResubmit() {
        jdbcTemplate.execute("update matches set submits_count = 4, screenshot_path = 'photos/1.jpg' where id = 15000");

        processor.process(resubmitCommandMessage);

        Boolean isScreenshotResetted = jdbcTemplate.queryForObject(
                "select exists (select 1 from matches where id = 15000 and screenshot_path is null)", Boolean.class);

        assertThat(isScreenshotResetted).isNotNull().isTrue();
    }

    @Test
    void shouldDeleteScreenshotFileOnResubmit() throws IOException {
        jdbcTemplate.execute("update matches set submits_count = 4, screenshot_path = 'photos/1.jpg' where id = 15000");
        byte[] screenshotBytes = "abc" .getBytes();
        Files.createDirectories(Path.of("photos"));
        Files.write(Path.of("photos/1.jpg"), screenshotBytes);

        processor.process(resubmitCommandMessage);

        boolean doesFileExist = Files.exists(Path.of("photos/1.jpg"));

        assertThat(doesFileExist).isFalse();
    }

    @Test
    void shouldResetMatchPlayersCandidatePlaceOnResubmit() {
        processor.process(resubmitCommandMessage);

        Boolean isAnyExternalMessageIdExist = jdbcTemplate.queryForObject("select exists" +
                                                                          "(select 1 from match_players where match_id = 15000 and candidate_place is not null)", Boolean.class);

        assertThat(isAnyExternalMessageIdExist).isNotNull().isFalse();
    }

    @Test
    void shouldResetMatchPlayersLeadersOnResubmit() {
        processor.process(resubmitCommandMessage);

        Boolean isAnyExternalMessageIdExist = jdbcTemplate.queryForObject("select exists" +
                                                                          "(select 1 from match_players where match_id = 15000 and leader is not null)", Boolean.class);

        assertThat(isAnyExternalMessageIdExist).isNotNull().isFalse();
    }

    @Test
    void shouldDeleteOldMatchPlayersExternalMessagesOnResubmit() {
        processor.process(resubmitCommandMessage);

        Boolean isAnyExternalMessageExist = jdbcTemplate.queryForObject(
                "select exists(select 1 from external_messages where id between 10002 and 10005)", Boolean.class);

        assertThat(isAnyExternalMessageExist).isNotNull().isFalse();
    }

    @Test
    void shouldInvokeUnsuccessfulSubmitMatchFinishOnResubmitExceedingMessage() {
        jdbcTemplate.execute("update matches set positive_answers_count = 4, submits_retry_count = 3 where id = 15000");

        processor.process(resubmitCommandMessage);

        verify(finishingService).finishNotSubmittedMatch(eq(15000L), eq(true));
    }

    @Test
    void shouldNotInvokeSubmitOnResubmitExceedingMessage() {
        jdbcTemplate.execute("update matches set positive_answers_count = 4, submits_retry_count = 3 where id = 15000");

        processor.process(resubmitCommandMessage);

        verifyNoInteractions(submitProcessor);
    }

    @Test
    void shouldSendDeleteSubmitMessageWhenOldSubmitMessageExist() {
        processor.process(resubmitCommandMessage);

        ArgumentCaptor<ExternalMessageId> messageDtoCaptor = ArgumentCaptor.forClass(ExternalMessageId.class);
        verify(messagingService, times(4)).deleteMessageAsync(messageDtoCaptor.capture());
        List<ExternalMessageId> actualDeleteDto = messageDtoCaptor.getAllValues();

        assertThat(actualDeleteDto)
                .extracting(ExternalMessageId::getChatId, ExternalMessageId::getMessageId)
                .containsExactlyInAnyOrder(
                        tuple(10022L, 10012),
                        tuple(10023L, 10013),
                        tuple(10024L, 10014),
                        tuple(10025L, 10015));
    }

    @Test
    void shouldReturnResubmitCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.RESUBMIT);
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
        return CommandMessage.getMessageInstance(message);
    }
}
