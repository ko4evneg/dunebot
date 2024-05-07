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
                [Подробная инструкция к боту](https://github.com/ko4evneg/dunebot/blob/master/help.md)
                              
                *Краткая инструкция*
                              
                *‼️Все команды пишем напрямую в чат бота **@tabledune\\_bot*
                              
                *1️⃣  Регистрация*
                `/profile Имя \\(ник\\_steam\\) Фамилия`
                'Имя' и 'Фамилия' \\- это ваши данные для рейтинга, 'ник\\_steam' \\- ваш ник в Steam\\. Писать в таком же формате как и указано \\- имя, ник стима в скобочках, фамилия\\.
                🪧  Для смены данных выполняется та же команда, что и выше\\.
                📌  `/profile` \\- обновляет имена из Telegram профиля \\(доступна только после регистрации\\)\\.
                              
                *2️⃣  Создание матча*
                `/new\\_dune` \\- для классики
                `/new\\_up4` \\- для обычного Uprising
                `/new\\_up6` \\- для Uprising 3х3 \\(для этого режима бот только создает опросы\\)
                              
                *3️⃣  Начало матча*
                Ждем, пока найдутся все игроки \\- бот пришлет уведомление в канал и тегнет вас\\. В уведомлении вы найдете *ID матча* \\- он понадобится для публикации результатов\\.
                              
                *4️⃣  Завершение матча*
                Любой игрок выполняет команду `/submit X`, где X \\- ID матча из пункта 3\\. Каждому игроку придет сообщение с кнопками для выбора занятого места и лидера\\. Победителю также придет запрос на загрузку скриншота\\. Скриншот можно просто перетащить в чат\\.
                              
                *5️⃣  Результаты*
                В канал матчей бота придет результат матча с занятыми местами \\- это значит, что все хорошо и матч зачтен в рейтинг\\. Иначе придет уведомление, что матч завершен без результата, а также причина ошибки\\.
                              
                ❗  На этапе пилота важно отслеживать все ошибки\\. Если видите, что бот работает как\\-то не так, пишите в канал фидбека бота\\.""";
    }
}
