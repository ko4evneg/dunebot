package ru.trainithard.dunebot.service.telegram.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.ChatType;
import ru.trainithard.dunebot.service.telegram.command.processor.SubmitCommandProcessor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class SubmitCommandProcessorTest extends TestContextMock {
    @Autowired
    private SubmitCommandProcessor commandProcessor;
    @MockBean
    private MessagingService messagingService;

    private static final long CHAT_ID_1 = 12000L;

    @BeforeEach
    void beforeEach() {
        doAnswer(new MockReplier()).when(messagingService).sendMessageAsync(any(MessageDto.class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 11000, '" + CHAT_ID_1 + "', 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10001, 11001, 12001, 'st_pl2', 'name2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10002, 11002, 12002, 'st_pl3', 'name3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10003, 11003, 12003, 'st_pl4', 'name4', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_message_id, owner_id, mod_type, positive_answers_count, created_at) " +
                "values (15000, '10000', '10000', 10000, '" + ModType.CLASSIC + "', 4, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10000, 15000, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10001, 15000, 10001, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10002, 15000, 10002, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10003, 15000, 10003, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
    }

    private CommandMessage getCommandMessage(long userId) {
        User user = new User();
        user.setId(userId);
        Chat chat = new Chat();
        chat.setId(CHAT_ID_1);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(10000);
        message.setFrom(user);
        message.setChat(chat);
        message.setText("/" + Command.SUBMIT.name() + " 15000");
        return new CommandMessage(message);
    }

    @ParameterizedTest
    @MethodSource("exceptionsSource")
    void shouldThrowOnFinishedMatchSubmit(String query, String expectedException) {
        jdbcTemplate.execute(query);

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> commandProcessor.process(getCommandMessage(11000L)));

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
    void shouldThrowOnAlienMatchSubmit() {
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> commandProcessor.process(getCommandMessage(11005L)));

        assertEquals("Вы не можете инициировать публикацию этого матча", actualException.getMessage());
    }

    @Test
    void shouldThrowOnNotExistentMatchSubmit() {
        jdbcTemplate.execute("delete from match_players where match_id = 15000");
        jdbcTemplate.execute("delete from matches where id = 15000");

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> commandProcessor.process(getCommandMessage(11000L)));

        assertEquals("Матча с таким ID не существует!", actualException.getMessage());
    }

    @Test
    void shouldSendMessagesToEveryMatchPlayer() {
        commandProcessor.process(getCommandMessage(11000L));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(4)).sendMessageAsync(messageDtoCaptor.capture());
        List<MessageDto> actualSendMessages = messageDtoCaptor.getAllValues();

        assertThat(actualSendMessages, containsInAnyOrder(
                hasProperty("chatId", is("12000")),
                hasProperty("chatId", is("12001")),
                hasProperty("chatId", is("12002")),
                hasProperty("chatId", is("12003")))
        );
    }

    @Test
    void shouldSendCorrectSubmitMessageMessage() {
        commandProcessor.process(getCommandMessage(11000L));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(4)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getAllValues().get(0);
        List<List<ButtonDto>> linedButtons = actualMessageDto.getKeyboard();

        assertEquals("Выберите место, которое вы заняли в матче 15000:", actualMessageDto.getText());
        assertThat(linedButtons.get(0), contains(
                both(hasProperty("text", is("1"))).and(hasProperty("callback", is("15000__1"))),
                both(hasProperty("text", is("2"))).and(hasProperty("callback", is("15000__2"))))
        );
        assertThat(linedButtons.get(1), contains(
                both(hasProperty("text", is("3"))).and(hasProperty("callback", is("15000__3"))),
                both(hasProperty("text", is("4"))).and(hasProperty("callback", is("15000__4"))))
        );
        assertThat(linedButtons.get(2), contains(
                both(hasProperty("text", is("не участвовал(а)"))).and(hasProperty("callback", is("15000__-1"))))
        );
    }

    @Test
    void shouldSaveSubmitMessageIdsToMatchPlayers() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(111001);
        Chat chat = new Chat();
        chat.setId(111002L);
        Message message = new Message();
        message.setMessageId(111000);
        message.setReplyToMessage(replyMessage);
        message.setChat(chat);

        doReturn(CompletableFuture.completedFuture(new ExternalMessageDto(message))).when(messagingService).sendMessageAsync(any(MessageDto.class));

        commandProcessor.process(getCommandMessage(11000L));

        Long assignedIdsPlayerCount = jdbcTemplate.queryForObject("select count(*) from match_players where " +
                "external_chat_id = '111002' and external_message_id = 111000 and external_reply_id = 111001", Long.class);

        assertEquals(4, assignedIdsPlayerCount);
    }

    @Test
    void shouldNotSaveSubmitMessageReplyIdToMatchPlayerFromPrivateChatSubmit() {
        Chat chat = new Chat();
        chat.setId(111001L);
        Message message = new Message();
        message.setMessageId(111000);
        message.setChat(chat);

        doReturn(CompletableFuture.completedFuture(new ExternalMessageDto(message))).when(messagingService).sendMessageAsync(any(MessageDto.class));

        commandProcessor.process(getCommandMessage(11000L));

        Long assignedIdsPlayerCount = jdbcTemplate.queryForObject("select count(*) from match_players where " +
                "external_chat_id = '111001' and external_message_id = 111000 and external_reply_id is null", Long.class);

        assertEquals(4, assignedIdsPlayerCount);
    }

    @Test
    void shouldNotSaveSubmitMessageIdsForOtherMatchPlayer() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10004, 11004, 12004, 'st_pl5', 'name5', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10004, 15000, 10004, '2010-10-10')");

        Chat chat = new Chat();
        chat.setId(111001L);
        Message message = new Message();
        message.setMessageId(111000);
        message.setChat(chat);

        doReturn(CompletableFuture.completedFuture(new ExternalMessageDto(message))).when(messagingService).sendMessageAsync(any(MessageDto.class));

        commandProcessor.process(getCommandMessage(11000L));

        List<Long> assignedIdsPlayers = jdbcTemplate.queryForList("select id from match_players where " +
                "external_chat_id = '111001' and external_message_id = 111000 and external_reply_id is null", Long.class);

        assertFalse(assignedIdsPlayers.contains(11004L));
    }

    private static class MockReplier implements Answer<CompletableFuture<ExternalMessageDto>> {
        private int externalId = 11000;
        private long chatId = CHAT_ID_1;

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