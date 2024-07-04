package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.*;
import ru.trainithard.dunebot.repository.LeaderRepository;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.task.DuneScheduledTaskFactory;
import ru.trainithard.dunebot.service.task.DunebotRunnable;
import ru.trainithard.dunebot.service.telegram.command.CallbackCommandDetector;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LeaderAcceptCommandProcessor extends AcceptSubmitCommandProcessor {
    private static final String ALREADY_ACCEPTED_SUBMIT_MESSAGE_TEMPLATE =
            "Вы уже назначили лидера %s игроку %s. Выберите другого лидера, или используйте команду '/resubmit %d', чтобы начать заново.";
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final LeaderRepository leaderRepository;
    private final ExternalMessageFactory externalMessageFactory;
    private final DuneBotTaskScheduler taskScheduler;
    private final DuneScheduledTaskFactory taskFactory;
    private final Clock clock;
    private final AppSettingsService appSettingsService;

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
            rescheduleAcceptSubmitTimeoutTask(matchId);
            sendPlayersSubmitCompletedMessages(commandMessage, match);
        }
    }

    private void rescheduleAcceptSubmitTimeoutTask(long matchId) {
        DuneBotTaskId taskId = new DuneBotTaskId(DuneTaskType.SUBMIT_ACCEPT_TIMEOUT, matchId);
        DunebotRunnable submitAcceptTimeoutTask = taskFactory.createInstance(taskId);
        int acceptSubmitTimeout = appSettingsService.getIntSetting(AppSettingKey.ACCEPT_SUBMIT_TIMEOUT);
        Instant startTime = Instant.now(clock).plus(acceptSubmitTimeout, ChronoUnit.MINUTES);
        taskScheduler.rescheduleSingleRunTask(submitAcceptTimeoutTask, taskId, startTime);
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
