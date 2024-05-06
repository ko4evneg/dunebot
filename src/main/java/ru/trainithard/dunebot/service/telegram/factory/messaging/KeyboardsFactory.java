package ru.trainithard.dunebot.service.telegram.factory.messaging;

import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;

import java.util.List;

public interface KeyboardsFactory {
    List<List<ButtonDto>> getLeadersKeyboard(Match match);
}
