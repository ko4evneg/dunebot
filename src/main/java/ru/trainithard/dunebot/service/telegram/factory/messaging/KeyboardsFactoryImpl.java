package ru.trainithard.dunebot.service.telegram.factory.messaging;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Leader;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.LeaderRepository;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;

import java.util.Collection;
import java.util.List;

import static ru.trainithard.dunebot.service.telegram.command.CallbackSymbol.*;

@Service
@RequiredArgsConstructor
public class KeyboardsFactoryImpl implements KeyboardsFactory {
    private final LeaderRepository leaderRepository;

    @Override
    public List<List<ButtonDto>> getSubmitLeadersKeyboard(Match match) {
        List<Leader> leaders = leaderRepository
                .findAllByModType(match.getModType(), Sort.sort(Leader.class).by(Leader::getName));
        List<ButtonDto> leadersButtons = leaders.stream()
                .map(leader -> new ButtonDto(leader.getName(), match.getId() + SUBMIT_LEADERS_CALLBACK_SYMBOL.getSymbol() + leader.getId()))
                .toList();
        return Lists.partition(leadersButtons, 2);
    }

    @Override
    public List<List<ButtonDto>> getSubmitPlayersKeyboard(Collection<MatchPlayer> matchPlayers) {
        Long matchId = matchPlayers.stream().findFirst().orElseThrow().getMatch().getId();
        List<ButtonDto> matchPlayersButtons = matchPlayers.stream()
                .map(matchPlayer -> {
                    Player player = matchPlayer.getPlayer();
                    String callbackText = matchId + SUBMIT_PLAYERS_CALLBACK_SYMBOL.getSymbol() + matchPlayer.getId();
                    return new ButtonDto(player.getFriendlyName(), callbackText);
                })
                .toList();
        return Lists.partition(matchPlayersButtons, 2);
    }

    @Override
    public List<List<ButtonDto>> getResubmitKeyboard(Long matchId, Long userId, long submitterId) {
        ButtonDto selfButton = new ButtonDto("Хочу сам", matchId.toString() + RESUBMIT_CALLBACK_SYMBOL + userId);
        ButtonDto otherPlayerButton = new ButtonDto("Передам прошлому", matchId.toString() + RESUBMIT_CALLBACK_SYMBOL + submitterId);
        return List.of(List.of(selfButton, otherPlayerButton));
    }
}
