package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.dto.TelegramUserPollDto;

@Service
@RequiredArgsConstructor
public class MatchServiceAdapter {
    private final MatchService matchService;
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;

    public void requestNewMatch(long telegramUserId, ModType modType) {
        playerRepository.findByTelegramId(telegramUserId)
                .ifPresent(player -> matchService.requestNewMatch(player, modType));
    }

    public void cancelMatch(long telegramUserId) {
        playerRepository.findByTelegramId(telegramUserId)
                .ifPresent(player -> matchService.cancelMatch(player.getId()));
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
