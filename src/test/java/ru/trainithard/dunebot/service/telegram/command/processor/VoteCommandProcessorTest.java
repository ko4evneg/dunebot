package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.SneakyThrows;
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
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.scheduling.TaskScheduler;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import ru.trainithard.dunebot.TestConstants;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
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

@SpringBootTest
class VoteCommandProcessorTest extends TestContextMock {
    private static final int REPLY_ID = 111001;
    @Autowired
    private VoteCommandProcessor processor;
    @MockBean
    private TaskScheduler dunebotTaskScheduler;
    @MockBean
    private Clock clock;

    private static final String POLL_ID = "100500";
    private static final long CHAT_ID = 100501L;
    private static final int TOPIC_ID = 100500;
    private static final long USER_1_ID = 12345L;
    private static final long USER_2_ID = 12346L;
    private static final long GUEST_ID = 12400L;
    //TODO: check need
    private static final Instant NOW = LocalDate.of(2010, 10, 10).atTime(15, 0, 0)
            .toInstant(ZoneOffset.UTC);

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture).when(dunebotTaskScheduler).schedule(any(), any(Instant.class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10000, " + USER_1_ID + ", 12345, 'st_pl1', 'name1', 'l1', 'ef1', 'en1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, created_at) " +
                             "values (10001, " + USER_2_ID + ", 12346, 'st_pl2', 'name2', 'l2', 'ef2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10002, 12347, 12347, 'st_pl3', 'name3', 'l3', 'ef3', 'en3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10003, 12348, 12348, 'st_pl4', 'name4', 'l4', 'ef4', 'en4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                             "values (10000, 'ExternalPollId', " + REPLY_ID + ", " + TestConstants.CHAT_ID + ", " + TOPIC_ID + ", '" + POLL_ID + "', '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, owner_id, mod_type, state, positive_answers_count, created_at) " +
                             "values (10000, 10000, 10000, '" + ModType.CLASSIC + "','" + MatchState.NEW + "', 1, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10000, 10000, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) " +
                             "values (10000, '" + SettingKey.MATCH_START_DELAY + "', 60, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from settings where id = 10000");
        jdbcTemplate.execute("delete from match_players where match_id = 10000");
        jdbcTemplate.execute("delete from matches where id = 10000");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004 or external_id in (" + USER_2_ID + ", " + GUEST_ID + ")");
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10001 or chat_id between 12345 and 12348 " +
                             "or chat_id in (" + CHAT_ID + ", " + GUEST_ID + ")");
    }

    @Test
    void shouldSaveNewMatchPlayerOnPositiveReplyRegistration() {
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, USER_2_ID), mockLoggingId);

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, containsInAnyOrder(10000L, 10001L));
    }

    @Test
    void shouldSaveNewGuestMatchPlayerOnPositiveReplyRegistration() {
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID), mockLoggingId);

        List<Long> actualPlayerExternalIds = jdbcTemplate.queryForList("select p.external_id from match_players mp " +
                                                                       "left join players p on p.id = mp.player_id " +
                                                                       "where mp.match_id = 10000", Long.class);

        assertThat(actualPlayerExternalIds, containsInAnyOrder(USER_1_ID, GUEST_ID));
    }

    @Test
    void shouldCorrectlyFillGuestUserFields() {
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID), mockLoggingId);

        Player actualPlayer = jdbcTemplate
                .queryForObject("select * from players p join match_players mp on p.id = mp.player_id " +
                                "where mp.match_id = 10000 and p.external_id = " + GUEST_ID, new BeanPropertyRowMapper<>(Player.class));

        assertEquals("Vasya", actualPlayer.getFirstName());
        assertEquals("Pupkin", actualPlayer.getLastName());
        assertEquals("guest1", actualPlayer.getSteamName());
        assertEquals(GUEST_ID, actualPlayer.getExternalChatId());
    }

    @Test
    void shouldCorrectlySetGuestIndex() {
        jdbcTemplate.execute("update players set is_guest = true, steam_name = 'guest411' where id = 10002");
        jdbcTemplate.execute("update players set is_guest = true, steam_name = 'guest412' where id = 10003");
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID), mockLoggingId);

        String actualGuestName = jdbcTemplate
                .queryForObject("select steam_name from players p join match_players mp on p.id = mp.player_id " +
                                "where mp.match_id = 10000 and p.external_id = " + GUEST_ID, String.class);

        assertEquals("guest413", actualGuestName);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotSaveNewMatchPlayerOnNonPositiveReplyRegistration(int optionId) {
        processor.process(getPollAnswerCommandMessage(optionId, USER_2_ID), mockLoggingId);

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, containsInAnyOrder(10000L));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldDeleteMatchPlayerOnPositiveRegistrationRevocation(int optionId) {
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10001, '2010-10-10')");

        processor.process(getPollAnswerCommandMessage(optionId, USER_2_ID), mockLoggingId);

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, contains(10000L));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldDeleteGuestMatchPlayerOnPositiveRegistrationRevocation(int optionId) {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10004, " + GUEST_ID + ", " + GUEST_ID + ", 'guest99', 'name5', 'l5', 'ef5', 'en5', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10004, 10000, 10004, '2010-10-10')");

        processor.process(getPollAnswerCommandMessage(optionId, GUEST_ID), mockLoggingId);

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, contains(10000L));
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"NEW"})
    void shouldNotDeleteNotNewStateMatchPlayerOnPositiveRegistrationRevocation(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 10000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10001, '2010-10-10')");

        processor.process(getPollAnswerCommandMessage(1, USER_2_ID), mockLoggingId);

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, contains(10000L, 10001L));
    }

    @Test
    void shouldSendDeleteStartMessageOnPositiveRegistrationRevocationWhenNotEnoughPlayersLeft() {
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 9000, " + CHAT_ID + ", " + TOPIC_ID + ", '2020-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 4, external_start_id = 10001 where id = 10000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10001, '2010-10-10')");

        processor.process(getPollAnswerCommandMessage(1, USER_2_ID), mockLoggingId);

        verify(messagingService, times(1)).deleteMessageAsync(argThat(messageDto ->
                messageDto.getMessageId().equals(9000) && messageDto.getChatId().equals(CHAT_ID) && messageDto.getReplyId().equals(TOPIC_ID)));
    }

    @Test
    void shouldNotSendDeleteStartMessageOnPositiveRegistrationRevocationWhenEnoughPlayersLeft() {
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 9000, " + CHAT_ID + ", " + TOPIC_ID + ", '2020-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 5, external_start_id = 10001 where id = 10000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10001, '2010-10-10')");

        processor.process(getPollAnswerCommandMessage(1, USER_2_ID), mockLoggingId);

        verify(messagingService, never()).deleteMessageAsync(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotDeleteMatchRegisteredPlayerOnNonPositiveReplyRegistrationRevocation(int optionId) {
        processor.process(getPollAnswerCommandMessage(optionId, USER_2_ID), mockLoggingId);

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds, contains(10000L));
    }

    // TODO:  del from here
    @Test
    void shouldIncreaseMatchRegisteredPlayersCountOnPositiveReplyRegistration() {
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, USER_2_ID), mockLoggingId);

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertEquals(2, actualPlayersCount);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotIncreaseMatchRegisteredPlayersCountOnNonPositiveReplyRegistration(int optionId) {
        processor.process(getPollAnswerCommandMessage(optionId, USER_2_ID), mockLoggingId);

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @Test
    void shouldDecreaseMatchRegisteredPlayersCountOnPositiveReplyRegistrationRevocation() {
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10001, '2010-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 2 where id = 10000");

        processor.process(getPollAnswerCommandMessage(null, USER_2_ID), mockLoggingId);

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotDecreaseMatchRegisteredPlayersCountOnNonPositiveReplyRegistrationRevocation(int optionId) {
        processor.process(getPollAnswerCommandMessage(optionId, USER_2_ID), mockLoggingId);

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @Test
    void shouldAddScheduledTaskOnFourthPlayerRegistration() {
        jdbcTemplate.execute("update matches set positive_answers_count = 3 where id = 10000");

        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, USER_2_ID), mockLoggingId);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(dunebotTaskScheduler, times(1)).schedule(any(), instantCaptor.capture());
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

        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, USER_2_ID), mockLoggingId);
        syncRunScheduledTaskAction();

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();
        String[] textRows = messageDto.getText().split("\n");
        List<String> names = Arrays.stream(textRows[1].split(", ")).toList();

        assertEquals(TestConstants.CHAT_ID, messageDto.getChatId());
        assertEquals(TOPIC_ID, messageDto.getTopicId());
        assertEquals(REPLY_ID, messageDto.getReplyMessageId());
        assertEquals("*Матч 10000* собран\\. Участники:", textRows[0]);
        assertThat(names, containsInAnyOrder("@en1", "@ef2", "@en3", "@en4"));
        assertNull(messageDto.getKeyboard());
    }

    @Test
    void shouldSendGuestWarningMessageOnFourthPlayerRegistration() throws InterruptedException {
        doReturn(CompletableFuture.completedFuture(getSubmitExternalMessage())).when(messagingService).sendMessageAsync(any(MessageDto.class));

        jdbcTemplate.execute("update players set is_guest = true, steam_name = 'guest411' where id = 10002");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10001, 10000, 10001, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10002, 10000, 10002, '2010-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 3 where id = 10000");

        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID), mockLoggingId);
        syncRunScheduledTaskAction();

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(2)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getAllValues().get(1);
        String[] textRows = messageDto.getText().split("\n");
        List<String> names = Arrays.stream(textRows[1].split(", ")).toList();
        List<String> guestsNames = Arrays.stream(textRows[4].split(", ")).toList();

        assertEquals(TestConstants.CHAT_ID, messageDto.getChatId());
        assertEquals(TOPIC_ID, messageDto.getTopicId());
        assertEquals(REPLY_ID, messageDto.getReplyMessageId());
        assertEquals("*Матч 10000* собран\\. Участники:", textRows[0]);
        assertThat(names, containsInAnyOrder("@en1", "@ef2"));
        assertTrue(textRows[2].isBlank());
        assertEquals("*Внимание:* в матче есть незарегистрированные игроки\\. Они автоматически зарегистрированы " +
                     "под именем Vasya Pupkin и смогут подтвердить результаты матчей для регистрации результатов:", textRows[3]);
        assertThat(guestsNames, containsInAnyOrder("@en3", "@fName"));
        assertNull(messageDto.getKeyboard());
    }

    @Test
    void shouldSendPrivateMessageOnNewGuestPlayerPositiveRegistration() {
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID), mockLoggingId);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        String actualText = messageDto.getText().replace("\\", "");
        assertEquals("""
                Вас приветствует DuneBot! Вы ответили да в опросе по рейтинговой игре - это значит, что по завершении \
                игры вам придет опрос, где нужно будет указать занятое в игре место (и загрузить скриншот матча в \
                случае победы) - не волнуйтесь, бот подскажет что делать.
                Также вы автоматически зарегистрированы у бота как гость под именем Vasya (guest1) Pupkin - это значит, что вы не \
                можете выполнять некоторые команды бота и не будете включены в результаты рейтинга.
                Для того, чтобы подтвердить регистрацию, выполните в этом чате команду* '/refresh_profile Имя (Steam) Фамилия'*.
                *Желательно это  сделать прямо сейчас.*
                Подробная информация о боте: /help.""", actualText);
        assertEquals(GUEST_ID, Long.parseLong(messageDto.getChatId()));
        assertNull(messageDto.getTopicId());
    }

    @Test
    void shouldSendPrivateMessageOnExistingGuestPlayerPositiveRegistration() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, is_guest, created_at) " +
                             "values (10004, " + GUEST_ID + ", " + GUEST_ID + ", 'guest1', 'Vasya', 'Pupkin', 'ef4', 'en4', true, '2010-10-10') ");

        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID), mockLoggingId);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        String actualText = messageDto.getText().replace("\\", "");
        assertEquals("""
                Вас приветствует DuneBot! Вы ответили да в опросе по рейтинговой игре - это значит, что по завершении \
                игры вам придет опрос, где нужно будет указать занятое в игре место (и загрузить скриншот матча в \
                случае победы) - не волнуйтесь, бот подскажет что делать.
                Также вы автоматически зарегистрированы у бота как гость под именем Vasya (guest1) Pupkin - это значит, что вы не \
                можете выполнять некоторые команды бота и не будете включены в результаты рейтинга.
                Для того, чтобы подтвердить регистрацию, выполните в этом чате команду* '/refresh_profile Имя (Steam) Фамилия'*.
                *Желательно это  сделать прямо сейчас.*
                Подробная информация о боте: /help.""", actualText);
        assertEquals(GUEST_ID, Long.parseLong(messageDto.getChatId()));
        assertNull(messageDto.getTopicId());
    }

    @Test
    void shouldSetMatchSubmitMessageIdOnFourthPlayerRegistration() throws InterruptedException {
        doReturn(CompletableFuture.completedFuture(getSubmitExternalMessage())).when(messagingService).sendMessageAsync(any(MessageDto.class));

        jdbcTemplate.execute("update matches set positive_answers_count = 3 where id = 10000");

        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, USER_2_ID), mockLoggingId);
        syncRunScheduledTaskAction();

        Boolean isExternalIsSet = jdbcTemplate.queryForObject("select exists (select 1 from matches where id = 10000 and external_start_id = " +
                                                              "(select id from external_messages where chat_id = " + CHAT_ID + " and message_id = 111000 and reply_id = 111001))", Boolean.class);

        assertNotNull(isExternalIsSet);
        assertTrue(isExternalIsSet);
    }

    @Test
    void shouldReturnVoteCommand() {
        Command actualCommand = processor.getCommand();

        assertEquals(Command.VOTE, actualCommand);
    }

    private void syncRunScheduledTaskAction() throws InterruptedException {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(dunebotTaskScheduler, times(1)).schedule(runnableCaptor.capture(), any(Instant.class));
        Runnable actualRunnable = runnableCaptor.getValue();

        Thread thread = new Thread(actualRunnable);
        thread.start();
        thread.join();
    }

    private ExternalMessageDto getSubmitExternalMessage() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(REPLY_ID);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        Message message = new Message();
        message.setMessageId(111000);
        message.setReplyToMessage(replyMessage);
        message.setChat(chat);
        message.setMessageThreadId(TOPIC_ID);
        return new ExternalMessageDto(message);
    }

    private CommandMessage getPollAnswerCommandMessage(Integer optionId, long userId) {
        User user = new User();
        user.setId(userId);
        user.setFirstName("fName");
        PollAnswer pollAnswer = new PollAnswer();
        pollAnswer.setUser(user);
        pollAnswer.setOptionIds(optionId == null ? Collections.emptyList() : Collections.singletonList(optionId));
        pollAnswer.setPollId(POLL_ID);
        return CommandMessage.getPollAnswerInstance(pollAnswer);
    }
}
