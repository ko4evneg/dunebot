package ru.trainithard.dunebot.service.telegram.command.processor.admin;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestConstants;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.messaging.dto.SetCommandsDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class AdminCommandProcessorTest extends TestContextMock {
    private static final String HELP_COMMAND_DESCRIPTION = "Как пользоваться ботом";
    private static final String COMMANDS_LIST_DESCRIPTION = "Показать список доступных команд";
    private static final String HELP_COMMAND_TEXT = "/help";
    private static final String COMMANDS_LIST_COMMAND_TEXT = "/commands";

    @Autowired
    private AdminCommandProcessor processor;

    @Test
    void shouldInvokeSetCommandsServiceOnInitSubcommand() {
        processor.process(getCommandMessage(), mockLoggingId);

        ArgumentCaptor<SetCommandsDto> setCommandsDtoCaptor = ArgumentCaptor.forClass(SetCommandsDto.class);
        verify(messagingService, times(1)).sendSetCommands(setCommandsDtoCaptor.capture());
        SetCommandsDto actualSetCommandsDto = setCommandsDtoCaptor.getValue();
        Map<String, String> actualCommands = actualSetCommandsDto.getCommandDescriptionsByName();

        assertEquals(HELP_COMMAND_DESCRIPTION, actualCommands.get(HELP_COMMAND_TEXT));
        assertEquals(COMMANDS_LIST_DESCRIPTION, actualCommands.get(COMMANDS_LIST_COMMAND_TEXT));
    }

    private CommandMessage getCommandMessage() {
        Message message = new Message();
        message.setMessageId(10000);
        message.setText("/admin init");
        Chat chat = new Chat();
        chat.setId(10000L);
        chat.setType(ChatType.GROUP.getValue());
        message.setChat(chat);
        User user = new User();
        user.setId(TestConstants.ADMIN_USER_ID);
        user.setFirstName("EFname");
        user.setUserName("Uname");
        message.setFrom(user);
        return CommandMessage.getMessageInstance(message);
    }
}
