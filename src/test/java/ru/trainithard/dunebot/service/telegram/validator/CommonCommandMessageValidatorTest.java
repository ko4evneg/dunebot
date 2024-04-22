package ru.trainithard.dunebot.service.telegram.validator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CommonCommandMessageValidatorTest extends TestContextMock {
    private static final String PUBLIC_CHAT_PROHIBITED_COMMAND_TEXT = "Команда запрещена в групповых чатах - напишите боту напрямую.";
    private static final String ANONYMOUS_COMMAND_TEXT = "Команду могут выполнять только зарегистрированные игроки! Для регистрации выполните команду '/register *steam_name*'";

    @Autowired
    private CommonCommandMessageValidator validator;

    private static final Long TELEGRAM_USER_ID = 12345L;
    private static final Long TELEGRAM_CHAT_ID = 9000L;
    private final Message message = new Message();

    @BeforeEach
    void beforeEach() {
        fillMessage();
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + TELEGRAM_USER_ID + ", " + TELEGRAM_CHAT_ID + " , 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from players where id = 10000");
    }

    @Test
    void shouldCorrectlyFillChatIdAndReplyIdForPersonalMessage() {
        message.setText("/fake_command");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

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

        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> validator.validate(commandMessage));
        assertEquals(TELEGRAM_CHAT_ID, actualException.getTelegramChatId());
        assertEquals(9001, actualException.getTelegramReplyId());
    }

    @ParameterizedTest
    @EnumSource(value = Command.class, mode = EnumSource.Mode.INCLUDE, names = {"CANCEL"})
    void shouldNotThrowForValidCommandWithoutArguments(Command command) {
        message.setText("/" + command.name().toLowerCase());

        assertDoesNotThrow(() -> validator.validate(CommandMessage.getMessageInstance(message)));
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void shouldNotThrowForValidCommandWithUnusedArguments(Command command) {
        message.getChat().setType(ChatType.PRIVATE.getValue());
        message.setText("/" + command.name().toLowerCase() + " arg1 arg2 arg3");

        assertDoesNotThrow(() -> validator.validate(CommandMessage.getMessageInstance(message)));
    }

    @ParameterizedTest
    @MethodSource("invalidCommandsProvider")
    void shouldThrowForInvalidCommand(String commandText, String expectedReply) {
        message.setText(commandText);

        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);
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
                Arguments.of("/register_X", "Неверная команда!")
        );
    }

    @ParameterizedTest
    @MethodSource("nonAnonymousCommandSource")
    void shouldThrowForAnonymousCallOfNonAnonymousCommand(Command command) {
        jdbcTemplate.execute("delete from players where id = 10000");
        message.setText("/" + command.name().toLowerCase() + " arg1 arg2 arg3");

        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> validator.validate(commandMessage));
        assertEquals(ANONYMOUS_COMMAND_TEXT, actualException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("nonAnonymousCommandSource")
    void shouldThrowForGuestCallOfNonAnonymousCommand(Command command) {
        jdbcTemplate.execute("update players set is_guest = true where id = 10000");
        message.setText("/" + command.name().toLowerCase() + " arg1 arg2 arg3");

        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> validator.validate(commandMessage));
        assertEquals(ANONYMOUS_COMMAND_TEXT, actualException.getMessage());
    }

    public static Stream<Arguments> nonAnonymousCommandSource() {
        return Arrays.stream(Command.values())
                .filter(command -> !command.isAnonymous())
                .map(Arguments::of);
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldThrowOnTextCommandsInNonPrivateChat(ChatType chatType) {
        message.getChat().setType(chatType.getValue());
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");

        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> validator.validate(commandMessage));
        assertEquals(PUBLIC_CHAT_PROHIBITED_COMMAND_TEXT, actualException.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldThrowOnTextCommandsInPublicChat(ChatType chatType) {
        message.getChat().setType(chatType.getValue());
        message.setText("/" + Command.SUBMIT.name().toLowerCase() + " 1");

        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class, () -> validator.validate(commandMessage));
        assertEquals(PUBLIC_CHAT_PROHIBITED_COMMAND_TEXT, actualException.getMessage());
    }

    @Test
    void shouldNotThrowOnPollAnswerCommandsInPublicChat() {
        PollAnswer pollAnswer = new PollAnswer();
        pollAnswer.setUser(getUser());
        pollAnswer.setOptionIds(Collections.singletonList(0));
        pollAnswer.setPollId("100001");
        Update update = new Update();
        update.setPollAnswer(pollAnswer);
        CommandMessage pollAnswerMessage = CommandMessage.getPollAnswerInstance(pollAnswer);

        assertDoesNotThrow(() -> validator.validate(pollAnswerMessage));
    }

    @Test
    void shouldNotThrowOnCallbackCommandsInPublicChat() {
        Message message = new Message();
        message.setMessageId(123);
        message.setFrom(getUser());
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(message);
        callbackQuery.setData("10000__-1");
        callbackQuery.setFrom(getUser());

        CommandMessage callbackMessage = CommandMessage.getCallbackInstance(callbackQuery);

        assertDoesNotThrow(() -> validator.validate(callbackMessage));
    }

    private void fillMessage() {
        Chat chat = new Chat();
        chat.setId(TELEGRAM_CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        message.setMessageId(10000);
        message.setFrom(getUser());
        message.setChat(chat);
    }

    private User getUser() {
        User user = new User();
        user.setId(TELEGRAM_USER_ID);
        return user;
    }
}
