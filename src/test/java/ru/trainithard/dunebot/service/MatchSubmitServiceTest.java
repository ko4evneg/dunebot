package ru.trainithard.dunebot.service;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class MatchSubmitServiceTest extends TestContextMock {
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
        jdbcTemplate.execute("delete from players where id between 10000 and 10005");
    }

    @Test
    void shouldThrowOnFinishedMatchSubmit() {
        jdbcTemplate.execute("update matches set is_finished = true where id = 15000");

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> matchCommandProcessor.getSubmitMessage(11000L, 12000L, "15000"));

        assertEquals("Запрещено регистрировать результаты завершенных матчей", actualException.getMessage());
    }

    @Test
    void shouldThrowOnAlreadySubmittingMatchSubmit() {
        jdbcTemplate.execute("update matches set is_onsubmit = true where id = 15000");

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> matchCommandProcessor.getSubmitMessage(11000L, 12000L, "15000"));

        assertEquals("Запрос на публикацию этого матча уже сделан", actualException.getMessage());
    }

    @Test
    void shouldThrowOnNotFullMatchSubmit() {
        jdbcTemplate.execute("update matches set positive_answers_count = 3 where id = 15000");

        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> matchCommandProcessor.getSubmitMessage(11000L, 12000L, "15000"));

        assertEquals("В опросе участвует меньше игроков чем нужно для матча. Все игроки должны войти в опрос", actualException.getMessage());
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
