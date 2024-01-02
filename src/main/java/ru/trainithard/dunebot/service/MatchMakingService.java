package ru.trainithard.dunebot.service;

import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;

public interface MatchMakingService {

    void cancelMatch(Match playerId);

    void registerMathPlayer(Player player, Match match);

    void unregisterMathPlayer(MatchPlayer matchPlayer);
}
