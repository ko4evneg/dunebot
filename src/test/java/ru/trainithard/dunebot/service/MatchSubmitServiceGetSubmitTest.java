package ru.trainithard.dunebot.service;

import lombok.SneakyThrows;
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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.dto.TelegramUserPollDto;

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
class MatchSubmitServiceGetSubmitTest extends TestContextMock {
    @Autowired
    private MatchCommandProcessor matchCommandProcessor;

    private static final TelegramUserPollDto POLL_MESSAGE_DTO = new TelegramUserPollDto(12349L, "100500", 4);

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        doAnswer(new MockReplier()).when(telegramBot).executeAsync(any(SendMessage.class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 11000, 12000, 'st_pl1', 'name1', '2010-10-10') ");
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

    @ParameterizedTest
    @MethodSource("exceptionsSource")
    void shouldThrowOnFinishedMatchSubmit(String query, String expectedException) {
        jdbcTemplate.execute(query);

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> matchCommandProcessor.getSubmitMessage(11000L, 12000L, "15000"));

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
                () -> matchCommandProcessor.getSubmitMessage(11005L, 12000L, "15000"));

        assertEquals("Вы не можете инициировать публикацию этого матча", actualException.getMessage());
    }

    @Test
    void shouldThrowOnNotExistentMatchSubmit() {
        jdbcTemplate.execute("delete from match_players where match_id = 15000");
        jdbcTemplate.execute("delete from matches where id = 15000");

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> matchCommandProcessor.getSubmitMessage(11000L, 12000L, "15000"));

        assertEquals("Матча с таким ID не существует!", actualException.getMessage());
    }

    @Test
    void shouldSendMessagesToEveryMatchPlayer() throws TelegramApiException {
        matchCommandProcessor.getSubmitMessage(11000L, 12000L, "15000");

        ArgumentCaptor<SendMessage> sendMessageArgumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(4)).executeAsync(sendMessageArgumentCaptor.capture());
        List<SendMessage> actualSendMessages = sendMessageArgumentCaptor.getAllValues();


        assertThat(actualSendMessages, containsInAnyOrder(
                hasProperty("chatId", is("12000")),
                hasProperty("chatId", is("12001")),
                hasProperty("chatId", is("12002")),
                hasProperty("chatId", is("12003")))
        );
    }

    @Test
    void shouldSendCorrectSubmitMessageMessage() throws TelegramApiException {
        matchCommandProcessor.getSubmitMessage(11000L, 12000L, "15000");

        ArgumentCaptor<SendMessage> sendMessageArgumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(4)).executeAsync(sendMessageArgumentCaptor.capture());
        SendMessage actualSendMessage = sendMessageArgumentCaptor.getAllValues().get(0);
        InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) actualSendMessage.getReplyMarkup();
        List<List<InlineKeyboardButton>> linedButtons = replyMarkup.getKeyboard();

        assertEquals("Выберите место, которое вы заняли в матче 15000:", actualSendMessage.getText());
        assertThat(linedButtons.get(0), contains(
                both(hasProperty("text", is("1"))).and(hasProperty("callbackData", is("15000__1"))),
                both(hasProperty("text", is("2"))).and(hasProperty("callbackData", is("15000__2"))))
        );
        assertThat(linedButtons.get(1), contains(
                both(hasProperty("text", is("3"))).and(hasProperty("callbackData", is("15000__3"))),
                both(hasProperty("text", is("4"))).and(hasProperty("callbackData", is("15000__4"))))
        );
        assertThat(linedButtons.get(2), contains(
                both(hasProperty("text", is("не участвовал(а)"))).and(hasProperty("callbackData", is("15000__-1"))))
        );
    }

    @Test
    void shouldSaveSubmitMessageIdsToMatchPlayers() throws TelegramApiException {
        Message replyMessage = new Message();
        replyMessage.setMessageId(111001);
        Chat chat = new Chat();
        chat.setId(111002L);
        Message message = new Message();
        message.setMessageId(111000);
        message.setReplyToMessage(replyMessage);
        message.setChat(chat);

        doReturn(CompletableFuture.completedFuture(message)).when(telegramBot).executeAsync(any(SendMessage.class));

        matchCommandProcessor.getSubmitMessage(11000L, 12000L, "15000");

        Long assignedIdsPlayerCount = jdbcTemplate.queryForObject("select count(*) from match_players where " +
                "external_chat_id = '111002' and external_message_id = 111000 and external_reply_id = 111001", Long.class);

        assertEquals(4, assignedIdsPlayerCount);
    }

    @Test
    void shouldNotSaveSubmitMessageReplyIdToMatchPlayerFromPrivateChatSubmit() throws TelegramApiException {
        Chat chat = new Chat();
        chat.setId(111001L);
        Message message = new Message();
        message.setMessageId(111000);
        message.setChat(chat);

        doReturn(CompletableFuture.completedFuture(message)).when(telegramBot).executeAsync(any(SendMessage.class));

        matchCommandProcessor.getSubmitMessage(11000L, 12000L, "15000");

        Long assignedIdsPlayerCount = jdbcTemplate.queryForObject("select count(*) from match_players where " +
                "external_chat_id = '111001' and external_message_id = 111000 and external_reply_id is null", Long.class);

        assertEquals(4, assignedIdsPlayerCount);
    }

    @Test
    void shouldNotSaveSubmitMessageIdsForOtherMatchPlayer() throws TelegramApiException {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10004, 11004, 12004, 'st_pl5', 'name5', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10004, 15000, 10004, '2010-10-10')");

        Chat chat = new Chat();
        chat.setId(111001L);
        Message message = new Message();
        message.setMessageId(111000);
        message.setChat(chat);

        doReturn(CompletableFuture.completedFuture(message)).when(telegramBot).executeAsync(any(SendMessage.class));

        matchCommandProcessor.getSubmitMessage(11000L, 12000L, "15000");

        List<Long> assignedIdsPlayers = jdbcTemplate.queryForList("select id from match_players where " +
                "external_chat_id = '111001' and external_message_id = 111000 and external_reply_id is null", Long.class);

        assertFalse(assignedIdsPlayers.contains(11004L));
    }

    private static class MockReplier implements Answer<CompletableFuture<Message>> {
        private int externalId = 11000;
        private long chatId = 12000L;

        @Override
        public CompletableFuture<Message> answer(InvocationOnMock invocationOnMock) {
            Chat chat = new Chat();
            chat.setId(chatId++);
            Message message = new Message();
            message.setMessageId(externalId++);
            message.setChat(chat);
            return CompletableFuture.completedFuture(message);
        }
    }
}
