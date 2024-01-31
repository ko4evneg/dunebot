package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class HelpCommandProcessor extends CommandProcessor {
    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        messagingService.sendMessageAsync(new MessageDto(commandMessage, getHelpText(), null));
    }

    @Override
    public Command getCommand() {
        return Command.HELP;
    }

    private String getHelpText() {
        // TODO:
        return """
                [Подробное описание бота](https://github.com/ko4evneg/dunebot/blob/master/help.md)
                                
                Список доступных команд:
                '/register Имя (ник_steam) Фамилия' Регистрация игрока в рейтинге
                '/refresh_profile Имя (ник_steam) Фамилия' Изменение данных существующего игрока
                '/new dune' Создание опроса для классической Dune
                '/new up4' Создание опроса для Dune Uprising (4 игрока)
                '/cancel' Удаление последнего опроса, созданного игроком
                '/submit ID_игры' Запуск регистрации результатов игры с номером **ID_игры**
                '/resubmit ID_игры' Запуск регистрации результатов игры заново. Возможно выполнить до трех раз на игру
                '/help' Помощь""";
    }
}
