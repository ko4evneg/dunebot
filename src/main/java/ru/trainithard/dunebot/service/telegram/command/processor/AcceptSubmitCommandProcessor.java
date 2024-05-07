package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.messaging.KeyboardsFactory;

import java.util.*;
import java.util.stream.Collectors;

import static ru.trainithard.dunebot.configuration.SettingConstants.EXTERNAL_LINE_SEPARATOR;

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
    private final SettingsService settingsService;
    private final KeyboardsFactory keyboardsFactory;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: ACCEPT_SUBMIT started", logId());

        Callback callback = new Callback(commandMessage.getCallback());
        Match match = matchRepository.findWithMatchPlayersBy(callback.matchId).orElseThrow();
        if (MatchState.getEndedMatchStates().contains(match.getState())) {
            log.debug("{}: submit received for match in {} state. Nothing done.", logId(), match.getState());
            log.debug("{}: ACCEPT_SUBMIT ended", logId());
            return;
        }
        List<MatchPlayer> matchPlayers = match.getMatchPlayers();
        MatchPlayer submittingPlayer = getSubmittingPlayer(commandMessage.getUserId(), matchPlayers);
        log.debug("{}: match {}, player {}", logId(), match.getId(), submittingPlayer.getPlayer().getId());

        if (!submittingPlayer.hasCandidateVote()) {
            deleteOldSubmitMessage(submittingPlayer);

            int candidatePlace = callback.candidatePlace;
            int resubmitsLimit = settingsService.getIntSetting(SettingKey.RESUBMITS_LIMIT);
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

    private MatchPlayer getSubmittingPlayer(long externalUserId, List<MatchPlayer> matchPlayers) {
        return matchPlayers.stream()
                .filter(mPlayer -> mPlayer.getPlayer().getExternalId() == externalUserId)
                .findFirst().orElseThrow();
    }

    private void processConflictResubmit(List<MatchPlayer> matchPlayers, MatchPlayer submittingPlayer, int candidatePlace, Match match) {
        log.debug("{}: conflict resolution - resubmit", logId());
        ExternalMessage conflictMessage = getConflictMessage(matchPlayers, submittingPlayer, candidatePlace);
        sendMessagesToMatchPlayers(matchPlayers, conflictMessage);
        resubmitProcessor.process(match);
    }

    private void processConflictMatchFinish(Match match) {
        log.debug("{}: conflict resolution - failed match", logId());
        matchFinishingService.finishNotSubmittedMatch(match.getId(), true);
    }

    private void processNonConflictSubmit(MatchPlayer submittingPlayer, Match match, int candidatePlace) {
        log.debug("{}: player's non-conflict submit processing", logId());
        submittingPlayer.setCandidatePlace(candidatePlace);
        match.setSubmitsCount(match.getSubmitsCount() + 1);
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.save(submittingPlayer);
            log.debug("{}: match {} player {} submit saved", logId(), match.getId(), submittingPlayer.getPlayer().getId());
        });
        Long chatId = submittingPlayer.getPlayer().getExternalChatId();
        List<List<ButtonDto>> leadersKeyboard = Set.of(2, 3, 4, 5, 6).contains(candidatePlace)
                ? keyboardsFactory.getLeadersKeyboard(submittingPlayer) : null;
        messagingService.sendMessageAsync(new MessageDto(chatId, getSubmitText(match.getId(), candidatePlace), null, leadersKeyboard));
        if (match.canBePreliminaryFinished()) {
            matchFinishingService.finishSubmittedMatch(match.getId());
        }
        log.debug("{}: player's submit successfully processed", logId());
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

    private ExternalMessage getConflictMessage(Collection<MatchPlayer> matchPlayers, MatchPlayer candidate, int candidatePlace) {
        Map<Integer, List<MatchPlayer>> playersByPlace = matchPlayers.stream()
                .filter(matchPlayer -> matchPlayer.getCandidatePlace() != null && !matchPlayer.getId().equals(candidate.getId()))
                .collect(Collectors.groupingBy(MatchPlayer::getCandidatePlace));
        List<MatchPlayer> candidatePlaceMatchPlayers = playersByPlace.computeIfAbsent(candidatePlace, key -> new ArrayList<>());
        candidatePlaceMatchPlayers.add(candidate);
        ExternalMessage conflictMessage = new ExternalMessage("Некоторые игроки не смогли поделить ").startBold();
        for (Map.Entry<Integer, List<MatchPlayer>> entry : playersByPlace.entrySet()) {
            List<MatchPlayer> conflictMatchPlayers = entry.getValue();
            if (conflictMatchPlayers.size() > 1) {
                conflictMessage.append(entry.getKey()).append(" место").endBold().append(":").newLine();
                conflictMatchPlayers.stream()
                        .map(matchPlayer -> matchPlayer.getPlayer().getFriendlyName())
                        .forEach(playerFriendlyName -> conflictMessage.append(playerFriendlyName).newLine());
            }
        }
        conflictMessage.append(EXTERNAL_LINE_SEPARATOR).append("Повторный опрос результата...");

        return conflictMessage;
    }

    private ExternalMessage getSubmitText(long matchId, int candidatePlace) {
        return switch (candidatePlace) {
            case 0 -> getAcceptedSubmitMessageTemplateForNonParticipant(matchId);
            case 1 -> getAcceptedFirstPlaceSubmitMessageTemplate(matchId, candidatePlace);
            default -> getAcceptedSubmitMessageTemplateForParticipant(matchId, candidatePlace);
        };
    }

    private ExternalMessage getAcceptedFirstPlaceSubmitMessageTemplate(long matchId, int candidatePlace) {
        return new ExternalMessage("В матче ").append(matchId).append(" за вами зафиксировано ")
                .startBold().append(candidatePlace).append(" место").endBold().append(".").newLine()
                .append("При ошибке используйте команду '/resubmit ").append(matchId).append("'.").newLine()
                .appendBold("Теперь загрузите в этот чат скриншот победы.");
    }

    private ExternalMessage getAcceptedSubmitMessageTemplateForParticipant(long matchId, int candidatePlace) {
        return new ExternalMessage("В матче ").append(matchId).append(" за вами зафиксировано ")
                .startBold().append(candidatePlace).append(" место").endBold().append(".").newLine()
                .append("При ошибке используйте команду '/resubmit ").append(matchId).append("'.").newLine()
                .appendBold("Теперь выберите лидера").append(" которым играли.");
    }

    private ExternalMessage getAcceptedSubmitMessageTemplateForNonParticipant(long matchId) {
        return new ExternalMessage("В матче ").append(matchId).append(" за вами зафиксирован статус: ")
                .appendBold("не участвует").append(".").newLine()
                .append("При ошибке используйте команду '/resubmit ").append(matchId).append("'.");
    }

    private void sendMessagesToMatchPlayers(Collection<MatchPlayer> matchPlayers, ExternalMessage message) {
        for (MatchPlayer matchPlayer : matchPlayers) {
            messagingService.sendMessageAsync(new MessageDto(matchPlayer.getPlayer().getExternalChatId(), message, null, null));
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
