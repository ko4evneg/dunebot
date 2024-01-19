package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcceptSubmitCommandProcessor extends CommandProcessor {
    private static final String UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE = "Матч %d завершен без результата, так как превышено максимальное количество попыток регистрации мест";

    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final MatchFinishingService matchFinishingService;
    private final ResubmitCommandProcessor resubmitProcessor;
    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage) {
        Callback callback = new Callback(commandMessage.getCallback());
        Match match = matchRepository.findByIdWithMatchPlayers(callback.matchId).orElseThrow();
        List<MatchPlayer> matchPlayers = match.getMatchPlayers();
        MatchPlayer matchPlayer = matchPlayers.stream()
                .filter(mPlayer -> mPlayer.getPlayer().getExternalId() == commandMessage.getUserId())
                .findFirst().orElseThrow();
        if (matchPlayer.getCandidatePlace() == null) {
            int candidatePlace = callback.candidatePlace;
            if (hasConflictInSubmit(matchPlayers, candidatePlace) && match.isResubmitAllowed(SettingConstants.RESUBMITS_LIMIT)) {
                String conflictText = getConflictMessage(matchPlayers, matchPlayer, candidatePlace);
                sendMessagesToMatchPlayers(matchPlayers, conflictText);
                resubmitProcessor.process(match);
            } else if (hasConflictInSubmit(matchPlayers, candidatePlace)) {
                sendMessagesToMatchPlayers(matchPlayers, "Превышено количество запросов на регистрацию результатов. Результаты не сохранены, регистрация запрещена.");
                matchFinishingService.finishUnsuccessfullySubmittedMatch(match.getId(), String.format(UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE, match.getId()));
            } else {
                //todo retry submit field fix
                matchPlayer.setCandidatePlace(candidatePlace);
                match.setSubmitsCount(match.getSubmitsCount() + 1);
                transactionTemplate.executeWithoutResult(status -> {
                    matchRepository.save(match);
                    matchPlayerRepository.saveAll(matchPlayers);
                });
                if (match.areAllSubmitsReceived()) {
                    matchFinishingService.finishSuccessfullySubmittedMatch(match.getId());
                }
            }
        }
    }

    private void sendMessagesToMatchPlayers(Collection<MatchPlayer> matchPlayers, String message) {
        for (MatchPlayer matchPlayer : matchPlayers) {
            messagingService.sendMessageAsync(new MessageDto(matchPlayer.getPlayer().getExternalChatId(), message, null, null));
        }
    }

    private String getConflictMessage(Collection<MatchPlayer> matchPlayers, MatchPlayer candidate, int candidatePlace) {
        Map<Integer, List<MatchPlayer>> playersByPlace = matchPlayers.stream()
                .filter(matchPlayer -> matchPlayer.getCandidatePlace() != null && !matchPlayer.getId().equals(candidate.getId()))
                .collect(Collectors.groupingBy(MatchPlayer::getCandidatePlace));
        List<MatchPlayer> candidatePlaceMatchPlayers = playersByPlace.computeIfAbsent(candidatePlace, key -> new ArrayList<>());
        candidatePlaceMatchPlayers.add(candidate);
        StringBuilder conflictTextBuilder = new StringBuilder("Некоторые игроки не смогли поделить место:\n");
        for (Map.Entry<Integer, List<MatchPlayer>> entry : playersByPlace.entrySet()) {
            List<MatchPlayer> conflictMatchPlayers = entry.getValue();
            if (conflictMatchPlayers.size() > 1) {
                List<String> playerNames = conflictMatchPlayers.stream().map(matchPlayer -> matchPlayer.getPlayer().getFriendlyName()).toList();
                String playerNamesString = String.join(" и ", playerNames);
                conflictTextBuilder.append(entry.getKey()).append(" место: ").append(playerNamesString + "\n\n");
            }
            conflictTextBuilder.append("Повторный опрос результата...");
        }

        return conflictTextBuilder.toString();
    }

    private boolean hasConflictInSubmit(Collection<MatchPlayer> matchPlayers, int candidatePlace) {
        return matchPlayers.stream()
                .anyMatch(matchPlayer -> {
                    Integer comparedCandidatePlace = matchPlayer.getCandidatePlace();
                    return comparedCandidatePlace != null && comparedCandidatePlace == candidatePlace;
                });
    }

    @Override
    public Command getCommand() {
        return Command.ACCEPT_SUBMIT;
    }

    private static class Callback {
        private final long matchId;
        private final int candidatePlace;

        public Callback(String callbackText) {
            String[] callbackData = callbackText.split("__");
            this.matchId = Long.parseLong(callbackData[0]);
            this.candidatePlace = Integer.parseInt(callbackData[1]);
        }
    }
}
