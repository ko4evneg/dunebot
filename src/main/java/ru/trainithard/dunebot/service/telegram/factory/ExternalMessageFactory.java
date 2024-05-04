package ru.trainithard.dunebot.service.telegram.factory;

import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;

public interface ExternalMessageFactory {
    ExternalMessage getGuestMessageDto(Player player);
}
