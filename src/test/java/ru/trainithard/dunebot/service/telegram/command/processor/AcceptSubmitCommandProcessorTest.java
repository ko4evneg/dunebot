package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.telegram.ChatType;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@SpringBootTest
class AcceptSubmitCommandProcessorTest extends TestContextMock {
    @Autowired
    private AcceptSubmitCommandProcessor processor;
    @MockBean
    private MatchFinishingService matchFinishingService;

    private static final long USER_1_ID = 11000L;
    private static final long USER_2_ID = 11001L;

    @BeforeEach
    void beforeEach() {
//        doAnswer(new SubmitCommandProcessorTest.MockReplier()).when(messagingService).sendMessageAsync(any(MessageDto.class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, " + USER_1_ID + ", 12000, 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10001, " + USER_2_ID + ", 12001, 'st_pl2', 'name2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10002, 11002, 12002, 'st_pl3', 'name3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10003, 11003, 12003, 'st_pl4', 'name4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, created_at) " +
                "values (10000, 'ExternalPollId', 10000, 10000, '10000', '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                "values (10001, 'ExternalMessageId', 10001, 10000, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, positive_answers_count, created_at) " +
                "values (15000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', 4, '2010-10-10') ");
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
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10005");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1, 4, 6})
    void shouldSetCandidatePlaceOnCallbackReply(int expectedPlace) {
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__" + expectedPlace));

        Integer actualCandidatePlace = jdbcTemplate.queryForObject("select candidate_place from match_players where id = 10000", Integer.class);

        assertEquals(expectedPlace, actualCandidatePlace);
    }

    @Test
    void shouldNotChangeAlreadySetCandidatePlaceOnCallbackReply() {
        jdbcTemplate.execute("update match_players set candidate_place = 1 where id = 10000");
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"));

        Integer actualCandidatePlace = jdbcTemplate.queryForObject("select candidate_place from match_players where id = 10000", Integer.class);

        assertEquals(1, actualCandidatePlace);
    }

    @Test
    void shouldIncreaseMatchSubmitCountOnCallbackReply() {
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__-1"));
        processor.process(getCommandMessage(USER_2_ID, 10003, 11003, "15000__3"));

        Integer actualCandidatePlace = jdbcTemplate.queryForObject("select submits_count from matches where id = 15000", Integer.class);

        assertEquals(2, actualCandidatePlace);
    }

    @Test
    void shouldInvokeMatchFinishOnLastCallbackReply() {
        jdbcTemplate.execute("update matches set submits_count = 3 where id = 15000");

        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"));

        verify(matchFinishingService, times(1)).finishMatch(eq(15000L));
    }

    @Test
    void shouldNotInvokeMatchFinishOnNotLastCallbackReply() {
        processor.process(getCommandMessage(USER_1_ID, 10002, 11002, "15000__2"));

        verify(matchFinishingService, never()).finishMatch(anyLong());
    }

    @Test
    void shouldNotInvokeMatchFinishOnLastConflictCallbackReply() {
        fail();
    }

    @Test
    void shouldInvokeMatchResubmitOnLastConflictCallbackReply() {
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
        return new CommandMessage(callbackQuery);
    }
}
