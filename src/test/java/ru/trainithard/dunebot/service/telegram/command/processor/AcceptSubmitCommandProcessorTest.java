package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@SpringBootTest
class AcceptSubmitCommandProcessorTest extends TestContextMock {
    private static final ExternalMessage UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE = new ExternalMessage()
            .appendBold("Матч 15000")
            .append(" завершен без результата, так как превышено максимальное количество попыток регистрации мест");
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
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, positive_answers_count, has_onsubmit_photo, state, created_at) " +
                             "values (15000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', 4, true, '" + MatchState.NEW + "','2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10002, 'ExternalMessageId', 10002, 11002, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10003, 'ExternalMessageId', 10003, 11003, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10004, 'ExternalMessageId', 10004, 11004, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10005, 'ExternalMessageId', 10005, 11005, '2020-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                             "values (10000, 15000, 10000, 10002, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                             "values (10001, 15000, 10001, 10003, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                             "values (10002, 15000, 10002, 10004, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                             "values (10003, 15000, 10003, 10005, '2010-10-10')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) " +
                             "values (10000, '" + SettingKey.RESUBMITS_LIMIT + "', 3, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from settings where id = 10000");
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10005");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 4, 6})
    void shouldSetCandidatePlaceOnCallbackReply(int expectedPlace) {
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__" + expectedPlace), mockLoggingId);

        Integer actualCandidatePlace = jdbcTemplate.queryForObject("select candidate_place from match_players where id = 10000", Integer.class);

        assertEquals(expectedPlace, actualCandidatePlace);
    }

    @Test
    void shouldNotChangeAlreadySetCandidatePlaceOnCallbackReply() {
        jdbcTemplate.execute("update match_players set candidate_place = 1 where id = 10000");
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"), mockLoggingId);

        Integer actualCandidatePlace = jdbcTemplate.queryForObject("select candidate_place from match_players where id = 10000", Integer.class);

        assertEquals(1, actualCandidatePlace);
    }

    @Test
    void shouldIncreaseMatchSubmitCountOnCallbackReply() {
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__-1"), mockLoggingId);
        processor.process(getCommandMessage(USER_2_ID, 10003, 11003, "15000__3"), mockLoggingId);

        Integer actualCandidatePlace = jdbcTemplate.queryForObject("select submits_count from matches where id = 15000", Integer.class);

        assertEquals(2, actualCandidatePlace);
    }

    @Test
    void shouldInvokeMatchFinishOnLastCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3 where id = 15000");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"), mockLoggingId);

        verify(matchFinishingService, times(1)).finishSubmittedMatch(eq(15000L), anyInt());
    }

    @Test
    void shouldNotInvokeMatchFinishOnLastCallbackReplyWhenMatchHasNoPhoto() {
        jdbcTemplate.execute("update matches set submits_count = 3, has_onsubmit_photo = false where id = 15000");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"), mockLoggingId);

        verify(matchFinishingService, never()).finishSubmittedMatch(eq(15000L), anyInt());
    }

    @Test
    void shouldNotInvokeMatchFinishOnNotLastCallbackReply() {
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"), mockLoggingId);

        verify(matchFinishingService, never()).finishSubmittedMatch(anyInt(), anyInt());
    }

    @Test
    void shouldNotInvokeMatchFinishOnLastConflictCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 2 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"), mockLoggingId);

        verify(matchFinishingService, never()).finishSubmittedMatch(eq(15000L), anyInt());
    }

    @Test
    void shouldNotInvokeUnsuccessfulMatchFinishOnLastConflictCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 2 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"), mockLoggingId);

        verify(matchFinishingService, never()).finishNotSubmittedMatch(eq(15000L), eq(UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE), anyInt());
    }

    @Test
    void shouldInvokeMatchResubmitOnLastConflictCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 2 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"), mockLoggingId);

        verify(resubmitCommandProcessor, times(1))
                .process(argThat((Match match) -> match.getId().equals(15000L)), anyInt());
    }

    @Test
    void shouldNotInvokeMatchResubmitOnResubmitExceedLastConflictCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3, submits_retry_count = " + TestConstants.RESUBMITS_LIMIT + " where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 2 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"), mockLoggingId);

        verify(resubmitCommandProcessor, never()).process(argThat((Match match) -> match.getId().equals(15000L)), anyInt());
    }

    @Test
    void shouldSendMessageAboutResubmitOnLastConflictCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3 where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 2 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"), mockLoggingId);

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(4)).sendMessageAsync(messageCaptor.capture());
        List<MessageDto> actualMessages = messageCaptor.getAllValues();

        String conflictText = """
                Некоторые игроки не смогли поделить *2 место*:
                name2 \\(st\\_pl2\\) l2
                name1 \\(st\\_pl1\\) l1

                Повторный опрос результата\\.\\.\\.""";
        assertThat(actualMessages, not(hasItem(hasProperty("text", not(is(conflictText))))));
        assertThat(actualMessages, containsInAnyOrder(
                hasProperty("chatId", is("12000")), hasProperty("chatId", is("12001")),
                hasProperty("chatId", is("12002")), hasProperty("chatId", is("12003"))
        ));
    }

    @Test
    void shouldSendMessageAboutResubmitExceedLimitOnResubmitExceedLastConflictCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3, submits_retry_count = " + TestConstants.RESUBMITS_LIMIT + " where id = 15000");
        jdbcTemplate.execute("update match_players set candidate_place = 2 where id = 10001");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"), mockLoggingId);

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(4)).sendMessageAsync(messageCaptor.capture());
        List<MessageDto> actualMessages = messageCaptor.getAllValues();

        String conflictText = "Игроки не смогли верно обозначить свои места\\! Превышено количество запросов на регистрацию результатов\\. " +
                              "Результаты не сохранены, регистрация запрещена\\.";
        assertThat(actualMessages, not(hasItem(hasProperty("text", not(is(conflictText))))));
        assertThat(actualMessages, containsInAnyOrder(
                hasProperty("chatId", is("12000")), hasProperty("chatId", is("12001")),
                hasProperty("chatId", is("12002")), hasProperty("chatId", is("12003"))
        ));
    }

    @Test
    void shouldSendDeleteMessageForSubmitMessageOnCallbackReply() {
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__1"), mockLoggingId);

        verify(messagingService, times(1)).deleteMessageAsync(argThat(messageDto ->
                messageDto.getMessageId().equals(10002) && messageDto.getChatId().equals(11002L)));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    void shouldSendMessageAboutCallbackAcceptOnCallbackReply(int place) {
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__" + place), mockLoggingId);

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessages = messageCaptor.getValue();

        assertNull(actualMessages.getReplyMessageId());
        assertEquals("11002", actualMessages.getChatId());
        assertEquals("В матче 15000 за вами зафиксировано *" + place + " место*\\." + TestConstants.EXTERNAL_LINE_SEPARATOR +
                     "При ошибке используйте команду '/resubmit 15000'\\.", actualMessages.getText());
    }

    @Test
    void shouldSendMessageAboutCallbackAcceptOnCallbackReply() {
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__" + 1), mockLoggingId);

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessages = messageCaptor.getValue();

        assertNull(actualMessages.getReplyMessageId());
        assertEquals("11002", actualMessages.getChatId());
        assertEquals("В матче 15000 за вами зафиксировано *1 место*\\." + TestConstants.EXTERNAL_LINE_SEPARATOR +
                     "При ошибке используйте команду '/resubmit 15000'\\." + TestConstants.EXTERNAL_LINE_SEPARATOR +
                     "Теперь загрузите в этот чат скриншот победы\\.", actualMessages.getText());
    }

    @Test
    void shouldReturnAcceptSubmitCommand() {
        Command actualCommand = processor.getCommand();

        assertEquals(Command.ACCEPT_SUBMIT, actualCommand);
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
