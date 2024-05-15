package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.UserSettingKey;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.UserSettingsService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
class ConfigCommandProcessorTest extends TestContextMock {
    private static final Long TELEGRAM_USER_ID = 12345L;
    private static final Long TELEGRAM_CHAT_ID = 9000L;

    @Autowired
    private ConfigCommandProcessor processor;
    @SpyBean
    private UserSettingsService userSettingsService;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + TELEGRAM_USER_ID + ", " + TELEGRAM_CHAT_ID + " , 'st_pl1', 'name1', 'ln1', 'en1', '2010-10-10') ");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from user_settings where player_id = 10000");
        jdbcTemplate.execute("delete from players where id = 10000");
    }

    @Test
    void shouldSaveHostDataOnHostCommandWithArguments() {
        processor.process(getCommandMessage("/config host MyServer 123+-3"));

        String actualUserSetting = jdbcTemplate
                .queryForObject("select value from user_settings where player_id = 10000 and key = '" + UserSettingKey.HOST + "'",
                        String.class);

        assertThat(actualUserSetting).isEqualTo("MyServer 123+-3");
    }

    @Test
    void shouldSendSettingsOnHostShowSubCommand() {
        jdbcTemplate.execute("insert into user_settings (id, player_id, key, value, created_at) " +
                             "values (10000, 10000, '" + UserSettingKey.HOST + "', 'abc/123', '2010-10-10')");

        processor.process(getCommandMessage("/config show"));

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessage = messageCaptor.getValue();

        assertThat(actualMessage)
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly(TELEGRAM_CHAT_ID.toString(), """
                        Ваши настройки:
                        *HOST*: abc/123""");
    }

    @Test
    void shouldSendEmptyMessageOnHostShowCommandWhenNoSettingsConfigured() {
        processor.process(getCommandMessage("/config show"));

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessage = messageCaptor.getValue();

        assertThat(actualMessage)
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly(TELEGRAM_CHAT_ID.toString(), """
                        Ваши настройки:
                        настроек нет""");
    }

    @Test
    void shouldSendMessageOnSuccessfulSettingSave() {
        processor.process(getCommandMessage("/config host MyServer 123+-3"));

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessage = messageCaptor.getValue();

        assertThat(actualMessage)
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly(TELEGRAM_CHAT_ID.toString(), "Настройка сохранена");
    }

    @Test
    void shouldThrowOnWrongSubCommand() {
        CommandMessage commandMessage = getCommandMessage("/config fake");

        assertThatThrownBy(() -> processor.process(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Неверный аргумент!");
        verifyNoInteractions(messagingService);
        verifyNoInteractions(userSettingsService);
    }

    @Test
    void shouldReturnConfigCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.CONFIG);
    }

    private CommandMessage getCommandMessage(String text) {
        User user = new User();
        user.setId(TELEGRAM_USER_ID);
        Chat chat = new Chat();
        chat.setId(TELEGRAM_CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(100500);
        message.setChat(chat);
        message.setText(text);
        message.setFrom(user);
        return CommandMessage.getMessageInstance(message);
    }
}
