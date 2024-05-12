package ru.trainithard.dunebot.service.telegram.command.processor;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.messaging.dto.SetCommandsDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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
        processor.process(getCommandMessage("init", 10000));

        ArgumentCaptor<SetCommandsDto> setCommandsDtoCaptor = ArgumentCaptor.forClass(SetCommandsDto.class);
        verify(messagingService, times(1)).sendSetCommands(setCommandsDtoCaptor.capture());
        SetCommandsDto actualSetCommandsDto = setCommandsDtoCaptor.getValue();
        Map<String, String> actualCommands = actualSetCommandsDto.getCommandDescriptionsByName();
        Map<String, String> expectedCommands = Map.of(
                "/help", "Как пользоваться ботом",
                "/new_dune", "Создание новой партии в классическую Дюну",
                "/new_up4", "Создание новой партии в Uprising на 4-х игроков",
                "/new_up6", "Создание новой партии в Uprising на 6-х игроков",
                "/cancel", "Отмена *вашего* последнего незавершенного матча"
        );

        assertThat(actualCommands).containsExactlyEntriesOf(expectedCommands);
    }

    @Test
    void shouldInvokeSaveChatIdSettingOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set_chat", 10000));

        verify(settingsService, times(1)).saveSetting(AppSettingKey.CHAT_ID, "10011");
    }

    @Test
    void shouldInvokeSaveClassicTopicIdSettingOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set_topic_dune", 12345));

        verify(settingsService, times(1)).saveSetting(AppSettingKey.TOPIC_ID_CLASSIC, "12345");
    }

    @Test
    void shouldInvokeSaveUprisingTopicIdSettingOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set_topic_up4", 12121));

        verify(settingsService, times(1)).saveSetting(AppSettingKey.TOPIC_ID_UPRISING, "12121");
    }

    @Test
    void shouldInvokeSaveFinishMatchTimeoutSettingOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set finish_match_timeout 30", 10000));

        verify(settingsService, times(1)).saveSetting(AppSettingKey.FINISH_MATCH_TIMEOUT, "30");
    }

    @Test
    void shouldInvokeSaveResubmitsLimitOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set resubmits_limit 3", 10000));

        verify(settingsService, times(1)).saveSetting(AppSettingKey.RESUBMITS_LIMIT, "3");
    }

    @Test
    void shouldInvokeSaveMothlyMatchesThresholdSettingOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set monthly_matches_threshold 10", 10000));

        verify(settingsService, times(1)).saveSetting(AppSettingKey.MONTHLY_MATCHES_THRESHOLD, "10");
    }

    @Test
    void shouldInvokeSaveMatchStartDelaySettingOnTopicInitSubcommand() {
        processor.process(getCommandMessage("set match_start_delay 40", 10000));

        verify(settingsService, times(1)).saveSetting(AppSettingKey.MATCH_START_DELAY, "40");
    }

    @Test
    void shouldThrowOnWrongSettingKey() {
        ThrowingCallable actualAction = () -> processor.process(getCommandMessage("set wrong_key 40", 10000));

        assertThatThrownBy(actualAction)
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(WRONG_SETTING_TEXT);
    }

    @Test
    void shouldThrowOnWrongSettingValue() {
        ThrowingCallable actualAction = () -> processor.process(getCommandMessage("set match_start_delay bad_val", 10000));

        assertThatThrownBy(actualAction)
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(WRONG_SETTING_VALUE_TEXT);
    }

    @Test
    void shouldSendErrorMessageOnUnknownSubcommand() {
        processor.process(getCommandMessage("zzz", 10020));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();

        assertThat(actualMessageDto)
                .extracting(MessageDto::getChatId, MessageDto::getReplyMessageId, MessageDto::getText)
                .containsExactly("10011", 10020, WRONG_COMMAND_EXCEPTION_MESSAGE);
    }

    @Test
    void shouldSendSuccessMessageOnUnknownSubcommand() {
        processor.process(getCommandMessage("zzz", 10020));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();

        assertThat(actualMessageDto)
                .extracting(MessageDto::getChatId, MessageDto::getReplyMessageId, MessageDto::getText)
                .containsExactly("10011", 10020, WRONG_COMMAND_EXCEPTION_MESSAGE);
    }

    @Test
    void shouldReturnAdminCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.ADMIN);
    }

    @Test
    void shouldSendSingleBroadcastMessageWhenTopicsAreSame() {
        when(settingsService.getStringSetting(AppSettingKey.CHAT_ID)).thenReturn("12345");
        when(settingsService.getIntSetting(AppSettingKey.TOPIC_ID_UPRISING)).thenReturn(3);
        when(settingsService.getIntSetting(AppSettingKey.TOPIC_ID_CLASSIC)).thenReturn(3);

        processor.process(getCommandMessage("message Корова шла по полю, а потом упала!$%()", 10020));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(2)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getAllValues().get(0);

        assertThat(actualMessageDto)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getText)
                .containsExactly("12345", 3, "Корова шла по полю, а потом упала\\!$%\\(\\)");
    }

    @Test
    void shouldSendTwoBroadcastMessagesWhenTopicsAreDifferent() {
        when(settingsService.getStringSetting(AppSettingKey.CHAT_ID)).thenReturn("12345");
        when(settingsService.getIntSetting(AppSettingKey.TOPIC_ID_UPRISING)).thenReturn(1);
        when(settingsService.getIntSetting(AppSettingKey.TOPIC_ID_CLASSIC)).thenReturn(2);

        processor.process(getCommandMessage("message Корова шла по полю, а потом упала!$%()", 10020));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(3)).sendMessageAsync(messageDtoCaptor.capture());

        MessageDto actualMessageDto1 = messageDtoCaptor.getAllValues().get(0);
        assertThat(actualMessageDto1)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getText)
                .containsExactly("12345", 1, "Корова шла по полю, а потом упала\\!$%\\(\\)");

        MessageDto actualMessageDto2 = messageDtoCaptor.getAllValues().get(1);
        assertThat(actualMessageDto2)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getText)
                .containsExactly("12345", 2, "Корова шла по полю, а потом упала\\!$%\\(\\)");
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
