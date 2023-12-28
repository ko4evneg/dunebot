package ru.trainithard.dunebot.service;

import ru.trainithard.dunebot.service.dto.MatchSubmitDto;

public interface MatchSubmitService {
    MatchSubmitDto getMatchSubmit(long telegramUserId, long telegramChatId, String matchIdtext);
}
