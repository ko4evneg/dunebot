package ru.trainithard.dunebot.service.telegram.command.processor.submit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class SubmitCommandProcessorTest extends TestContextMock {
    private static final Long CHAT_ID = 12000L;
    private static final long USER_ID_1 = 11000L;
    private static final long USER_ID_2 = 11001L;
    private static final int FINISH_MATCH_TIMEOUT = 120;
    private static final Instant NOW = LocalDate.of(2010, 10, 10).atTime(15, 0, 0)
            .toInstant(ZoneOffset.UTC);
    private final CommandMessage submitCommandMessage = getCommandMessage(USER_ID_1);

    @Autowired
    private SubmitCommandProcessor processor;
    @Autowired
    private MatchRepository matchRepository;
    @MockBean
    private Clock clock;
    @MockBean
    private DuneBotTaskScheduler taskScheduler;

    @BeforeEach
    void beforeEach() {
        doAnswer(new MockReplier()).when(messagingService).sendMessageAsync(any(MessageDto.class));
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture).when(taskScheduler).rescheduleSingleRunTask(any(), any(), any(Instant.class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + USER_ID_1 + ", " + CHAT_ID + ", 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, " + USER_ID_2 + ", 12001, 'st_pl2', 'name2', 'l2', 'e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10002, 11002, 12002, 'st_pl3', 'name3', 'l3', 'e3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10003, 11003, 12003, 'st_pl4', 'name4', 'l4', 'e4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, created_at) " +
                             "values (10000, 'ExternalPollId', 10000, " + CHAT_ID + ", '10000', '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 10000, " + CHAT_ID + ", '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, state, positive_answers_count, submits_retry_count, created_at) " +
                             "values (15000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', '" + MatchState.NEW + "', 4, 0, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10000, 15000, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10001, 15000, 10001, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10002, 15000, 10002, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 15000, 10003, '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10200, 'la leader 1', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10201, 'la leader 2', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10202, 'la leader 3', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10203, 'la leader 4', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) " +
                             "values (10000, '" + AppSettingKey.SUBMIT_TIMEOUT + "', 120, '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) " +
                             "values (10001, '" + AppSettingKey.SUBMIT_TIMEOUT_WARNING_NOTIFICATION + "', 9, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from app_settings where id between 10000 and 10001");
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from leaders where id between 10200 and 10203");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where chat_id between 12000 and 12006");
    }

    @ParameterizedTest
    @MethodSource("exceptionsSource")
    void shouldThrowOnUnsuitableMatchSubmit(String query, String expectedException) {
        jdbcTemplate.execute(query);

        assertThatThrownBy(() -> processor.process(submitCommandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(expectedException);
    }

    private static Stream<Arguments> exceptionsSource() {
        return Stream.of(
                Arguments.of("update matches set state = '" + MatchState.FAILED + "' where id = 15000", "Запрещено регистрировать результаты завершенных матчей"),
                Arguments.of("update matches set state = '" + MatchState.FINISHED + "' where id = 15000", "Запрещено регистрировать результаты завершенных матчей"),
                Arguments.of("update matches set state = '" + MatchState.ON_SUBMIT + "' where id = 15000", "Запрос на публикацию этого матча уже сделан"),
                Arguments.of("update matches set state = '" + MatchState.SUBMITTED + "' where id = 15000", "Результаты матча уже зарегистрированы. При ошибке в результатах, используйте команду '/resubmit 15000'"),
                Arguments.of("update matches set positive_answers_count = 3 where id = 15000", "В опросе участвует меньше игроков чем нужно для матча. Все игроки должны войти в опрос")
        );
    }

    @Test
    void shouldThrowOnAlienMatchSubmit() {
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

        assertThatThrownBy(() -> processor.process(submitCommandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Матча с таким ID не существует!");
    }

    @Test
    void shouldSendMessageToSubmitInitiator() {
        processor.process(submitCommandMessage);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();

        assertThat(actualMessageDto.getChatId()).isEqualTo(CHAT_ID.toString());
    }

    @Test
    void shouldSendCorrectSubmitMessage() {
        processor.process(submitCommandMessage);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();
        List<List<ButtonDto>> linedButtons = actualMessageDto.getKeyboard();

        assertThat(actualMessageDto.getText()).isEqualTo("""
                Регистрация результатов для *матча 15000*\\. \
                Нажмите по очереди кнопки с именами участвовавших игроков, \
                начиная от победителя и заканчивая последним местом\\.""");
        assertThat(linedButtons)
                .flatExtracting(buttonDtos -> buttonDtos)
                .extracting(ButtonDto::getText, ButtonDto::getCallback)
                .containsExactlyInAnyOrder(
                        tuple("name1 (st_pl1) l1", "15000_SP_10000"),
                        tuple("name2 (st_pl2) l2", "15000_SP_10001"),
                        tuple("name3 (st_pl3) l3", "15000_SP_10002"),
                        tuple("name4 (st_pl4) l4", "15000_SP_10003")
                );
    }

    @Test
    void shouldSendCorrectSubmitMessageWhenMatchHasMorePlayersThanAllowed() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10004, 11004, 12004, 'st_pl5', 'name5', 'l5', 'e5', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10004, 15000, 10004, '2010-10-10')");

        processor.process(submitCommandMessage);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();
        List<List<ButtonDto>> linedButtons = actualMessageDto.getKeyboard();

        assertThat(actualMessageDto.getText()).isEqualTo("""
                Регистрация результатов для *матча 15000*\\. \
                Нажмите по очереди кнопки с именами участвовавших игроков, \
                начиная от победителя и заканчивая последним местом\\.""");
        assertThat(linedButtons)
                .flatExtracting(buttonDtos -> buttonDtos)
                .extracting(ButtonDto::getText, ButtonDto::getCallback)
                .containsExactlyInAnyOrder(
                        tuple("name1 (st_pl1) l1", "15000_SP_10000"),
                        tuple("name2 (st_pl2) l2", "15000_SP_10001"),
                        tuple("name3 (st_pl3) l3", "15000_SP_10002"),
                        tuple("name4 (st_pl4) l4", "15000_SP_10003"),
                        tuple("name5 (st_pl5) l5", "15000_SP_10004")
                );
    }

    @Test
    void shouldSetSubmitMatchState() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(111001);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        Message message = new Message();
        message.setMessageId(111000);
        message.setReplyToMessage(replyMessage);
        message.setChat(chat);

        doReturn(CompletableFuture.completedFuture(new ExternalMessageDto(message))).when(messagingService).sendMessageAsync(any(MessageDto.class));

        processor.process(submitCommandMessage);

        MatchState actualState = jdbcTemplate.queryForObject("select state from matches where id = 15000", MatchState.class);

        assertThat(actualState).isNotNull().isEqualTo(MatchState.ON_SUBMIT);
    }

    @Test
    void shouldResetMatchSubmitDataOnResubmit() {
        jdbcTemplate.execute("update matches set state ='" + MatchState.SUBMITTED + "' where id = 15000");
        Match match = matchRepository.findWithMatchPlayersBy(15000L).orElseThrow();

        processor.process(match, USER_ID_2);

        Match actualMatch = jdbcTemplate.queryForObject("select submits_retry_count, state from matches where id = 15000",
                new BeanPropertyRowMapper<>(Match.class));

        assertThat(actualMatch)
                .extracting(Match::getState, Match::getSubmitsRetryCount)
                .containsExactly(MatchState.ON_SUBMIT, 1);
    }

    @Test
    void shouldResetMatchPlayersPlacesOnResubmit() {
        jdbcTemplate.execute("update matches set state ='" + MatchState.SUBMITTED + "' where id = 15000");
        jdbcTemplate.execute("update match_players set place = 1, leader = 10200 where id = 10000");
        jdbcTemplate.execute("update match_players set place = 2, leader = 10201 where id = 10001");
        jdbcTemplate.execute("update match_players set place = 3, leader = 10202 where id = 10002");
        jdbcTemplate.execute("update match_players set place = 4, leader = 10203 where id = 10003");
        Match match = matchRepository.findWithMatchPlayersBy(15000L).orElseThrow();

        processor.process(match, USER_ID_2);

        List<Integer> actualPlaces = jdbcTemplate
                .queryForList("select place from match_players where id between 10000 and 10003 order by id", Integer.class);

        assertThat(actualPlaces).isNotEmpty().allMatch(Objects::isNull);
    }

    @Test
    void shouldResetMatchLeadersPlacesOnResubmit() {
        jdbcTemplate.execute("update matches set state ='" + MatchState.SUBMITTED + "' where id = 15000");
        jdbcTemplate.execute("update match_players set place = 1, leader = 10200 where id = 10000");
        jdbcTemplate.execute("update match_players set place = 2, leader = 10201 where id = 10001");
        jdbcTemplate.execute("update match_players set place = 3, leader = 10202 where id = 10002");
        jdbcTemplate.execute("update match_players set place = 4, leader = 10203 where id = 10003");
        Match match = matchRepository.findWithMatchPlayersBy(15000L).orElseThrow();

        processor.process(match, USER_ID_2);

        List<Integer> actualLeaders = jdbcTemplate
                .queryForList("select leader from match_players where id between 10000 and 10003 order by id", Integer.class);

        assertThat(actualLeaders).isNotEmpty().allMatch(Objects::isNull);
    }

    @Test
    void shouldScheduleUnsuccessfullySubmittedMatchFinishTaskOnFirstSubmit() {
        processor.process(submitCommandMessage);

        DuneBotTaskId expectedTaskId = new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT, 15000L);
        Instant expectedInstant = NOW.plus(FINISH_MATCH_TIMEOUT, ChronoUnit.MINUTES);
        verify(taskScheduler).rescheduleSingleRunTask(any(), eq(expectedTaskId), eq(expectedInstant));
    }

    @ParameterizedTest
    @EnumSource(value = DuneTaskType.class, mode = EnumSource.Mode.INCLUDE, names = {"SUBMIT_TIMEOUT", "SUBMIT_TIMEOUT_NOTIFICATION"})
    void shouldRescheduleUnsuccessfullySubmittedMatchFinishTaskOnResubmit(DuneTaskType taskType) {
        jdbcTemplate.execute("update matches set submits_retry_count = 2 where id = 15000");
        DuneBotTaskId taskId = new DuneBotTaskId(taskType, 15000L);
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).get(taskId);
        doReturn(208L).when(future).getDelay(any());

        processor.process(submitCommandMessage);

        Instant expectedInstant = NOW.plus(208 + 420, ChronoUnit.SECONDS);
        verify(taskScheduler).rescheduleSingleRunTask(any(), eq(taskId), eq(expectedInstant));
    }

    @Test
    void shouldReturnSubmitCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.SUBMIT);
    }

    private CommandMessage getCommandMessage(long userId) {
        User user = new User();
        user.setId(userId);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(10000);
        message.setFrom(user);
        message.setChat(chat);
        message.setText("/" + Command.SUBMIT.name() + " 15000");
        return CommandMessage.getMessageInstance(message);
    }

    private static class MockReplier implements Answer<CompletableFuture<ExternalMessageDto>> {
        private int externalId = 11000;
        private long chatId = CHAT_ID;

        @Override
        public CompletableFuture<ExternalMessageDto> answer(InvocationOnMock invocationOnMock) {
            Chat chat = new Chat();
            chat.setId(chatId++);
            Message message = new Message();
            message.setMessageId(externalId++);
            message.setChat(chat);
            return CompletableFuture.completedFuture(new ExternalMessageDto(message));
        }
    }
}