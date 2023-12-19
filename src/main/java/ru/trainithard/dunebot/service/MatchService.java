package ru.trainithard.dunebot.service;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.dto.ConfirmMatchDto;
import ru.trainithard.dunebot.service.dto.MatchSubmitDto;

public interface MatchService {
    // TODO:  add handling
    void requestNewMatch(Player initiator, ModType modType) throws TelegramApiException;

    void cancelNewMatch(Player initiator, ModType modType);

    void requestMatchSubmit(Player player);

    void acceptMatchSubmit(MatchSubmitDto matchSubmit);

    void confirmMatchSubmit(ConfirmMatchDto confirmMatch);
}
