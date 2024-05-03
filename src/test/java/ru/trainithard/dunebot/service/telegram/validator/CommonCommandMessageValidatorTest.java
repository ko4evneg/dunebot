package ru.trainithard.dunebot.service.telegram.validator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class CommonCommandMessageValidatorTest extends TestContextMock {
    private static final String PUBLIC_CHAT_PROHIBITED_COMMAND_TEXT = "Команда запрещена в групповых чатах - напишите боту напрямую.";
    private static final String ANONYMOUS_COMMAND_TEXT =
            "Команду могут выполнять только зарегистрированные игроки! Для регистрации выполните команду '/register *steam_name*'";
    private static final String BOT_NOT_CONFIGURED_TEXT = "Бот не настроен. Разрешены только административные команды.";
    private static final Long TELEGRAM_USER_ID = 12345L;
    private static final Long TELEGRAM_CHAT_ID = 9000L;

    @Autowired
    private CommonCommandMessageValidator validator;
    private final Message message = new Message();

    @BeforeEach
    void beforeEach() {
        fillMessage();
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + TELEGRAM_USER_ID + ", " + TELEGRAM_CHAT_ID + " , 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) values (10000, 'CHAT_ID', 'strVal', '2010-01-02')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) values (10001, 'TOPIC_ID_CLASSIC', '9000', '2010-01-02')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) values (10002, 'TOPIC_ID_UPRISING', '9001', '2010-01-02')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from players where id = 10000");
        jdbcTemplate.execute("delete from settings where id between 10000 and 10002");
    }

    @Test
    void shouldCorrectlyFillChatIdAndReplyIdForPersonalMessage() {
        message.setText("/cancel");
        message.setMessageId(9001);
        message.getFrom().setId(100500L);
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatThrownBy(() -> validator.validate(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasFieldOrPropertyWithValue("telegramChatId", TELEGRAM_CHAT_ID)
                .hasFieldOrPropertyWithValue("telegramReplyId", null);
    }

    @Test
    void shouldCorrectlyFillChatIdAndReplyIdForTopicMessage() {
        Message replyMessage = new Message();
        message.setText("/cancel");
        message.getFrom().setId(100500L);
        replyMessage.setMessageId(9001);
        message.setReplyToMessage(replyMessage);
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatThrownBy(() -> validator.validate(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasFieldOrPropertyWithValue("telegramChatId", TELEGRAM_CHAT_ID)
                .hasFieldOrPropertyWithValue("telegramReplyId", 9001);
    }

    @ParameterizedTest
    @EnumSource(value = Command.class, mode = EnumSource.Mode.INCLUDE, names = {"CANCEL"})
    void shouldNotThrowForValidCommandWithoutArguments(Command command) {
        message.setText("/" + command.name().toLowerCase());

        assertThatCode(() -> validator.validate(CommandMessage.getMessageInstance(message))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void shouldNotThrowForValidCommandWithUnusedArguments(Command command) {
        message.getChat().setType(ChatType.PRIVATE.getValue());
        message.setText("/" + command.name().toLowerCase() + " arg1 arg2 arg3");

        assertThatCode(() -> validator.validate(CommandMessage.getMessageInstance(message))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("invalidCommandsProvider")
    void shouldReturnFalseForInvalidCommand(String commandText, String expectedReply) {
        message.setText(commandText);
        message.setMessageThreadId(9000);
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        boolean actualIsProcessingRequired = validator.validate(commandMessage);

        assertThat(actualIsProcessingRequired).withFailMessage(expectedReply).isFalse();
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

        assertThatThrownBy(() -> validator.validate(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(ANONYMOUS_COMMAND_TEXT);
    }

    @ParameterizedTest
    @MethodSource("nonAnonymousCommandSource")
    void shouldThrowForGuestCallOfNonAnonymousCommand(Command command) {
        jdbcTemplate.execute("update players set is_guest = true where id = 10000");
        message.setText("/" + command.name().toLowerCase() + " arg1 arg2 arg3");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatThrownBy(() -> validator.validate(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(ANONYMOUS_COMMAND_TEXT);
    }

    public static Stream<Arguments> nonAnonymousCommandSource() {
        return Arrays.stream(Command.values())
                .filter(command -> !command.isAnonymous())
                .map(Arguments::of);
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldThrowOnTextCommandsInPublicChat(ChatType chatType) {
        message.getChat().setType(chatType.getValue());
        message.setText("/" + Command.SUBMIT.name().toLowerCase() + " 1");
        message.setMessageThreadId(9000);
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatThrownBy(() -> validator.validate(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(PUBLIC_CHAT_PROHIBITED_COMMAND_TEXT);
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

        assertThatCode(() -> validator.validate(pollAnswerMessage)).doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowOnCallbackCommandsInPublicChat() {
        Message messageInfo = new Message();
        messageInfo.setMessageId(123);
        messageInfo.setFrom(getUser());
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(messageInfo);
        callbackQuery.setData("10000__-1");
        callbackQuery.setFrom(getUser());

        CommandMessage callbackMessage = CommandMessage.getCallbackInstance(callbackQuery);

        assertThatCode(() -> validator.validate(callbackMessage)).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowForNonAdminCommandWhenChatIdIsNotConfigured_PrivateChat() {
        jdbcTemplate.execute("delete from settings where id = 10000");
        message.getChat().setType(ChatType.PRIVATE.getValue());
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatThrownBy(() -> validator.validate(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(BOT_NOT_CONFIGURED_TEXT);
    }

    @ParameterizedTest
    @ValueSource(ints = {10001, 10002})
    void shouldThrowForNonAdminCommandWhenTopicIdIsNotConfigured_PrivateChat(int topicDatabaseRowId) {
        jdbcTemplate.execute("delete from settings where id = " + topicDatabaseRowId);
        message.getChat().setType(ChatType.PRIVATE.getValue());
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatThrownBy(() -> validator.validate(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(BOT_NOT_CONFIGURED_TEXT);
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class)
    void shouldReturnTrueForAdminCommandWhenChatAndTopicAreNotConfigured(ChatType chatType) {
        jdbcTemplate.execute("delete from settings where id between 10000 and 10002");
        message.getChat().setType(chatType.getValue());
        message.setText("/" + Command.ADMIN.name().toLowerCase());
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        boolean actualIsProcessingRequired = validator.validate(commandMessage);
        assertThat(actualIsProcessingRequired).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldReturnFalseForNonAdminCommandWhenChatIsNotConfigured_PublicChat(ChatType chatType) {
        jdbcTemplate.execute("delete from settings where id = 10000");
        message.getChat().setType(chatType.getValue());
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        boolean actualIsProcessingRequired = validator.validate(commandMessage);
        assertThat(actualIsProcessingRequired).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldReturnFalseForNonAdminCommandWhenTopicIdsAreNotConfigured_PublicChat(ChatType chatType) {
        jdbcTemplate.execute("delete from settings where id between 10001 and 10002");
        message.getChat().setType(chatType.getValue());
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        boolean actualIsProcessingRequired = validator.validate(commandMessage);
        assertThat(actualIsProcessingRequired).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldReturnFalseForNonAdminCommandWhenChatIsNotConfigured_PublicChat_Document(ChatType chatType) {
        jdbcTemplate.execute("delete from settings where id = 10000");
        message.setDocument(new Document());
        message.getChat().setType(chatType.getValue());
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        boolean actualIsProcessingRequired = validator.validate(commandMessage);
        assertThat(actualIsProcessingRequired).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldReturnFalseForNonAdminCommandWhenTopicIdsAreNotConfigured_PublicChat_Document(ChatType chatType) {
        jdbcTemplate.execute("delete from settings where id between 10001 and 10002");
        message.getChat().setType(chatType.getValue());
        message.setDocument(new Document());
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        boolean actualIsProcessingRequired = validator.validate(commandMessage);
        assertThat(actualIsProcessingRequired).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldReturnFalseForNonAdminCommandInNonBotTopic_Text(ChatType chatType) {
        jdbcTemplate.execute("delete from settings where id between 10001 and 10002");
        message.getChat().setType(chatType.getValue());
        message.setMessageThreadId(93124);
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        boolean actualIsProcessingRequired = validator.validate(commandMessage);
        assertThat(actualIsProcessingRequired).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldReturnTrueForNonAdminCommandInBotTopic_Text(ChatType chatType) {
        jdbcTemplate.execute("delete from settings where id between 10001 and 10002");
        message.getChat().setType(chatType.getValue());
        message.setMessageThreadId(9000);
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        boolean actualIsProcessingRequired = validator.validate(commandMessage);
        assertThat(actualIsProcessingRequired).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldReturnFalseForNonAdminCommandInNonBotTopic_Document(ChatType chatType) {
        jdbcTemplate.execute("delete from settings where id between 10001 and 10002");
        message.getChat().setType(chatType.getValue());
        message.setDocument(new Document());
        message.setMessageThreadId(93124);
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        boolean actualIsProcessingRequired = validator.validate(commandMessage);
        assertThat(actualIsProcessingRequired).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldReturnTrueForNonAdminCommandInBotTopic_Document(ChatType chatType) {
        jdbcTemplate.execute("delete from settings where id between 10001 and 10002");
        message.getChat().setType(chatType.getValue());
        message.setDocument(new Document());
        message.setMessageThreadId(9000);
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        boolean actualIsProcessingRequired = validator.validate(commandMessage);
        assertThat(actualIsProcessingRequired).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldReturnFalseForNonAdminCommandInNonBotTopic_Photo(ChatType chatType) {
        jdbcTemplate.execute("delete from settings where id between 10001 and 10002");
        message.getChat().setType(chatType.getValue());
        message.setPhoto(List.of(new PhotoSize()));
        message.setMessageThreadId(93124);
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        boolean actualIsProcessingRequired = validator.validate(commandMessage);
        assertThat(actualIsProcessingRequired).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ChatType.class, mode = EnumSource.Mode.EXCLUDE, names = {"PRIVATE"})
    void shouldReturnTrueForNonAdminCommandInBotTopic_Photo(ChatType chatType) {
        jdbcTemplate.execute("delete from settings where id between 10001 and 10002");
        message.getChat().setType(chatType.getValue());
        message.setPhoto(List.of(new PhotoSize()));
        message.setMessageThreadId(9000);
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        boolean actualIsProcessingRequired = validator.validate(commandMessage);
        assertThat(actualIsProcessingRequired).isFalse();
    }

    @Test
    void shouldThrowForNonAdminCommandWhenChatIsNotConfigured_PrivateChat() {
        jdbcTemplate.execute("delete from settings where id = 10000");
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatThrownBy(() -> validator.validate(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(BOT_NOT_CONFIGURED_TEXT);
    }

    @Test
    void shouldThrowForNonAdminCommandWhenTopicIdsAreNotConfigured_PrivateChat() {
        jdbcTemplate.execute("delete from settings where id between 10001 and 10002");
        message.setText("/" + Command.REGISTER.name().toLowerCase() + " steam_name");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatThrownBy(() -> validator.validate(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(BOT_NOT_CONFIGURED_TEXT);
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
