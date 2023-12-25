package ru.trainithard.dunebot.service.telegram;

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
import ru.trainithard.dunebot.service.TextCommandProcessor;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@SpringBootTest
class TelegramUpdateProcessorTest extends TestContextMock {
    @Autowired
    private TelegramUpdateProcessor updateProcessor;
    @MockBean
    private TextCommandProcessor commandProcessor;

    @Test
    void shouldSendCommandToTextCommandProcessor() {
        when(telegramBot.poll()).thenReturn(getUpdate(10000L, "/" + Command.UP4)).thenReturn(null);

        updateProcessor.process();

        verify(commandProcessor, times(1)).registerNewMatch(eq(10000L), eq(ModType.UPRISING_4));
    }

    @Test
    void shouldSendTelegramMessageOnWrongCommand() throws TelegramApiException {
        when(telegramBot.poll()).thenReturn(getUpdate(10000L, "/up")).thenReturn(null);

        updateProcessor.process();

        verify(telegramBot, times(1))
                .executeAsync(argThat((SendMessage sendMessage) -> "9000".equals(sendMessage.getChatId()) && "Неверная команда!".equals(sendMessage.getText())));
    }

    @ParameterizedTest
    @MethodSource("exceptionsProvider")
    void shouldNotThrowOnException(Class<? extends Exception> aClass) {
        when(telegramBot.poll()).thenReturn(getUpdate(10000L, "/up")).thenReturn(null);
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
        when(telegramBot.poll()).thenReturn(getUpdate(10000L, "/up")).thenReturn(getUpdate(3333L, "/up4")).thenReturn(null);
        doThrow(RuntimeException.class).when(commandProcessor).registerNewMatch(anyLong(), any());

        updateProcessor.process();

        verify(commandProcessor, times(1)).registerNewMatch(eq(3333L), eq(ModType.UPRISING_4));
    }

    @Test
    void shouldMoveToNextUpdateWithoutException() {
        when(telegramBot.poll()).thenReturn(getUpdate(10000L, "/up")).thenReturn(getUpdate(3333L, "/up4")).thenReturn(null);
        doNothing().when(commandProcessor).registerNewMatch(anyLong(), any());

        updateProcessor.process();

        verify(commandProcessor, times(1)).registerNewMatch(eq(3333L), eq(ModType.UPRISING_4));
    }

    private static Update getUpdate(long telegramUserId, String text) {
        User user = new User();
        user.setId(telegramUserId);
        Chat chat = new Chat();
        chat.setId(9000L);
        Message message = new Message();
        message.setChat(chat);
        message.setFrom(user);
        message.setText(text);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}
