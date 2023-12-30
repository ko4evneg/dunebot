package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;
import ru.trainithard.dunebot.service.dto.TelegramUserPollDto;

@Service
@RequiredArgsConstructor
public class MatchCommandProcessor {
    private final MatchMakingService matchMakingService;
    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;

    public void registerMathPlayer(TelegramUserPollDto pollMessage) {
        playerRepository.findByExternalId(pollMessage.telegramUserId()).ifPresent(player ->
                matchRepository.findByExternalPollIdPollId(pollMessage.telegramPollId()).ifPresent(match ->
                        matchMakingService.registerMathPlayer(player, match, pollMessage.positiveAnswersCount())
                )
        );
    }

    public void unregisterMathPlayer(TelegramUserPollDto pollMessage) {
        matchPlayerRepository
                .findByMatchExternalPollIdPollIdAndPlayerExternalId(pollMessage.telegramPollId(), pollMessage.telegramUserId())
                .ifPresent(matchPlayer -> matchMakingService.unregisterMathPlayer(matchPlayer, pollMessage.positiveAnswersCount()));
    }

    public void registerNewPlayer(PlayerRegistrationDto playerRegistration) {
        playerService.registerNewPlayer(playerRegistration);
    }
}
