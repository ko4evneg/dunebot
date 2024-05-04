package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

/**
 * Shows bot how to instructions to requester.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelpCommandProcessor extends CommandProcessor {
    @Value("${bot.version}")
    private String version;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: HELP started", logId());
        messagingService.sendMessageAsync(new MessageDto(commandMessage, getHelpText(), null));
        log.debug("{}: HELP ended", logId());
    }

    @Override
    public Command getCommand() {
        return Command.HELP;
    }

    private ExternalMessage getHelpText() {
        return new ExternalMessage()
                .startBold().append("Dunebot v").append(version).endBold().newLine()
                .appendLink("Подробное описание бота", "https://github.com/ko4evneg/dunebot/blob/master/help.md").newLine()
                .appendLink("История изменений", "https://github.com/ko4evneg/dunebot/blob/master/changelog.txt")
                .newLine().newLine()
                .append("Список доступных команд:").newLine()
                .append("'/profile Имя (ник_steam) Фамилия' Регистрация игрока в рейтинге").newLine()
                .append("'/profile Имя (ник_steam) Фамилия' Изменение данных существующего игрока").newLine()
                .append("'/profile' Обновление данных Telegram (только для зарегистрированных игроков)").newLine()
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
