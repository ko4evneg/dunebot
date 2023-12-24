package ru.trainithard.dunebot.service.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.exception.AnswerableDubeBotException;
import ru.trainithard.dunebot.model.Command;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TelegramUpdateMessageValidatorTest {
    private static final TelegramUpdateMessageValidator validator = new TelegramUpdateMessageValidator();
    private final Message message = new Message();
    private static final Long TELEGRAM_USER_ID = 12345L;
    private static final Long TELEGRAM_CHAT_ID = 9000L;

    @BeforeEach
    void beforeEach() {
        User user = new User();
        user.setId(TELEGRAM_USER_ID);
        Chat chat = new Chat();
        chat.setId(TELEGRAM_CHAT_ID);
        message.setFrom(user);
        message.setChat(chat);
    }

    @ParameterizedTest
    @EnumSource(value = Command.class, mode = EnumSource.Mode.EXCLUDE, names = {"REGISTER"})
    void shouldNotThrowForValidCommand(Command command) {
        message.setText("/" + command.name().toLowerCase());

        assertDoesNotThrow(() -> validator.validate(message));
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void shouldNotThrowForValidCommandWithUnusedArguments(Command command) {
        message.setText("/" + command.name().toLowerCase() + " arg1 arg2 arg3");

        assertDoesNotThrow(() -> validator.validate(message));
    }

    @ParameterizedTest
    @MethodSource("invalidCommandsProvider")
    void shouldThrowForInvalidCommand(String commandText, String expectedReply) {
        message.setText(commandText);

        AnswerableDubeBotException actualException = assertThrows(AnswerableDubeBotException.class, () -> validator.validate(message));
        assertEquals(expectedReply, actualException.getMessage());
    }

    private static Stream<Arguments> invalidCommandsProvider() {
        return Stream.of(
                Arguments.of("register", "Неверная команда!"),
                Arguments.of("register abc", "Неверная команда!"),
                Arguments.of("", "Неверная команда!"),
                Arguments.of("/", "Неверная команда!"),
                Arguments.of("/registe", "Неверная команда!"),
                Arguments.of("/registerX", "Неверная команда!"),
                Arguments.of("/register_X", "Неверная команда!"),
                Arguments.of("/register", "Неверный формат команды! Пример правильной команды: \"/register *steam_nickname*\"")
        );
    }
}
