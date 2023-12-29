package ru.trainithard.dunebot.service;

import ru.trainithard.dunebot.service.dto.MatchSubmitDto;
import ru.trainithard.dunebot.service.messaging.dto.PlayerSubmitDto;

public interface MatchSubmitService {
    MatchSubmitDto getMatchSubmit(long telegramUserId, long telegramChatId, String matchIdString);

    void acceptPlayerSubmitAnswer(PlayerSubmitDto playerSubmitDto);
}
