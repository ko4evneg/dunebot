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
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import ru.trainithard.dunebot.TestConstants;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class VoteCommandProcessorTest extends TestContextMock {
    private static final int REPLY_ID = 111001;
    private static final String POLL_ID = "100500";
    private static final long CHAT_ID = 100501L;
    private static final int TOPIC_ID = 100500;
    private static final long USER_1_ID = 12345L;
    private static final long USER_2_ID = 12346L;
    private static final long GUEST_ID = 12400L;
    private static final Instant NOW = LocalDate.of(2010, 10, 10).atTime(15, 0, 0)
            .toInstant(ZoneOffset.UTC);

    @Autowired
    private VoteCommandProcessor processor;
    @MockBean
    private DuneBotTaskScheduler dunebotTaskScheduler;
    @MockBean
    private Clock clock;

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
        CompletableFuture<ExternalMessageDto> mockResponse = new CompletableFuture<>();
        mockResponse.complete(new ExternalMessageDto());
        doReturn(mockResponse).when(messagingService).sendMessageAsync(any());

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
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) " +
                             "values (10000, '" + AppSettingKey.MATCH_START_DELAY + "', 60, '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) " +
                             "values (10001, '" + AppSettingKey.NEXT_GUEST_INDEX + "', 1, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from app_settings where id between 10000 and 10001");
        jdbcTemplate.execute("delete from match_players where match_id = 10000");
        jdbcTemplate.execute("delete from matches where id = 10000");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004 or external_id in (" + USER_2_ID + ", " + GUEST_ID + ")");
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10001 or chat_id between 12345 and 12348 " +
                             "or chat_id in (" + CHAT_ID + ", " + GUEST_ID + ")");
    }

    @Test
    void shouldSaveNewMatchPlayerOnPositiveReplyRegistration() {
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, USER_2_ID));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds).containsExactlyInAnyOrder(10000L, 10001L);
    }

    @Test
    void shouldSaveNewGuestMatchPlayerOnPositiveReplyRegistration() {
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID));

        List<Long> actualPlayerExternalIds = jdbcTemplate.queryForList("select p.external_id from match_players mp " +
                                                                       "left join players p on p.id = mp.player_id " +
                                                                       "where mp.match_id = 10000", Long.class);

        assertThat(actualPlayerExternalIds).containsExactlyInAnyOrder(USER_1_ID, GUEST_ID);
    }

    @Test
    void shouldCorrectlyFillGuestUserFields() {
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID));

        Player actualPlayer = jdbcTemplate
                .queryForObject("select * from players p join match_players mp on p.id = mp.player_id " +
                                "where mp.match_id = 10000 and p.external_id = " + GUEST_ID, new BeanPropertyRowMapper<>(Player.class));

        assertThat(actualPlayer)
                .extracting(Player::getFirstName, Player::getLastName, Player::getSteamName, Player::getExternalChatId, Player::isChatBlocked)
                .containsExactly("Vasya", "Pupkin", "guest1", GUEST_ID, false);
    }

    @Test
    void shouldSetGuestBlockedChatFlagWhenUserChatIsBlocked() {
        ApiResponse<?> apiResponse = mock(ApiResponse.class);
        doReturn(403).when(apiResponse).getErrorCode();
        TelegramApiRequestException exception = new TelegramApiRequestException("lol, blocked", apiResponse);
        CompletableFuture<ExternalMessageDto> mockExceptionalResponse = new CompletableFuture<>();
        mockExceptionalResponse.completeExceptionally(exception);
        doReturn(mockExceptionalResponse).when(messagingService).sendMessageAsync(any());

        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID));

        Boolean actualIsChatBlocked = jdbcTemplate
                .queryForObject("select exists(select 1 from players p join match_players mp on p.id = mp.player_id " +
                                "where p.is_chat_blocked and mp.match_id = 10000 and p.external_id = " + GUEST_ID + ")", Boolean.class);

        assertThat(actualIsChatBlocked).isNotNull().isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 402, 500})
    void shouldNotSetGuestBlockedChatFlagWhenExceptionThrownButUserChatIsNotBlocked(int exceptionCode) {
        ApiResponse<?> apiResponse = mock(ApiResponse.class);
        doReturn(exceptionCode).when(apiResponse).getErrorCode();
        TelegramApiRequestException exception = new TelegramApiRequestException("lol, blocked", apiResponse);
        CompletableFuture<ExternalMessageDto> mockExceptionalResponse = new CompletableFuture<>();
        mockExceptionalResponse.completeExceptionally(exception);
        doReturn(mockExceptionalResponse).when(messagingService).sendMessageAsync(any());

        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID));

        Boolean actualIsChatBlocked = jdbcTemplate
                .queryForObject("select exists(select 1 from players p join match_players mp on p.id = mp.player_id " +
                                "where p.is_chat_blocked and mp.match_id = 10000 and p.external_id = " + GUEST_ID + ")", Boolean.class);

        assertThat(actualIsChatBlocked).isNotNull().isFalse();
    }

    @Test
    void shouldCorrectlySetGuestIndex() {
        jdbcTemplate.execute("update players set is_guest = true, steam_name = 'guest9' where id = 10002");
        jdbcTemplate.execute("update players set is_guest = true, steam_name = 'guest10' where id = 10003");
        jdbcTemplate.execute("update app_settings set value = 11 where id = 10001");
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID));

        String actualGuestName = jdbcTemplate
                .queryForObject("select steam_name from players p join match_players mp on p.id = mp.player_id " +
                                "where mp.match_id = 10000 and p.external_id = " + GUEST_ID, String.class);

        assertThat(actualGuestName).isEqualTo("guest11");
    }

    @Test
    void shouldUpdateGuestIndexSettingWhenGuestPlayerRegistered() {
        jdbcTemplate.execute("update players set is_guest = true, steam_name = 'guest9' where id = 10002");
        jdbcTemplate.execute("update players set is_guest = true, steam_name = 'guest10' where id = 10003");
        jdbcTemplate.execute("update app_settings set value = 11 where id = 10001");
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID));

        Integer actualNextIndex = jdbcTemplate.queryForObject("select value from app_settings where id = 10001", Integer.class);

        assertThat(actualNextIndex).isEqualTo(12);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotSaveNewMatchPlayerOnNonPositiveReplyRegistration(int optionId) {
        processor.process(getPollAnswerCommandMessage(optionId, USER_2_ID));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds).containsExactly(10000L);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldDeleteMatchPlayerOnPositiveRegistrationRevocation(int optionId) {
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10001, '2010-10-10')");

        processor.process(getPollAnswerCommandMessage(optionId, USER_2_ID));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds).containsExactly(10000L);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldDeleteGuestMatchPlayerOnPositiveRegistrationRevocation(int optionId) {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10004, " + GUEST_ID + ", " + GUEST_ID + ", 'guest99', 'name5', 'l5', 'ef5', 'en5', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10004, 10000, 10004, '2010-10-10')");

        processor.process(getPollAnswerCommandMessage(optionId, GUEST_ID));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds).containsExactly(10000L);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"NEW"})
    void shouldNotDeleteNotNewStateMatchPlayerOnPositiveRegistrationRevocation(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 10000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10001, '2010-10-10')");

        processor.process(getPollAnswerCommandMessage(1, USER_2_ID));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds).containsExactly(10000L, 10001L);
    }

    @Test
    void shouldSendDeleteStartMessageOnPositiveRegistrationRevocationWhenNotEnoughPlayersLeft() {
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 9000, " + CHAT_ID + ", " + TOPIC_ID + ", '2020-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 4, external_start_id = 10001 where id = 10000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10001, '2010-10-10')");

        processor.process(getPollAnswerCommandMessage(1, USER_2_ID));

        verify(messagingService, times(1)).deleteMessageAsync(argThat(messageDto ->
                messageDto.getMessageId().equals(9000) && messageDto.getChatId().equals(CHAT_ID) && messageDto.getReplyId().equals(TOPIC_ID)));
    }

    @Test
    void shouldCancelMatchStartTaskOnPositiveRegistrationRevocationWhenNotEnoughPlayersLeft() {
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 9000, " + CHAT_ID + ", " + TOPIC_ID + ", '2020-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 4, external_start_id = 10001 where id = 10000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10001, '2010-10-10')");

        processor.process(getPollAnswerCommandMessage(1, USER_2_ID));

        DuneTaskId duneTaskId = new DuneTaskId(DuneTaskType.START_MESSAGE, 10000L);
        verify(dunebotTaskScheduler).cancel(duneTaskId);
    }

    @Test
    void shouldNotSendDeleteStartMessageOnPositiveRegistrationRevocationWhenEnoughPlayersLeft() {
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 9000, " + CHAT_ID + ", " + TOPIC_ID + ", '2020-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 5, external_start_id = 10001 where id = 10000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10001, '2010-10-10')");

        processor.process(getPollAnswerCommandMessage(1, USER_2_ID));

        verify(messagingService, never()).deleteMessageAsync(any());
    }

    @Test
    void shouldNotCancelMatchStartTaskOnPositiveRegistrationRevocationWhenEnoughPlayersLeft() {
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 9000, " + CHAT_ID + ", " + TOPIC_ID + ", '2020-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 5, external_start_id = 10001 where id = 10000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10001, '2010-10-10')");

        processor.process(getPollAnswerCommandMessage(1, USER_2_ID));

        verifyNoInteractions(dunebotTaskScheduler);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotDeleteMatchRegisteredPlayerOnNonPositiveReplyRegistrationRevocation(int optionId) {
        processor.process(getPollAnswerCommandMessage(optionId, USER_2_ID));

        List<Long> actualPlayerIds = jdbcTemplate.queryForList("select player_id from match_players where match_id = 10000", Long.class);

        assertThat(actualPlayerIds).containsExactly(10000L);
    }

    @Test
    void shouldIncreaseMatchRegisteredPlayersCountOnPositiveReplyRegistration() {
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, USER_2_ID));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertThat(actualPlayersCount).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotIncreaseMatchRegisteredPlayersCountOnNonPositiveReplyRegistration(int optionId) {
        processor.process(getPollAnswerCommandMessage(optionId, USER_2_ID));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertThat(actualPlayersCount).isEqualTo(1);
    }

    @Test
    void shouldDecreaseMatchRegisteredPlayersCountOnPositiveReplyRegistrationRevocation() {
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10001, '2010-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 2 where id = 10000");

        processor.process(getPollAnswerCommandMessage(null, USER_2_ID));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertThat(actualPlayersCount).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 100000})
    void shouldNotDecreaseMatchRegisteredPlayersCountOnNonPositiveReplyRegistrationRevocation(int optionId) {
        processor.process(getPollAnswerCommandMessage(optionId, USER_2_ID));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select positive_answers_count from matches where id = 10000", Long.class);

        assertThat(actualPlayersCount).isEqualTo(1);
    }

    @Test
    void shouldScheduleStartMessageTaskOnFourthPlayerRegistration() {
        jdbcTemplate.execute("update matches set positive_answers_count = 3 where id = 10000");

        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, USER_2_ID));

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        DuneTaskId expectedTaskId = new DuneTaskId(DuneTaskType.START_MESSAGE, 10000L);
        verify(dunebotTaskScheduler, times(1)).reschedule(any(), eq(expectedTaskId), instantCaptor.capture());
        Instant actualInstant = instantCaptor.getValue();

        assertThat(actualInstant).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void shouldFailUprising6MatchOnSixthPlayerRegistration() {
        doReturn(CompletableFuture.completedFuture(getSubmitExternalMessage())).when(messagingService).sendMessageAsync(any(MessageDto.class));

        jdbcTemplate.execute("update matches set mod_type = '" + ModType.UPRISING_6 + "' where id = 10000");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10001, 10000, 10002, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10002, 10000, 10003, '2010-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 5 where id = 10000");

        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, USER_2_ID));

        MatchState actualMatchState = jdbcTemplate.queryForObject("select state from matches where id = 10000", MatchState.class);

        assertThat(actualMatchState).isEqualTo(MatchState.FAILED);
    }

    @Test
    void shouldSendPrivateMessageOnNewGuestPlayerPositiveRegistration() {
        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        String actualText = messageDto.getText().replace("\\", "");
        assertThat(actualText).isEqualTo("""
                Вас приветствует DuneBot! Вы ответили да в опросе по рейтинговой игре - это значит, что по завершении \
                игры вам придет опрос, где нужно будет указать занятое в игре место (и загрузить скриншот матча в \
                случае победы) - не волнуйтесь, бот подскажет что делать.
                Также вы автоматически зарегистрированы у бота как гость под именем Vasya (guest1) Pupkin - это значит, что вы не \
                можете выполнять некоторые команды бота и не будете включены в результаты рейтинга.
                Для того, чтобы подтвердить регистрацию, выполните в этом чате команду* '/profile Имя (ник в steam) Фамилия'*.
                *Желательно это  сделать прямо сейчас.*
                Подробная информация о боте: /help.""");
        assertThat(Long.parseLong(messageDto.getChatId())).isEqualTo(GUEST_ID);
        assertThat(messageDto.getTopicId()).isNull();
    }

    @Test
    void shouldSendPrivateMessageOnExistingGuestPlayerPositiveRegistration() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, is_guest, created_at) " +
                             "values (10004, " + GUEST_ID + ", " + GUEST_ID + ", 'guest1', 'Vasya', 'Pupkin', 'ef4', 'en4', true, '2010-10-10') ");

        processor.process(getPollAnswerCommandMessage(TestConstants.POSITIVE_POLL_OPTION_ID, GUEST_ID));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        String actualText = messageDto.getText().replace("\\", "");
        assertThat(actualText).isEqualTo("""
                Вас приветствует DuneBot! Вы ответили да в опросе по рейтинговой игре - это значит, что по завершении \
                игры вам придет опрос, где нужно будет указать занятое в игре место (и загрузить скриншот матча в \
                случае победы) - не волнуйтесь, бот подскажет что делать.
                Также вы автоматически зарегистрированы у бота как гость под именем Vasya (guest1) Pupkin - это значит, что вы не \
                можете выполнять некоторые команды бота и не будете включены в результаты рейтинга.
                Для того, чтобы подтвердить регистрацию, выполните в этом чате команду* '/profile Имя (ник в steam) Фамилия'*.
                *Желательно это  сделать прямо сейчас.*
                Подробная информация о боте: /help.""");
        assertThat(Long.parseLong(messageDto.getChatId())).isEqualTo(GUEST_ID);
        assertThat(messageDto.getTopicId()).isNull();
    }

    @Test
    void shouldReturnVoteCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.VOTE);
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
