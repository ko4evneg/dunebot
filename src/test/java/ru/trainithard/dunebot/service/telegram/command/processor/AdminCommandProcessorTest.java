package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestConstants;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.messaging.dto.SetCommandsDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class AdminCommandProcessorTest extends TestContextMock {
    private static final String HELP_COMMAND_DESCRIPTION = "Как пользоваться ботом";
    private static final String COMMANDS_LIST_DESCRIPTION = "Показать список доступных команд";
    private static final String HELP_COMMAND_TEXT = "/help";
    private static final String COMMANDS_LIST_COMMAND_TEXT = "/commands";
    private static final String WRONG_COMMAND_EXCEPTION_MESSAGE = "Неверная команда\\!";
    private static final String WRONG_SETTING_TEXT = "Неверное название настройки!";
    private static final String WRONG_SETTING_VALUE_TEXT = "Значение настройки должно быть числом!";

    @Autowired
    private AdminCommandProcessor processor;

    @MockBean
    private SettingsService settingsService;

    @Test
    void shouldInvokeSetCommandsServiceOnInitSubcommand() {
        processor.process(getCommandMessage("init", 10000), mockLoggingId);

        ArgumentCaptor<SetCommandsDto> setCommandsDtoCaptor = ArgumentCaptor.forClass(SetCommandsDto.class);
        verify(messagingService, times(1)).sendSetCommands(setCommandsDtoCaptor.capture());
        SetCommandsDto actualSetCommandsDto = setCommandsDtoCaptor.getValue();
        Map<String, String> actualCommands = actualSetCommandsDto.getCommandDescriptionsByName();

        assertEquals(HELP_COMMAND_DESCRIPTION, actualCommands.get(HELP_COMMAND_TEXT));
        assertEquals(COMMANDS_LIST_DESCRIPTION, actualCommands.get(COMMANDS_LIST_COMMAND_TEXT));
    }

    @Test
    void shouldInvokeSaveChatIdSettingOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set_chat", 10000), mockLoggingId);

        verify(settingsService, times(1)).saveSetting(SettingKey.CHAT_ID, "10011");
    }

    @Test
    void shouldInvokeSaveClassicTopicIdSettingOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set_topic_dune", 12345), mockLoggingId);

        verify(settingsService, times(1)).saveSetting(SettingKey.TOPIC_ID_CLASSIC, "12345");
    }

    @Test
    void shouldInvokeSaveUprisingTopicIdSettingOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set_topic_up4", 12121), mockLoggingId);

        verify(settingsService, times(1)).saveSetting(SettingKey.TOPIC_ID_UPRISING, "12121");
    }

    @Test
    void shouldInvokeSaveFinishMatchTimeoutSettingOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set finish_match_timeout 30", 10000), mockLoggingId);

        verify(settingsService, times(1)).saveSetting(SettingKey.FINISH_MATCH_TIMEOUT, "30");
    }

    @Test
    void shouldInvokeSaveResubmitsLimitOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set resubmits_limit 3", 10000), mockLoggingId);

        verify(settingsService, times(1)).saveSetting(SettingKey.RESUBMITS_LIMIT, "3");
    }

    @Test
    void shouldInvokeSaveMothlyMatchesThresholdSettingOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set monthly_matches_threshold 10", 10000), mockLoggingId);

        verify(settingsService, times(1)).saveSetting(SettingKey.MONTHLY_MATCHES_THRESHOLD, "10");
    }

    @Test
    void shouldInvokeSaveMatchStartDelaySettingOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set match_start_delay 40", 10000), mockLoggingId);

        verify(settingsService, times(1)).saveSetting(SettingKey.MATCH_START_DELAY, "40");
    }

    @Test
    void shouldThrowOnWrongSettingKey() {
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> processor.process(getCommandMessage("set wrong_key 40", 10000), mockLoggingId));
        assertEquals(WRONG_SETTING_TEXT, actualException.getMessage());
    }

    @Test
    void shouldThrowOnWrongSettingValue() {
        AnswerableDuneBotException actualException = assertThrows(AnswerableDuneBotException.class,
                () -> processor.process(getCommandMessage("set match_start_delay bad_val", 10000), mockLoggingId));
        assertEquals(WRONG_SETTING_VALUE_TEXT, actualException.getMessage());
    }

    @Test
    void shouldSendErrorMessageOnUnknownSubcommand() {
        processor.process(getCommandMessage("zzz", 10020), mockLoggingId);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();

        assertEquals("10011", actualMessageDto.getChatId());
        assertEquals(10020, actualMessageDto.getReplyMessageId());
        assertEquals(WRONG_COMMAND_EXCEPTION_MESSAGE, actualMessageDto.getText());
    }

    @Test
    void shouldSendSuccessMessageOnUnknownSubcommand() {
        processor.process(getCommandMessage("zzz", 10020), mockLoggingId);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();

        assertEquals("10011", actualMessageDto.getChatId());
        assertEquals(10020, actualMessageDto.getReplyMessageId());
        assertEquals(WRONG_COMMAND_EXCEPTION_MESSAGE, actualMessageDto.getText());
    }

    @Test
    void shouldReturnAdminCommand() {
        Command actualCommand = processor.getCommand();

        assertEquals(Command.ADMIN, actualCommand);
    }

    private CommandMessage getCommandMessage(String arg, int replyId) {
        Message replyMessage = new Message();
        replyMessage.setMessageId(replyId);

        Chat chat = new Chat();
        chat.setId(10011L);
        chat.setType(ChatType.GROUP.getValue());

        User user = new User();
        user.setId(TestConstants.ADMIN_USER_ID);
        user.setFirstName("EFname");
        user.setUserName("Uname");

        Message message = new Message();
        message.setReplyToMessage(replyMessage);
        message.setMessageId(10000);
        message.setText("/admin " + arg);
        message.setChat(chat);
        message.setFrom(user);

        return CommandMessage.getMessageInstance(message);
    }
}
