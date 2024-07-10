package ru.trainithard.dunebot.service.telegram.command.processor.submit;

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
import org.telegram.telegrambots.meta.api.objects.*;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.task.SubmitAcceptTimeoutTask;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@SpringBootTest
class LeaderAcceptCommandProcessorTest extends TestContextMock {
    private static final Instant NOW = LocalDate.of(2010, 10, 20).atTime(5, 0).toInstant(ZoneOffset.UTC);
    private static final Long CHAT_ID = 12000L;
    private static final Long USER_ID = 11000L;

    @Autowired
    private LeaderAcceptCommandProcessor processor;
    @MockBean
    private DuneBotTaskScheduler taskScheduler;
    @MockBean
    private Clock clock;

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + USER_ID + ", " + USER_ID + ", 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, 11001, 11001, 'st_pl2', 'name2', 'l2', 'e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10002, 11002, 11002, 'st_pl3', 'name3', 'l3', 'e3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10003, 11003, 11003, 'st_pl4', 'name4', 'l4', 'e4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, created_at) " +
                             "values (10000, 'ExternalPollId', 10000, " + CHAT_ID + ", '10000', '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 10000, " + CHAT_ID + ", '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, submitter_id, mod_type, state, positive_answers_count, created_at) " +
                             "values (15000, 10000, 10001, 10000, 10000, '" + ModType.CLASSIC + "', '" + MatchState.ON_SUBMIT + "', 4, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10100, 15000, 10000, 4, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10101, 15000, 10001, 3, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10102, 15000, 10002, 2, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10103, 15000, 10003, 1, '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10200, 'la leader 1', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10201, 'la leader 2', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10202, 'la leader 3', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10203, 'la leader 4', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) " +
                             "values (10000, '" + AppSettingKey.ACCEPT_SUBMIT_TIMEOUT + "', 13, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from app_settings where id = 10000");
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from leaders where id between 10200 and 10203");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where chat_id between 12000 and 12006");
    }

    @ParameterizedTest
    @ValueSource(longs = {10200, 10201, 10202, 10203})
    void shouldSaveFirstLeaderWhenNoLeadersSelectedYet(long leaderId) {
        processor.process(getCallbackMessage("15000_SL_" + leaderId));

        Long actualLeaderId = jdbcTemplate.queryForObject("select leader from match_players where id = 10103", Long.class);

        assertThat(actualLeaderId).isEqualTo(leaderId);
    }

    @Test
    void shouldSaveThirdPlaceLeaderWhenTwoLeadersSelected() {
        jdbcTemplate.execute("update match_players set leader = 10200 where id = 10103");
        jdbcTemplate.execute("update match_players set leader = 10201 where id = 10102");

        processor.process(getCallbackMessage("15000_SL_10203"));

        Long actualLeaderId = jdbcTemplate.queryForObject("select leader from match_players where id = 10101", Long.class);

        assertThat(actualLeaderId).isEqualTo(10203L);
    }

    @Test
    void shouldSendLeadersSubmitMessageOnLastLeaderSubmit() {
        jdbcTemplate.execute("update match_players set leader = 10201 where id = 10101");
        jdbcTemplate.execute("update match_players set leader = 10202 where id = 10102");
        jdbcTemplate.execute("update match_players set leader = 10203 where id = 10103");

        processor.process(getCallbackMessage("15000_SL_10200"));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(4)).sendMessageAsync(messageDtoCaptor.capture());
        List<MessageDto> actualMessageDtos = messageDtoCaptor.getAllValues();

        assertThat(actualMessageDtos)
                .filteredOn(messageDto -> messageDto.getChatId().equals("11000"))
                .map(MessageDto::getText)
                .containsExactly("""
                        Следующие результаты зарегистрированы для *матча 15000*:
                        1: name4 \\(st\\_pl4\\) l4 \\- la leader 4
                        2: name3 \\(st\\_pl3\\) l3 \\- la leader 3
                        3: name2 \\(st\\_pl2\\) l2 \\- la leader 2
                        4: name1 \\(st\\_pl1\\) l1 \\- la leader 1
                                        
                        В случае ошибки используйте команду '/resubmit 15000'""");
    }

    @Test
    void shouldSendSubmittedMatchMessageOnLastLeaderSubmit() {
        jdbcTemplate.execute("update match_players set leader = 10201 where id = 10101");
        jdbcTemplate.execute("update match_players set leader = 10202 where id = 10102");
        jdbcTemplate.execute("update match_players set leader = 10203 where id = 10103");

        processor.process(getCallbackMessage("15000_SL_10200"));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(4)).sendMessageAsync(messageDtoCaptor.capture());
        List<MessageDto> actualMessageDtos = messageDtoCaptor.getAllValues();

        assertThat(actualMessageDtos)
                .extracting(MessageDto::getChatId)
                .containsExactly("11000", "11001", "11002", "11003");
        assertThat(actualMessageDtos)
                .filteredOn(messageDto -> !messageDto.getChatId().equals("11000"))
                .extracting(MessageDto::getText)
                .allMatch(message -> message.equals("*Матч 15000* завершен\\!\n" +
                                                    "Ознакомьтесь с результатами \\- у вас есть 13 минута чтобы проверить их\\." +
                                                    " В случае ошибки, используйте команду '/resubmit 15000'"));
    }

    @Test
    void shouldSaveSubmittedStateOnLastLeaderSubmit() {
        jdbcTemplate.execute("update match_players set leader = 10201 where id = 10101");
        jdbcTemplate.execute("update match_players set leader = 10202 where id = 10102");
        jdbcTemplate.execute("update match_players set leader = 10203 where id = 10103");

        processor.process(getCallbackMessage("15000_SL_10200"));

        MatchState actualMatchState = jdbcTemplate.queryForObject("select state from matches where id = 15000", MatchState.class);

        assertThat(actualMatchState).isEqualTo(MatchState.SUBMITTED);
    }

    @Test
    void shouldNotChangeStateOnNotLastLeaderSubmit() {
        jdbcTemplate.execute("update match_players set leader = 10201 where id = 10102");
        jdbcTemplate.execute("update match_players set leader = 10200 where id = 10103");

        processor.process(getCallbackMessage("15000_SL_10202"));

        MatchState actualMatchState = jdbcTemplate.queryForObject("select state from matches where id = 15000", MatchState.class);

        assertThat(actualMatchState).isEqualTo(MatchState.ON_SUBMIT);
    }

    @Test
    void shouldThrowWhenLeaderIsSubmittedTwice() {
        jdbcTemplate.execute("update match_players set leader = 10201 where id = 10101");
        CommandMessage callbackMessage = getCallbackMessage("15000_SL_10201");

        assertThatThrownBy(() -> processor.process(callbackMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Вы уже назначили лидера la leader 2 игроку name2 (st_pl2) l2. " +
                            "Выберите другого лидера, или используйте команду '/resubmit 15000', чтобы начать заново.");
    }

    @Test
    void shouldNotChangeSchedulingOnNotLastLeaderSubmit() {
        jdbcTemplate.execute("update match_players set leader = 10201 where id = 10102");
        jdbcTemplate.execute("update match_players set leader = 10200 where id = 10103");

        processor.process(getCallbackMessage("15000_SL_10202"));

        verify(taskScheduler, never()).cancelSingleRunTask(any());
        verify(taskScheduler, never()).rescheduleSingleRunTask(any(), any(), any());
    }

    @Test
    void shouldScheduleSubmitAcceptTimeoutTaskOnLastLeaderSubmit() {
        jdbcTemplate.execute("update match_players set leader = 10201 where id = 10101");
        jdbcTemplate.execute("update match_players set leader = 10202 where id = 10102");
        jdbcTemplate.execute("update match_players set leader = 10203 where id = 10103");

        processor.process(getCallbackMessage("15000_SL_10200"));

        Instant expectedStartTime = NOW.plus(13, ChronoUnit.MINUTES);
        DuneBotTaskId taskId = new DuneBotTaskId(DuneTaskType.SUBMIT_ACCEPT_TIMEOUT, 15000L);
        verify(taskScheduler).rescheduleSingleRunTask(isA(SubmitAcceptTimeoutTask.class), eq(taskId), eq(expectedStartTime));
    }

    @Test
    void shouldCancelSubmitTimeoutTaskOnLastLeaderSubmit() {
        jdbcTemplate.execute("update match_players set leader = 10201 where id = 10101");
        jdbcTemplate.execute("update match_players set leader = 10202 where id = 10102");
        jdbcTemplate.execute("update match_players set leader = 10203 where id = 10103");

        processor.process(getCallbackMessage("15000_SL_10200"));

        DuneBotTaskId taskId = new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT, 15000L);
        verify(taskScheduler).cancelSingleRunTask(taskId);
    }

    @Test
    void shouldCancelSubmitTimeoutNotificationTaskOnLastLeaderSubmit() {
        jdbcTemplate.execute("update match_players set leader = 10201 where id = 10101");
        jdbcTemplate.execute("update match_players set leader = 10202 where id = 10102");
        jdbcTemplate.execute("update match_players set leader = 10203 where id = 10103");

        processor.process(getCallbackMessage("15000_SL_10200"));

        DuneBotTaskId taskId = new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT_NOTIFICATION, 15000L);
        verify(taskScheduler).cancelSingleRunTask(taskId);
    }

    @Test
    void shouldNotSaveLeaderWhenItIsSubmittedTwice() {
        jdbcTemplate.execute("update match_players set leader = 10201 where id = 10100");

        try {
            processor.process(getCallbackMessage("15000_SL_10201"));
        } catch (Exception ignored) {
        }

        Boolean isLeaderSaved = jdbcTemplate.queryForObject(
                "select exists(select 1 from match_players where id between 10101 and 10103 and leader is not null)", Boolean.class);
        assertThat(isLeaderSaved).isNotNull().isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"NEW", "ON_SUBMIT", "SUBMITTED"})
    void shouldThrowWhenMatchInWrongState(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 15000");
        CommandMessage callbackMessage = getCallbackMessage("15000_SL_10201");

        assertThatThrownBy(() -> processor.process(callbackMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Матч 15000 уже завершен. Регистрация результата более невозможна.");
    }

    @Test
    void shouldReturnLeaderAcceptCommand() {
        Command actualCommand = processor.getCommand();
        assertThat(actualCommand).isEqualTo(Command.LEADER_ACCEPT);
    }

    private static CommandMessage getCallbackMessage(String callbackData) {
        User user = new User();
        user.setId(USER_ID);
        Message message = new Message();
        message.setFrom(user);
        Chat chat = new Chat();
        chat.setId(USER_ID);
        message.setChat(chat);
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(message);
        callbackQuery.setData(callbackData);
        callbackQuery.setFrom(user);
        Update update = new Update();
        update.setCallbackQuery(callbackQuery);
        return CommandMessage.getCallbackInstance(callbackQuery);
    }
}
