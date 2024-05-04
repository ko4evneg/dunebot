package ru.trainithard.dunebot.service.telegram.factory;

import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;

@Service
public class ExternalMessageFactoryImpl implements ExternalMessageFactory {
    @Override
    public ExternalMessage getGuestMessageDto(Player player) {
        return new ExternalMessage("""
                Вас приветствует DuneBot! Вы ответили да в опросе по рейтинговой игре - это значит, что по завершении \
                игры вам придет опрос, где нужно будет указать занятое в игре место (и загрузить скриншот матча в \
                случае победы) - не волнуйтесь, бот подскажет что делать.""").newLine()
                .append("Также вы автоматически зарегистрированы у бота как гость под именем ")
                .append(player.getFirstName()).append(" (").append(player.getSteamName()).append(") ").append(player.getLastName())
                .append(" - это значит, что вы не можете выполнять некоторые команды бота и не будете включены " +
                        "в результаты рейтинга.").newLine()
                .append("Для того, чтобы подтвердить регистрацию, выполните в этом чате команду")
                .appendBold(" '/refresh_profile Имя (ник в steam) Фамилия'").append(".").newLine()
                .appendBold("Желательно это  сделать прямо сейчас.").newLine()
                .append("Подробная информация о боте: /help.");
    }
}
