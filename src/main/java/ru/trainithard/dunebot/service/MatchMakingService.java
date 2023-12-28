package ru.trainithard.dunebot.service;

import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.messaging.ExternalPollDto;

public interface MatchMakingService {

    void registerNewMatch(Player initiator, ModType modType, ExternalPollDto telegramUserMessage);

    void cancelMatch(Match playerId);

    void registerMathPlayer(Player player, Match match, int positiveAnswersCount);

    void unregisterMathPlayer(MatchPlayer matchPlayer, int positiveAnswersCount);
}
