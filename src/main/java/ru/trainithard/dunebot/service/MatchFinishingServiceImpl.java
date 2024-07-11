package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchFinishingServiceImpl implements MatchFinishingService {
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final TransactionTemplate transactionTemplate;
    private final MessagingService messagingService;
    private final Clock clock;
    private final ExternalMessageFactory messageFactory;

    @Override
    public void finishCompletelySubmittedMatch(long matchId) {
        int logId = LogId.get();
        log.debug("{}: finishing submitted match started", logId);

        Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow();
        log.debug("{}: match {} state: {}", logId, matchId, LogId.getMatchLogInfo(match));
        if (match.getState() != MatchState.SUBMITTED) {
            log.error("{}: Match {} has wrong state for completed submit: {}. Ending...", logId, matchId, match.getState());
            return;
        }

        match.setState(MatchState.FINISHED);
        match.setFinishDate(LocalDate.now(clock));
        matchRepository.save(match);

        ExternalMessage matchSuccessfulFinishMessage = messageFactory.getMatchSuccessfulFinishMessage(match);
        messagingService.sendMessageAsync(new MessageDto(match.getExternalPollId(), matchSuccessfulFinishMessage));

        log.debug("{}: finishing submitted match ended", logId);
    }

    @Override
    public void finishPartiallySubmittedMatch(long matchId, boolean isFailedByResubmitsLimit) {
        int logId = LogId.get();
        log.debug("{}: match {} (not_submitted) finishing...", logId, matchId);

        Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow();
        log.debug("{}: match {} found (state: {})", logId, match.getId(), match.getState());
        if (match.getState() != MatchState.ON_SUBMIT) {
            log.error("{}: Match {} has wrong state for partial submit: {}. Ending...", logId, matchId, match.getState());
            return;
        }

        failMatchAndPlayers(match);
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.saveAll(match.getMatchPlayers());
            log.debug("{}: match {} and its player has been saved", logId, matchId);
        });

        ExternalMessage finishReasonMessage = isFailedByResubmitsLimit
                ? messageFactory.getFailByResubmitLimitExceededMessage(matchId)
                : messageFactory.getPartialSubmittedMatchFinishMessage(match);
        messagingService.sendMessageAsync(new MessageDto(match.getExternalPollId(), finishReasonMessage));

        log.debug("{}: finishing not submitted match ended", logId);
    }

    private void failMatchAndPlayers(Match match) {
        match.setState(MatchState.FAILED);
        match.setFinishDate(LocalDate.now(clock));
        match.getMatchPlayers().forEach(MatchPlayer::resetSubmitData);
    }
}
