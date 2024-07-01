package ru.trainithard.dunebot.service.telegram.factory.messaging;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Leader;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.LeaderRepository;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;

import java.util.Collection;
import java.util.List;

import static ru.trainithard.dunebot.service.telegram.command.CallbackCommandDetector.LEADER_CALLBACK_SYMBOL;
import static ru.trainithard.dunebot.service.telegram.command.CallbackCommandDetector.SUBMIT_PLAYERS_CALLBACK_SYMBOL;

@Service
@RequiredArgsConstructor
public class KeyboardsFactoryImpl implements KeyboardsFactory {
    private final LeaderRepository leaderRepository;

    @Override
    public List<List<ButtonDto>> getLeadersKeyboard(MatchPlayer submittingPlayer) {
        String callbackPrefix = submittingPlayer.getId() + LEADER_CALLBACK_SYMBOL;
        List<Leader> leaders = leaderRepository
                .findAllByModType(submittingPlayer.getMatch().getModType(), Sort.sort(Leader.class).by(Leader::getName));
        List<ButtonDto> leadersButtons = leaders.stream()
                .map(leader -> new ButtonDto(leader.getName(), callbackPrefix + leader.getId()))
                .toList();
        return Lists.partition(leadersButtons, 2);
    }

    @Override
    public List<List<ButtonDto>> getSubmitPlayersKeyboard(Collection<MatchPlayer> matchPlayers) {
        Long matchId = matchPlayers.stream().findFirst().orElseThrow().getMatch().getId();
        List<ButtonDto> matchPlayersButtons = matchPlayers.stream()
                .map(matchPlayer -> {
                    Player player = matchPlayer.getPlayer();
                    String callbackText = matchId + SUBMIT_PLAYERS_CALLBACK_SYMBOL + matchPlayer.getId();
                    return new ButtonDto(player.getFriendlyName(), callbackText);
                })
                .toList();
        return Lists.partition(matchPlayersButtons, 2);
    }
}
