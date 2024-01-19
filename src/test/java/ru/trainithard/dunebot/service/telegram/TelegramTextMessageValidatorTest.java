package ru.trainithard.dunebot.service.telegram;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TelegramTextMessageValidatorTest extends TestContextMock {
    @Autowired
    private TelegramTextCommandValidator validator;

    private static final Long TELEGRAM_USER_ID = 12345L;
    private static final Long TELEGRAM_CHAT_ID = 9000L;
    private final Message message = new Message();

    @BeforeEach
    void beforeEach() {
        fillMessage();
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, " + TELEGRAM_USER_ID + ", " + TELEGRAM_CHAT_ID + " , 'st_pl1', 'name1', '2010-10-10') ");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from players where id = 10000");
    }

    private void fillMessage() {
        User user = new User();
        user.setId(TELEGRAM_USER_ID);
        Chat chat = new Chat();
        chat.setId(TELEGRAM_CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        message.setMessageId(10000);
        message.setFrom(user);
        message.setChat(chat);
    }

    @Test
    void shouldCorrectlyFillChatIdAndReplyIdForPersonalMessage() {
        message.setText("/fake_command");

        CommandMessage commandMessage = new CommandMessage(message);
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> validator.validate(commandMessage));
        assertEquals(TELEGRAM_CHAT_ID, actualException.getTelegramChatId());
        assertNull(actualException.getTelegramReplyId());
    }

    @Test
    void shouldCorrectlyFillChatIdAndReplyIdForTopicMessage() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(9001);
        message.setText("/fake_command");
        message.setReplyToMessage(replyMessage);

        CommandMessage commandMessage = new CommandMessage(message);
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> validator.validate(commandMessage));
        assertEquals(TELEGRAM_CHAT_ID, actualException.getTelegramChatId());
        assertEquals(9001, actualException.getTelegramReplyId());
    }

    @ParameterizedTest
    @EnumSource(value = Command.class, mode = EnumSource.Mode.INCLUDE, names = {"CANCEL"})
    void shouldNotThrowForValidCommandWithoutArguments(Command command) {
        message.setText("/" + command.name().toLowerCase());

        assertDoesNotThrow(() -> validator.validate(new CommandMessage(message)));
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void shouldNotThrowForValidCommandWithUnusedArguments(Command command) {
        message.getChat().setType(ChatType.PRIVATE.getValue());
        message.setText("/" + command.name().toLowerCase() + " arg1 arg2 arg3");

        assertDoesNotThrow(() -> validator.validate(new CommandMessage(message)));
    }

    @ParameterizedTest
    @MethodSource("invalidCommandsProvider")
    void shouldThrowForInvalidCommand(String commandText, String expectedReply) {
        message.setText(commandText);

        CommandMessage commandMessage = new CommandMessage(message);
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> validator.validate(commandMessage));
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
                Arguments.of("/register", "Данная команда должна иметь как минимум один аргумент. Например '/register *steam_name*'")
        );
    }

    @ParameterizedTest
    @EnumSource(value = Command.class, mode = EnumSource.Mode.EXCLUDE, names = {"REGISTER"})
    void shouldThrowForAnonymousCallOfNonAnonymousCommand(Command command) {
        jdbcTemplate.execute("delete from players where id = 10000");
        message.setText("/" + command.name().toLowerCase() + " arg1 arg2 arg3");

        CommandMessage commandMessage = new CommandMessage(message);
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> validator.validate(commandMessage));
        assertEquals("Команду могут выполнять только зарегистрированные игроки! Для регистрации выполните команду '/register *steam_name*'", actualException.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldThrowOnCommandsInNonPrivateChat(ChatType chatType) {
        message.getChat().setType(chatType.getValue());
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");

        CommandMessage commandMessage = new CommandMessage(message);
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> validator.validate(commandMessage));
        assertEquals("Команда запрещена в групповых чатах - напишите боту напрямую.", actualException.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldThrowOnCommandsInPublicChat(ChatType chatType) {
        message.getChat().setType(chatType.getValue());
        message.setText("/" + Command.SUBMIT.name().toLowerCase() + " 1");

        CommandMessage commandMessage = new CommandMessage(message);
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> validator.validate(commandMessage));
        assertEquals("Команда запрещена в групповых чатах - напишите боту напрямую.", actualException.getMessage());
    }
}
