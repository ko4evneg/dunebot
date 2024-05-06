package ru.trainithard.dunebot.service.telegram.factory.messaging;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Leader;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.repository.LeaderRepository;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeyboardsFactoryImpl implements KeyboardsFactory {
    private final LeaderRepository leaderRepository;

    @Override
    public List<List<ButtonDto>> getLeadersKeyboard(Match match) {
        String callbackPrefix = match.getId() + "_L_";
        List<Leader> leaders = leaderRepository.findAllByModType(match.getModType(), Sort.sort(Leader.class).by(Leader::getName));
        List<ButtonDto> leadersButtons = leaders.stream()
                .map(leader -> new ButtonDto(leader.getName(), callbackPrefix + leader.getId()))
                .toList();
        return Lists.partition(leadersButtons, 2);
    }
}
