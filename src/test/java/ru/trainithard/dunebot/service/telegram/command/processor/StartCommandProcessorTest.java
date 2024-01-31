package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class StartCommandProcessorTest extends TestContextMock {
    @Autowired
    private StartCommandProcessor startCommandProcessor;
    @MockBean
    private HelpCommandProcessor helpCommandProcessor;

    @Test
    void shouldRedirectToHelpProcessorOnCommand() {
        CommandMessage commandMessage = getCommandMessage();

        startCommandProcessor.process(commandMessage, mockLoggingId);

        verify(helpCommandProcessor, times(1)).process(eq(commandMessage), eq(mockLoggingId));
    }

    @Test
    void shouldReturnHelpCommand() {
        Command actualCommand = startCommandProcessor.getCommand();

        assertEquals(Command.START, actualCommand);
    }

    private CommandMessage getCommandMessage() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(10101);

        Chat chat = new Chat();
        chat.setId(10222L);
        chat.setType(ChatType.PRIVATE.getValue());

        User user = new User();
        user.setId(12345L);
        user.setFirstName("EFname");
        user.setUserName("Uname");

        Message message = new Message();
        message.setReplyToMessage(replyMessage);
        message.setMessageId(10000);
        message.setText("/help");
        message.setChat(chat);
        message.setFrom(user);

        return CommandMessage.getMessageInstance(message);
    }
}
