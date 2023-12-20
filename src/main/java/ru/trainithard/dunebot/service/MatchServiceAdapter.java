package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.dto.PollMessageDto;

@Service
@RequiredArgsConstructor
public class MatchServiceAdapter {
    private final MatchService matchService;
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;


    public void registerMathPlayer(PollMessageDto pollMessage) {
        playerRepository.findByTelegramId(pollMessage.telegramUserId()).ifPresent(player -> {
            matchRepository.findByTelegramPollId(pollMessage.telegramPollId()).ifPresent(match -> {
                matchService.registerMathPlayer(player, match);
            });
        });
    }

    public void unregisterMathPlayer(PollMessageDto pollMessage) {
        matchPlayerRepository
                .findByMatchTelegramPollIdAndPlayerTelegramId(pollMessage.telegramPollId(), pollMessage.telegramUserId())
                .ifPresent(matchService::unregisterMathPlayer);
    }
}
