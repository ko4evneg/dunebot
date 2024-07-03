package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Leader;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.LeaderRepository;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CallbackCommandDetector;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;
import ru.trainithard.dunebot.service.telegram.factory.messaging.KeyboardsFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LeaderAcceptCommandProcessor extends AcceptSubmitCommandProcessor {
    private static final String ALREADY_ACCEPTED_SUBMIT_MESSAGE_TEMPLATE =
            "Вы уже назначили лидера %s игроку %s. Выберите другого лидера, или используйте команду '/resubmit %d', чтобы начать заново.";
    private static final String FINISHED_MATCH_SUBMIT_MESSAGE_TEMPLATE = "Матч %d уже завершен. Регистрация результат более невозможна.";
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final LeaderRepository leaderRepository;
    private final ExternalMessageFactory externalMessageFactory;
    private final KeyboardsFactory keyboardsFactory;

    @Override
    public void process(CommandMessage commandMessage) {
        String[] callbackData = commandMessage.getCallback().split(CallbackCommandDetector.SUBMIT_LEADERS_CALLBACK_SYMBOL);
        long matchId = Long.parseLong(callbackData[0]);
        long leaderId = Long.parseLong(callbackData[1]);

        Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow();
        validateMatchIsNotFinished(commandMessage, match);

        Leader submittedLeader = leaderRepository.findById(leaderId).orElseThrow();
        Map<Integer, MatchPlayer> playerByPlaces = new HashMap<>();
        int submittedLeaderMaxPlace = 0;
        for (MatchPlayer matchPlayer : match.getMatchPlayers()) {
            validateLeaderIsNotSubmitted(commandMessage, matchPlayer, submittedLeader, matchId);
            Integer place = Objects.requireNonNull(matchPlayer.getPlace());
            playerByPlaces.put(place, matchPlayer);
            if (!Objects.isNull(matchPlayer.getLeader())) {
                submittedLeaderMaxPlace = Math.max(place, submittedLeaderMaxPlace);
            }
        }
        int nextLeaderPlace = submittedLeaderMaxPlace + 1;
        MatchPlayer submittedPlayer = playerByPlaces.get(nextLeaderPlace);
        submittedPlayer.setLeader(submittedLeader);
        match.setState(MatchState.SUBMITTED);
        transactionTemplate.executeWithoutResult(status -> {
            matchPlayerRepository.save(submittedPlayer);
            matchRepository.save(match);
        });

        if (nextLeaderPlace == match.getModType().getPlayersCount()) {
            sendPlayersSubmitCompletedMessages(commandMessage, match);
        }
    }

    private void validateLeaderIsNotSubmitted(CommandMessage commandMessage, MatchPlayer matchPlayer, Leader leader, long matchId) {
        if (leader.equals(matchPlayer.getLeader())) {
            String playerName = matchPlayer.getPlayer().getFriendlyName();
            String message = String.format(ALREADY_ACCEPTED_SUBMIT_MESSAGE_TEMPLATE, leader.getName(), playerName, matchId);
            throw new AnswerableDuneBotException(message, commandMessage);
        }
    }

    private void sendPlayersSubmitCompletedMessages(CommandMessage commandMessage, Match match) {
        ExternalMessage playerSubmitFinishMessage = externalMessageFactory.getFinishedLeadersSubmitMessage(match.getMatchPlayers());
        messagingService.sendMessageAsync(new MessageDto(commandMessage, playerSubmitFinishMessage, null));
    }

    @Override
    public Command getCommand() {
        return Command.LEADER_ACCEPT;
    }
}
