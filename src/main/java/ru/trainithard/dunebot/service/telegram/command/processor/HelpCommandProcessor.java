package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

/**
 * Shows bot how to instructions to requester.
 */
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

    private ExternalMessage getHelpText() {
        return new ExternalMessage()
                .appendLink("Подробное описание бота", "https://github.com/ko4evneg/dunebot/blob/master/help.md")
                .newLine().newLine()
                .append("Список доступных команд:").newLine()
                .append("'/register Имя (ник_steam) Фамилия' Регистрация игрока в рейтинге").newLine()
                .append("'/refresh_profile Имя (ник_steam) Фамилия' Изменение данных существующего игрока").newLine()
                .append("'/new dune' Создание опроса для классической Dune").newLine()
                .append("'/new up4' Создание опроса для Dune Uprising (4 игрока)").newLine()
                .append("'/cancel' Удаление последнего опроса, созданного игроком").newLine()
                .append("'/submit ").appendBold("ID_игры").append("' Запуск регистрации результатов игры с номером ")
                .appendBold("ID_игры").newLine()
                .append("'/resubmit ").appendBold("ID_игры")
                .append("' Запуск регистрации результатов игры заново. Возможно выполнить до трех раз на игру").newLine()
                .append("'/help' Помощь");
    }
}
