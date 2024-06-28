package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.LogId;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;
import ru.trainithard.dunebot.service.telegram.factory.messaging.KeyboardsFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Accepts player's reply to match submit message (place selection), validates consistency of specific match selected
 * places and run resubmit process for conflicting matches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcceptSubmitCommandProcessor extends CommandProcessor {
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final MatchFinishingService matchFinishingService;
    private final ResubmitCommandProcessor resubmitProcessor;
    private final AppSettingsService appSettingsService;
    private final ExternalMessageFactory messageFactory;
    private final KeyboardsFactory keyboardsFactory;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: ACCEPT_SUBMIT started", logId());

        Callback callback = new Callback(commandMessage.getCallback());
        Match match = matchRepository.findWithMatchPlayersBy(callback.matchId).orElseThrow();
        if (MatchState.getEndedMatchStates().contains(match.getState())) {
            log.debug("{}: submit received for match in {} state. Nothing done.", logId(), match.getState());
            log.debug("{}: ACCEPT_SUBMIT ended", logId());
            sendRejectMessage(commandMessage, match);
            return;
        }
        List<MatchPlayer> matchPlayers = match.getMatchPlayers();
        MatchPlayer submittingPlayer = getSubmittingPlayer(commandMessage.getUserId(), matchPlayers);
        log.debug("{}: match {}, player {}. state: {}",
                logId(), match.getId(), submittingPlayer.getPlayer().getId(), LogId.getMatchLogInfo(match));

        if (!submittingPlayer.hasCandidateVote()) {
            deleteOldSubmitMessage(submittingPlayer);

            int candidatePlace = callback.candidatePlace;
            int resubmitsLimit = appSettingsService.getIntSetting(AppSettingKey.RESUBMITS_LIMIT);
            log.debug("{}: candidatePlace = {}", logId(), candidatePlace);
            if (isConflictSubmit(match.getMatchPlayers(), candidatePlace) && match.isResubmitAllowed(resubmitsLimit)) {
                processConflictResubmit(matchPlayers, submittingPlayer, candidatePlace, match);
            } else if (isConflictSubmit(matchPlayers, candidatePlace)) {
                processConflictMatchFinish(match);
            } else {
                processNonConflictSubmit(submittingPlayer, match, candidatePlace);
            }
        }

        log.debug("{}: ACCEPT_SUBMIT ended", logId());
    }

    private void sendRejectMessage(CommandMessage commandMessage, Match match) {
        ExternalMessage externalMessage = messageFactory.getAcceptSubmitRejectedDueToMatchFinishMessage(match.getId());
        MessageDto messageDto = new MessageDto(commandMessage, externalMessage, null);
        messagingService.sendMessageAsync(messageDto);
    }

    private MatchPlayer getSubmittingPlayer(long externalUserId, List<MatchPlayer> matchPlayers) {
        return matchPlayers.stream()
                .filter(mPlayer -> mPlayer.getPlayer().getExternalId() == externalUserId)
                .findFirst().orElseThrow();
    }

    private void deleteOldSubmitMessage(MatchPlayer submittingPlayer) {
        if (submittingPlayer.hasSubmitMessage()) {
            messagingService.deleteMessageAsync(submittingPlayer.getSubmitMessageId());
        }
    }

    private boolean isConflictSubmit(List<MatchPlayer> matchPlayers, int candidatePlace) {
        return candidatePlace != 0 && matchPlayers.stream()
                .filter(matchPlayer -> {
                    Integer comparedCandidatePlace = matchPlayer.getCandidatePlace();
                    return comparedCandidatePlace != null && comparedCandidatePlace == candidatePlace;
                })
                .peek(matchPlayer -> log.debug("{}: conflict found. matchPlayer {} has same candidatePlace ", logId(), matchPlayer.getId()))
                .findFirst().isPresent();
    }

    private void processConflictResubmit(List<MatchPlayer> matchPlayers, MatchPlayer submittingPlayer, int candidatePlace, Match match) {
        log.debug("{}: conflict detected - resubmit...", logId());
        sendConflictSubmitMessagesToMatchPlayers(matchPlayers, submittingPlayer, candidatePlace);
        resubmitProcessor.process(match);
    }

    private void processConflictMatchFinish(Match match) {
        log.debug("{}: conflict detected - not_submitted finish...", logId());
        matchFinishingService.finishNotSubmittedMatch(match.getId(), true);
    }

    private void processNonConflictSubmit(MatchPlayer submittingPlayer, Match match, int candidatePlace) {
        log.debug("{}: non-conflict submit processing...", logId());
        submittingPlayer.setCandidatePlace(candidatePlace);
        match.setSubmitsCount(match.getSubmitsCount() + 1);
        Match savedMatch = transactionTemplate.execute(status -> {
            matchPlayerRepository.save(submittingPlayer);
            return matchRepository.save(match);

        });
        log.debug("{}: match {} player {} (submits: {}) submit accepted",
                logId(), match.getId(), savedMatch == null ? "null" : savedMatch.getSubmitsCount(), submittingPlayer.getPlayer().getId());
        sendLeadersMessageToSubmittingPlayer(submittingPlayer, match, candidatePlace);
        if (match.canBePreliminaryFinished()) {
            matchFinishingService.finishSubmittedMatch(match.getId());
        }
        log.debug("{}: player's submit successfully processed", logId());
    }

    private void sendLeadersMessageToSubmittingPlayer(MatchPlayer submittingPlayer, Match match, int candidatePlace) {
        long chatId = submittingPlayer.getPlayer().getExternalChatId();
        List<List<ButtonDto>> leadersKeyboard = Set.of(1, 2, 3, 4, 5, 6).contains(candidatePlace)
                ? keyboardsFactory.getLeadersKeyboard(submittingPlayer) : null;
        ExternalMessage submitMessage = messageFactory.getNonClonflictSubmitMessage(match.getId(), candidatePlace);
        messagingService.sendMessageAsync(new MessageDto(chatId, submitMessage, null, leadersKeyboard));
    }

    private void sendConflictSubmitMessagesToMatchPlayers(Collection<MatchPlayer> matchPlayers,
                                                          MatchPlayer submittingPlayer, int candidatePlace) {
        ExternalMessage conflictMessage = messageFactory.getConflictSubmitMessage(matchPlayers, submittingPlayer, candidatePlace);
        for (MatchPlayer matchPlayer : matchPlayers) {
            long playerExternalChatId = matchPlayer.getPlayer().getExternalChatId();
            messagingService.sendMessageAsync(new MessageDto(playerExternalChatId, conflictMessage, null, null));
        }
    }

    @Override
    public Command getCommand() {
        return Command.ACCEPT_SUBMIT;
    }

    private static class Callback {
        private final long matchId;
        private final int candidatePlace;

        private Callback(String callbackText) {
            String[] callbackData = callbackText.split("__");
            this.matchId = Long.parseLong(callbackData[0]);
            this.candidatePlace = Integer.parseInt(callbackData[1]);
        }
    }
}
