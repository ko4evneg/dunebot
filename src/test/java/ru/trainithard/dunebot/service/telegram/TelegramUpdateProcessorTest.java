package ru.trainithard.dunebot.service.telegram;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.DubeBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.MatchCommandProcessor;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@SpringBootTest
class TelegramUpdateProcessorTest extends TestContextMock {
    @Autowired
    private TelegramUpdateProcessor updateProcessor;
    @MockBean
    private MatchCommandProcessor commandProcessor;

    private static final String COMMAND_UPRISING_4 = "/" + Command.NEW + " up4";
    private static final long TELEGRAM_USER_ID_1 = 10000L;
    private static final long TELEGRAM_USER_ID_2 = 10001L;
    private static final long TELEGRAM_CHAT_ID_1 = 9000L;
    private static final long TELEGRAM_CHAT_ID_2 = 9001L;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, " + TELEGRAM_USER_ID_1 + ", " + TELEGRAM_CHAT_ID_1 + " , 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10001, " + TELEGRAM_USER_ID_2 + ", " + TELEGRAM_CHAT_ID_2 + " , 'st_pl2', 'name2', '2010-10-10') ");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from players where id in(10000, 10001)");
    }

    @Test
    void shouldSendCommandToTextCommandProcessor() {
        when(telegramBot.poll()).thenReturn(getUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, COMMAND_UPRISING_4)).thenReturn(null);

        updateProcessor.process();

        verify(commandProcessor, times(1)).registerNewMatch(eq(10000L), eq(ModType.UPRISING_4));
    }

    @Test
    void shouldSendTelegramMessageOnWrongCommand() throws TelegramApiException {
        when(telegramBot.poll()).thenReturn(getUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, "/up")).thenReturn(null);

        updateProcessor.process();

        verify(telegramBot, times(1))
                .executeAsync(argThat((SendMessage sendMessage) -> "9000".equals(sendMessage.getChatId()) && "Неверная команда!".equals(sendMessage.getText())));
    }

    @Test
    @Disabled
    void shouldIncludeReplyMessageIdOnTopicWrongCommandReceive() {
        fail();
    }

    @ParameterizedTest
    @MethodSource("exceptionsProvider")
    void shouldNotThrowOnException(Class<? extends Exception> aClass) {
        when(telegramBot.poll()).thenReturn(getUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, "/up")).thenReturn(null);
        doThrow(aClass).when(commandProcessor).registerNewMatch(anyLong(), any());

        assertDoesNotThrow(() -> updateProcessor.process());
    }

    private static Stream<Arguments> exceptionsProvider() {
        return Stream.of(
                Arguments.of(RuntimeException.class),
                Arguments.of(DubeBotException.class),
                Arguments.of(NullPointerException.class)
        );
    }

    @Test
    void shouldMoveToNextUpdateOnException() {
        when(telegramBot.poll()).thenReturn(getUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, "/up")).thenReturn(getUpdate(TELEGRAM_USER_ID_2, TELEGRAM_CHAT_ID_2, COMMAND_UPRISING_4)).thenReturn(null);
        doThrow(RuntimeException.class).when(commandProcessor).registerNewMatch(anyLong(), any());

        updateProcessor.process();

        verify(commandProcessor, times(1)).registerNewMatch(eq(TELEGRAM_USER_ID_2), eq(ModType.UPRISING_4));
    }

    @Test
    void shouldMoveToNextUpdateWithoutException() {
        when(telegramBot.poll()).thenReturn(getUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, "/up")).thenReturn(getUpdate(TELEGRAM_USER_ID_2, TELEGRAM_CHAT_ID_2, COMMAND_UPRISING_4)).thenReturn(null);
        doNothing().when(commandProcessor).registerNewMatch(anyLong(), any());

        updateProcessor.process();

        verify(commandProcessor, times(1)).registerNewMatch(eq(TELEGRAM_USER_ID_2), eq(ModType.UPRISING_4));
    }

    private static Update getUpdate(long telegramUserId, long telegramChatId, String text) {
        User user = new User();
        user.setId(telegramUserId);
        Chat chat = new Chat();
        chat.setId(telegramChatId);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(10000);
        message.setChat(chat);
        message.setFrom(user);
        message.setText(text);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}
