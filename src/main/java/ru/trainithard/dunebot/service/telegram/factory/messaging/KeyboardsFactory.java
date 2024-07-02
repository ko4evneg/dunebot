package ru.trainithard.dunebot.service.telegram.factory.messaging;

import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;

import java.util.Collection;
import java.util.List;

public interface KeyboardsFactory {
    List<List<ButtonDto>> getLeadersKeyboard(MatchPlayer submittingPlayer);

    List<List<ButtonDto>> getSubmitLeadersKeyboard(Match match);

    List<List<ButtonDto>> getSubmitPlayersKeyboard(Collection<MatchPlayer> matchPlayers);
}
