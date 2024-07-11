package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest
class HelpCommandProcessorTest extends TestContextMock {
    @Autowired
    private HelpCommandProcessor processor;
    @MockBean
    private ExternalMessageFactory messageFactory;

    @Test
    void shouldSendHelpText() {
        ExternalMessage message = new ExternalMessage("xXxXx");
        doReturn(message).when(messageFactory).getHelpMessage();

        processor.process(getCommandMessage());

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();

        assertThat(actualMessageDto.getChatId()).isEqualTo("10222");
        assertThat(actualMessageDto.getReplyMessageId()).isEqualTo(10101);
        assertThat(actualMessageDto.getText()).isEqualTo("xXxXx");
    }

    @Test
    void shouldReturnHelpCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.HELP);
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
