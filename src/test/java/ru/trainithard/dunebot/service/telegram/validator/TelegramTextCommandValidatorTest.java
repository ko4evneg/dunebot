package ru.trainithard.dunebot.service.telegram.validator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestConstants;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TelegramTextCommandValidatorTest extends TestContextMock {
    private static final String NOT_AUTHORIZED_EXCEPTION_MESSAGE = "Команда требует прав администратора.";
    private static final String NOT_ENOUGH_ARGUMENTS_EXCEPTION_MESSAGE = "Данная команда должна иметь 3 параметр(а).";
    @Autowired
    private TelegramTextCommandValidator validator;

    private static final Long TELEGRAM_USER_ID = 12345L;
    private static final Long TELEGRAM_CHAT_ID = 9000L;
    private final Message message = new Message();

    @BeforeEach
    void beforeEach() {
        fillMessage();
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + TELEGRAM_USER_ID + ", " + TELEGRAM_CHAT_ID + " , 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) " +
                             "values (10000, '" + SettingKey.ADMIN_USER_ID + "', " + TestConstants.ADMIN_USER_ID + ", '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from settings where id = 10000");
        jdbcTemplate.execute("delete from players where id = 10000");
    }

    @Test
    void shouldThrowForNotEnoughArgumentsCommand() {
        message.setText("/register");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatThrownBy(() -> validator.validate(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(NOT_ENOUGH_ARGUMENTS_EXCEPTION_MESSAGE);
    }

    @Test
    void shouldNotThrowForEnoughArgumentsCommand() {
        message.setText("/register a (b) c");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatCode(() -> validator.validate(commandMessage)).doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowForEnoughArgumentsCommandWithTrailingSpaces() {
        message.setText("/register a (b) c   ");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatCode(() -> validator.validate(commandMessage)).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenNonAdminInvokesAdminCommand() {
        message.setText("/admin init");
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatThrownBy(() -> validator.validate(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(NOT_AUTHORIZED_EXCEPTION_MESSAGE);
    }


    @Test
    void shouldNotThrowWhenAdminInvokesAdminCommand() {
        message.setText("/admin init");
        message.getFrom().setId(TestConstants.ADMIN_USER_ID);
        CommandMessage commandMessage = CommandMessage.getMessageInstance(message);

        assertThatCode(() -> validator.validate(commandMessage)).doesNotThrowAnyException();
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
}
