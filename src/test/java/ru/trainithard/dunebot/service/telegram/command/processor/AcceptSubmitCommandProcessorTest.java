package ru.trainithard.dunebot.service.telegram.command.processor;

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
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestConstants;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

@SpringBootTest
class AcceptSubmitCommandProcessorTest extends TestContextMock {
    private static final long USER_1_ID = 11000L;
    private static final long USER_2_ID = 11001L;

    @Autowired
    private AcceptSubmitCommandProcessor processor;
    @MockBean
    private MatchFinishingService matchFinishingService;
    @MockBean
    private ResubmitCommandProcessor resubmitCommandProcessor;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + USER_1_ID + ", 12000, 'st_pl1', 'name1', 'l1', 'ef1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, " + USER_2_ID + ", 12001, 'st_pl2', 'name2', 'l2', 'ef2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10002, 11002, 12002, 'st_pl3', 'name3', 'l3', 'ef3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10003, 11003, 12003, 'st_pl4', 'name4', 'l4', 'ef4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, created_at) " +
                             "values (10000, 'ExternalPollId', 10000, 10000, '10000', '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 10001, 10000, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, positive_answers_count, screenshot_path, state, created_at) " +
                             "values (15000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', 4, 'photos/1.jpg', '" + MatchState.NEW + "','2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10002, 'ExternalMessageId', 10002, 12000, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10003, 'ExternalMessageId', 10003, 12001, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10004, 'ExternalMessageId', 10004, 12002, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10005, 'ExternalMessageId', 10005, 12003, '2020-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                             "values (10000, 15000, 10000, 10002, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                             "values (10001, 15000, 10001, 10003, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                             "values (10002, 15000, 10002, 10004, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                             "values (10003, 15000, 10003, 10005, '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) " +
                             "values (10000, '" + AppSettingKey.RESUBMITS_LIMIT + "', 3, '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10000, 'lead1', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10001, 'lead2', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10002, 'lead3', '" + ModType.CLASSIC + "', '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from leaders where id in (10000, 10001, 10002)");
        jdbcTemplate.execute("delete from app_settings where id = 10000");
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10005");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 4, 6})
    void shouldSetCandidatePlaceOnCallbackReply(int expectedPlace) {
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__" + expectedPlace));

        Integer actualCandidatePlace = jdbcTemplate.queryForObject("select candidate_place from match_players where id = 10000", Integer.class);

        assertThat(actualCandidatePlace).isEqualTo(expectedPlace);
    }

    @Test
    void shouldNotChangeAlreadySetCandidatePlaceOnCallbackReply() {
        jdbcTemplate.execute("update match_players set candidate_place = 1 where id = 10000");
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"));

        Integer actualCandidatePlace = jdbcTemplate.queryForObject("select candidate_place from match_players where id = 10000", Integer.class);

        assertThat(actualCandidatePlace).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.INCLUDE, names = {"FAILED", "FINISHED", "EXPIRED"})
    void shouldNotInvokeActionsWhenCallbackReplyForEndedMatchReceived(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 15000");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__" + 1));

        verifyNoInteractions(matchFinishingService);
        verifyNoInteractions(resubmitCommandProcessor);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.INCLUDE, names = {"FAILED", "FINISHED", "EXPIRED"})
    void shouldSendMessageWhenCallbackReplyForEndedMatchReceived(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 15000");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__" + 1));

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessage = messageCaptor.getValue();

        // chatId expected to be equals userId, because CommandMessageFactory takes chatId from different source for callbacks
        assertThat(actualMessage)
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly("11000", "*Матч 15000* уже завершен\\. Регистрация вашего голоса невозможна\\.");
    }

    @Test
    void shouldIncreaseMatchSubmitCountOnCallbackReply() {
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__-1"));
        processor.process(getCommandMessage(USER_2_ID, 10003, 11003, "15000__3"));

        Integer actualCandidatePlace = jdbcTemplate.queryForObject("select submits_count from matches where id = 15000", Integer.class);

        assertThat(actualCandidatePlace).isEqualTo(2);
    }

    @Test
    void shouldInvokeMatchFinishOnLastCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3 where id = 15000");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"));

        verify(matchFinishingService, times(1)).finishSubmittedMatch(eq(15000L));
    }

    @Test
    void shouldNotInvokeMatchFinishOnNotLastCallbackReply() {
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"));

        verify(matchFinishingService, never()).finishSubmittedMatch(anyInt());
    }

    @Test
    void shouldNotInvokeMatchFinishOnLastConflictCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 2 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"));

        verify(matchFinishingService, never()).finishSubmittedMatch(eq(15000L));
    }

    @Test
    void shouldNotInvokeUnsuccessfulMatchFinishOnLastConflictCallbackReplyWhenResubmitsLimitIsNotReached() {
        jdbcTemplate.execute("update matches set submits_count = 3 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 2 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"));

        verify(matchFinishingService, never()).finishNotSubmittedMatch(15000L, false);
    }

    @Test
    void shouldInvokeMatchResubmitOnConflictCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 2 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 2 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"));

        verify(resubmitCommandProcessor, times(1))
                .process(argThat((Match match) -> match.getId().equals(15000L)));
    }

    @Test
    void shouldNotInvokeMatchResubmitOnConflictCallbackReplyWhenNotParticipatedOptionSelected() {
        jdbcTemplate.execute("update matches set submits_count = 2 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 0 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__0"));

        verifyNoInteractions(resubmitCommandProcessor);
    }

    @Test
    void shouldNotInvokeMatchResubmitOnResubmitExceedLastConflictCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3, submits_retry_count = " + TestConstants.RESUBMITS_LIMIT + " where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 2 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"));

        verify(resubmitCommandProcessor, never()).process(argThat((Match match) -> match.getId().equals(15000L)));
    }

    @Test
    void shouldSendMessageAboutResubmitOnLastConflictCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 2 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"));

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(4)).sendMessageAsync(messageCaptor.capture());
        List<MessageDto> actualMessages = messageCaptor.getAllValues();

        String conflictText = """
                Некоторые игроки не смогли поделить *2 место*:
                name2 \\(st\\_pl2\\) l2
                name1 \\(st\\_pl1\\) l1

                Повторный опрос результата\\.\\.\\.""";
        assertThat(actualMessages)
                .allMatch(message -> conflictText.equals(message.getText()))
                .extracting(MessageDto::getChatId)
                .containsExactlyInAnyOrder("12000", "12001", "12002", "12003");
    }

    @Test
    void shouldInvokeMatchUnsubmittedFinishOnResubmitExceedLastConflictCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3, submits_retry_count = " + TestConstants.RESUBMITS_LIMIT + " where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 2 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"));

        verify(matchFinishingService).finishNotSubmittedMatch(15000, true);
    }

    @Test
    void shouldSendDeleteMessageForSubmitMessageOnCallbackReply() {
        processor.process(getCommandMessage(USER_1_ID, 10002, 12000, "15000__1"));

        verify(messagingService, times(1)).deleteMessageAsync(argThat(messageDto ->
                messageDto.getMessageId().equals(10002) && messageDto.getChatId().equals(12000L)));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    void shouldSendMessageAboutCallbackAcceptOnCallbackReplyWneNotFirstPlaceSelected(int place) {
        processor.process(getCommandMessage(USER_2_ID, 10002, 12001, "15000__" + place));

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessages = messageCaptor.getValue();

        assertThat(actualMessages)
                .extracting(MessageDto::getTopicId, MessageDto::getChatId, MessageDto::getText)
                .containsExactly(null, "12001",
                        "В матче 15000 за вами зафиксировано *" + place + " место*\\." + TestConstants.EXTERNAL_LINE_SEPARATOR +
                        "При ошибке используйте команду '/resubmit 15000'\\." + TestConstants.EXTERNAL_LINE_SEPARATOR +
                        "*Теперь выберите лидера* которым играли\\.");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void shouldAttachLeaderKeyboardToMessageAboutCallbackAcceptOnCallbackReplyWneNotFirstPlaceSelected(int place) {
        processor.process(getCommandMessage(USER_2_ID, 10002, 11002, "15000__" + place));

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessages = messageCaptor.getValue();

        assertThat(actualMessages.getKeyboard())
                .flatExtracting(buttonDtos -> buttonDtos)
                .extracting(ButtonDto::getText, ButtonDto::getCallback)
                .containsExactly(
                        tuple("lead1", "10001_L_10000"),
                        tuple("lead2", "10001_L_10001"),
                        tuple("lead3", "10001_L_10002")
                );
    }

    @Test
    void shouldSendMessageAboutCallbackAcceptOnNotParticipatedCallbackReply() {
        processor.process(getCommandMessage(USER_2_ID, 10002, 12001, "15000__" + 0));

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessage = messageCaptor.getValue();

        assertThat(actualMessage)
                .extracting(MessageDto::getTopicId, MessageDto::getChatId, MessageDto::getText)
                .containsExactly(null, "12001",
                        "В матче 15000 за вами зафиксирован статус: *не участвует*\\." + TestConstants.EXTERNAL_LINE_SEPARATOR +
                        "При ошибке используйте команду '/resubmit 15000'\\.");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void shouldSendMessageAboutCallbackAcceptOnCallbackReplyWhenResubmitWasDoneEarlier(int place) {
        jdbcTemplate.execute("update match_players set external_submit_id = null where id between 10000 and 10003");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__" + place));

        verify(messagingService).sendMessageAsync(any());
    }

    @Test
    void shouldReturnAcceptSubmitCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.ACCEPT_SUBMIT);
    }

    private CommandMessage getCommandMessage(long userId, int messageId, long chatId, String callbackData) {
        User user = new User();
        user.setId(userId);
        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(messageId);
        message.setChat(chat);
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(message);
        callbackQuery.setFrom(user);
        callbackQuery.setData(callbackData);
        return CommandMessage.getCallbackInstance(callbackQuery);
    }
}
