package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.TaskScheduler;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.trainithard.dunebot.configuration.SettingConstants.POSITIVE_POLL_OPTION_ID;

@SpringBootTest
class VoteCommandProcessorTest extends TestContextMock {

    @Autowired
    private VoteCommandProcessor commandProcessor;
    @MockBean
    private MessagingService messagingService;
    @MockBean
    private Clock clock;
    @MockBean
    private TaskScheduler taskScheduler;

    private static final String POLL_ID = "100500";
    private static final long CHAT_ID = 100501L;
    private static final int REPLY_ID = 100500;
    private static final long USER_2_ID = 12346L;
    private static final Instant NOW = LocalDate.of(2010, 10, 10).atTime(15, 0, 0)
            .toInstant(ZoneOffset.UTC);

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(), any(Instant.class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, external_name, created_at) " +
                "values (10000, 12345, 12345, 'st_pl1', 'name1', 'en1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10001, " + USER_2_ID + ", 12346, 'st_pl2', 'name2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, external_name, created_at) " +
                "values (10002, 12347, 12347, 'st_pl3', 'name3', 'en3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, external_name, created_at) " +
                "values (10003, 12348, 12348, 'st_pl4', 'name4', 'en4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                "values (10000, 'ExternalPollId', 123, " + SettingConstants.CHAT_ID + ", " + REPLY_ID + ", '" + POLL_ID + "', '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, owner_id, mod_type, positive_answers_count, created_at) " +
                "values (10000, 10000, 10000, '" + ModType.CLASSIC + "', 1, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10000, 10000, 10000, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id = 10000");
        jdbcTemplate.execute("delete from matches where id = 10000");
        jdbcTemplate.execute("delete from players where id between 10000 and 10003 or external_id = " + USER_2_ID);
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10001 or chat_id between 12345 and 12348 or chat_id = " + CHAT_ID);
    }

    @Test
    void shouldSaveNewMatchPlayerOnPositiveReplyRegistration() {
        commandProcessor.process(getPollAnswerCommandMessage(POSITIVE_POLL_OPTION_ID));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, containsInAnyOrder(10000L, 10001L));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotSaveNewMatchPlayerOnNonPositiveReplyRegistration(int optionId) {
        commandProcessor.process(getPollAnswerCommandMessage(optionId));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, containsInAnyOrder(10000L));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldDeleteMatchPlayerOnPositiveRegistrationRevocation(int optionId) {
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10003, 10000, 10001, '2010-10-10')");

        commandProcessor.process(getPollAnswerCommandMessage(optionId));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, contains(10000L));
    }

    @Test
    void shouldSendDeleteStartMessageOnPositiveRegistrationRevocationWhenNotEnoughPlayersLeft() {
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, created_at) " +
                "values (10001, 'ExternalMessageId', 9000, " + CHAT_ID + ", " + REPLY_ID + ", '2020-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 4, external_start_id = 10001 where id = 10000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10003, 10000, 10001, '2010-10-10')");

        commandProcessor.process(getPollAnswerCommandMessage(1));

        verify(messagingService, times(1)).deleteMessageAsync(argThat(messageDto ->
                messageDto.getMessageId().equals(9000) && messageDto.getChatId().equals(CHAT_ID) && messageDto.getReplyId().equals(REPLY_ID)));
    }

    @Test
    void shouldNotSendDeleteStartMessageOnPositiveRegistrationRevocationWhenEnoughPlayersLeft() {
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, created_at) " +
                "values (10001, 'ExternalMessageId', 9000, " + CHAT_ID + ", " + REPLY_ID + ", '2020-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 5, external_start_id = 10001 where id = 10000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10003, 10000, 10001, '2010-10-10')");

        commandProcessor.process(getPollAnswerCommandMessage(1));

        verify(messagingService, never()).deleteMessageAsync(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotDeleteMatchRegisteredPlayerOnNonPositiveReplyRegistrationRevocation(int optionId) {
        commandProcessor.process(getPollAnswerCommandMessage(optionId));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, contains(10000L));
    }

    @Test
    void shouldIncreaseMatchRegisteredPlayersCountOnPositiveReplyRegistration() {
        commandProcessor.process(getPollAnswerCommandMessage(POSITIVE_POLL_OPTION_ID));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertEquals(2, actualPlayersCount);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotIncreaseMatchRegisteredPlayersCountOnNonPositiveReplyRegistration(int optionId) {
        commandProcessor.process(getPollAnswerCommandMessage(optionId));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @Test
    void shouldDecreaseMatchRegisteredPlayersCountOnPositiveReplyRegistrationRevocation() {
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10003, 10000, 10001, '2010-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 2 where id = 10000");

        commandProcessor.process(getPollAnswerCommandMessage(null));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotDecreaseMatchRegisteredPlayersCountOnNonPositiveReplyRegistrationRevocation(int optionId) {
        commandProcessor.process(getPollAnswerCommandMessage(optionId));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @Test
    void shouldAddScheduledTaskOnFourthPlayerRegistration() {
        jdbcTemplate.execute("update matches set positive_answers_count = 3 where id = 10000");

        commandProcessor.process(getPollAnswerCommandMessage(POSITIVE_POLL_OPTION_ID));

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler, times(1)).schedule(any(), instantCaptor.capture());
        Instant actualInstant = instantCaptor.getValue();

        assertEquals(NOW.plusSeconds(60), actualInstant);
    }

    @Test
    void shouldSendMessageOnFourthPlayerRegistration() throws InterruptedException {
        doReturn(CompletableFuture.completedFuture(getSubmitExternalMessage())).when(messagingService).sendMessageAsync(any(MessageDto.class));

        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10001, 10000, 10002, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10002, 10000, 10003, '2010-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 3 where id = 10000");

        commandProcessor.process(getPollAnswerCommandMessage(POSITIVE_POLL_OPTION_ID));
        syncRunScheduledTaskAction();

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertEquals(SettingConstants.CHAT_ID, messageDto.getChatId());
        assertEquals(REPLY_ID, messageDto.getReplyMessageId());
        assertEquals("""
                Матч 10000 собран. Участники:
                @en1, @name2, @en3, @en4""", messageDto.getText());
        assertNull(messageDto.getKeyboard());
    }

    @Test
    void shouldSetMatchSubmitMessageIdOnFourthPlayerRegistration() throws InterruptedException {
        doReturn(CompletableFuture.completedFuture(getSubmitExternalMessage())).when(messagingService).sendMessageAsync(any(MessageDto.class));

        jdbcTemplate.execute("update matches set positive_answers_count = 3 where id = 10000");

        commandProcessor.process(getPollAnswerCommandMessage(POSITIVE_POLL_OPTION_ID));
        syncRunScheduledTaskAction();

        Boolean isExternalIsSet = jdbcTemplate.queryForObject("select exists (select 1 from matches where id = 10000 and external_start_id = " +
                "(select id from external_messages where chat_id = " + CHAT_ID + " and message_id = 111000 and reply_id = 111001))", Boolean.class);

        assertNotNull(isExternalIsSet);
        assertTrue(isExternalIsSet);
    }

    private void syncRunScheduledTaskAction() throws InterruptedException {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler, times(1)).schedule(runnableCaptor.capture(), any(Instant.class));
        Runnable actualRunnable = runnableCaptor.getValue();

        Thread thread = new Thread(actualRunnable);
        thread.start();
        thread.join();
    }

    private ExternalMessageDto getSubmitExternalMessage() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(111001);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        Message message = new Message();
        message.setMessageId(111000);
        message.setReplyToMessage(replyMessage);
        message.setChat(chat);
        return new ExternalMessageDto(message);
    }

    private CommandMessage getPollAnswerCommandMessage(Integer optionId) {
        User user = new User();
        user.setId(VoteCommandProcessorTest.USER_2_ID);
        PollAnswer pollAnswer = new PollAnswer();
        pollAnswer.setUser(user);
        pollAnswer.setOptionIds(optionId == null ? Collections.emptyList() : Collections.singletonList(optionId));
        pollAnswer.setPollId(POLL_ID);
        return new CommandMessage(pollAnswer);
    }
}
