package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class HelpCommandProcessorTest extends TestContextMock {
    @Autowired
    private HelpCommandProcessor processor;

    @Test
    void shouldSendHelpText() {
        processor.process(getCommandMessage());

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();

        assertThat(actualMessageDto.getChatId()).isEqualTo("10222");
        assertThat(actualMessageDto.getReplyMessageId()).isEqualTo(10101);
        assertThat(actualMessageDto.getText()).isEqualTo(getExpectedHelpText());
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

    private String getExpectedHelpText() {
        return """
                *Dunebot v1\\.3*
                [Подробное описание бота](https://github.com/ko4evneg/dunebot/blob/master/help.md)
                              
                Список доступных команд:
                '/register Имя \\(ник\\_steam\\) Фамилия' Регистрация игрока в рейтинге
                '/refresh\\_profile Имя \\(ник\\_steam\\) Фамилия' Изменение данных существующего игрока
                '/new dune' Создание опроса для классической Dune
                '/new up4' Создание опроса для Dune Uprising \\(4 игрока\\)
                '/cancel' Удаление последнего опроса, созданного игроком
                '/submit *ID\\_игры*' Запуск регистрации результатов игры с номером *ID\\_игры*
                '/resubmit *ID\\_игры*' Запуск регистрации результатов игры заново\\. Возможно выполнить до трех раз на игру
                '/help' Помощь""";
    }
}
