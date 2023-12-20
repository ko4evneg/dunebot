package ru.trainithard.dunebot.service;

import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.dto.ConfirmMatchDto;
import ru.trainithard.dunebot.service.dto.MatchSubmitDto;

public interface MatchService {
    // TODO:  add handling
    void requestNewMatch(Player initiator, ModType modType);

    void cancelMatch(long playerId);

    void registerMathPlayer(Player player, Match match);

    void unregisterMathPlayer(MatchPlayer matchPlayer);

    void acceptMatchSubmit(MatchSubmitDto matchSubmit);

    void confirmMatchSubmit(ConfirmMatchDto confirmMatch);
}
