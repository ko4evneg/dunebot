package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;
import ru.trainithard.dunebot.service.dto.TelegramUserPollDto;
import ru.trainithard.dunebot.service.messaging.MessagingService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchCommandProcessor {
    private final MatchMakingService matchMakingService;
    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final MessagingService messagingService;

    public void cancelMatch(long externalUserId) {
        playerRepository.findByExternalId(externalUserId).ifPresent(player -> {
            Optional<Match> latestOwnedMatchOptional = matchRepository.findLatestOwnedMatch(player.getId());
            if (latestOwnedMatchOptional.isPresent()) {
                Match latestOwnedMatch = latestOwnedMatchOptional.get();
                if (latestOwnedMatch.isFinished()) {
                    throw new AnswerableDuneBotException("Запрещено отменять завершенные матчи!", player.getExternalChatId());
                }
                messagingService.deletePollAsync(latestOwnedMatch.getExternalPollId());
                matchMakingService.cancelMatch(latestOwnedMatch);
            }
        });
    }

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
