package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.dto.TelegramUserPollDto;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchServiceAdapter {
    private final MatchService matchService;
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;

    public void cancelMatch(long telegramUserId) {
        Optional<Player> playerOptional = playerRepository.findByTelegramId(telegramUserId);
        if (playerOptional.isPresent()) {
            matchService.cancelMatch(playerOptional.get().getId());
        }
    }

    public void registerMathPlayer(TelegramUserPollDto pollMessage) {
        playerRepository.findByTelegramId(pollMessage.telegramUserId())
                .ifPresent(player -> matchRepository.findByTelegramPollId(pollMessage.telegramPollId())
                        .ifPresent(match -> matchService.registerMathPlayer(player, match)
                        )
                );
    }

    public void unregisterMathPlayer(TelegramUserPollDto pollMessage) {
        matchPlayerRepository
                .findByMatchTelegramPollIdAndPlayerTelegramId(pollMessage.telegramPollId(), pollMessage.telegramUserId())
                .ifPresent(matchService::unregisterMathPlayer);
    }
}
