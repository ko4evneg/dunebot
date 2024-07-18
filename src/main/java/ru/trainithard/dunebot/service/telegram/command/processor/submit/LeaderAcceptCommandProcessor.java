package ru.trainithard.dunebot.service.telegram.command.processor.submit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.*;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.LeaderRepository;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.task.DuneScheduledTaskFactory;
import ru.trainithard.dunebot.service.task.DunebotRunnable;
import ru.trainithard.dunebot.service.telegram.command.CallbackSymbol;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
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
    private final ExternalMessageFactory messageFactory;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: LEADER_ACCEPT started", logId());
        String[] callbackData = commandMessage.getCallback().split(CallbackSymbol.SUBMIT_LEADERS_CALLBACK_SYMBOL.getSymbol());
        long matchId = Long.parseLong(callbackData[0]);
        long leaderId = Long.parseLong(callbackData[1]);

        Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow();
        validateMatchIsNotFinished(commandMessage, match);
        log.debug("{}: match {} found and validated", logId(), matchId);

        Leader submittedLeader = leaderRepository.findById(leaderId).orElseThrow();
        log.debug("{}: leader {} found", logId(), submittedLeader.getId());

        MatchLeaderSubmit matchLeaderSubmit = getMatchLeaderSubmit(commandMessage, match, submittedLeader, matchId);
        MatchPlayer submittedPlayer = matchLeaderSubmit.submittedPlayer();
        submittedPlayer.setLeader(submittedLeader);
        matchPlayerRepository.save(submittedPlayer);

        if (matchLeaderSubmit.nextLeaderPlace() == match.getModType().getPlayersCount()) {
            log.debug("{}: received last leader, match {} received SUBMITTED state", logId(), matchId);
            rescheduleAcceptSubmitTimeoutTask(matchId);
            sendMessages(commandMessage, match).whenComplete((message, throwable) -> {
                if (throwable == null) {
                    matchRepository.findById(matchId).ifPresent(savedMatch -> {
                        ExternalMessageId externalSubmitId = new ExternalMessageId(message);
                        savedMatch.setState(MatchState.SUBMITTED);
                        savedMatch.setExternalSubmitId(externalSubmitId);
                        matchRepository.save(savedMatch);
                    });
                }
            });
        }

        log.debug("{}: match {}, match_player {} saved", logId(), matchId, submittedLeader.getId());
        log.debug("{}: LEADER_ACCEPT ended", logId());
    }

    private MatchLeaderSubmit getMatchLeaderSubmit(CommandMessage commandMessage, Match match, Leader submittedLeader, long matchId) {
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

        log.debug("{}: submit for leader {}, max place {}", logId(), submittedLeader.getId(), submittedLeaderMaxPlace);
        return new MatchLeaderSubmit(nextLeaderPlace, submittedPlayer);
    }

    private CompletableFuture<ExternalMessageDto> sendMessages(CommandMessage commandMessage, Match match) {
        sendSubmitterSubmitCompletedMessages(commandMessage, match.getMatchPlayers());
        sendPlayersSubmitCompletedMessages(match);
        ExternalMessage matchSuccessfulFinishMessage = messageFactory.getMatchSuccessfulFinishMessage(match);
        MessageDto messageDto = new MessageDto(match.getExternalPollId(), matchSuccessfulFinishMessage);
        return messagingService.sendMessageAsync(messageDto);
    }

    private void rescheduleAcceptSubmitTimeoutTask(long matchId) {
        DuneBotTaskId taskId = new DuneBotTaskId(DuneTaskType.SUBMIT_ACCEPT_TIMEOUT, matchId);
        DunebotRunnable submitAcceptTimeoutTask = taskFactory.createInstance(taskId);
        int acceptSubmitTimeout = appSettingsService.getIntSetting(AppSettingKey.ACCEPT_SUBMIT_TIMEOUT);
        Instant startTime = Instant.now(clock).plus(acceptSubmitTimeout, ChronoUnit.MINUTES);
        taskScheduler.rescheduleSingleRunTask(submitAcceptTimeoutTask, taskId, startTime);
        taskScheduler.cancelSingleRunTask(new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT_NOTIFICATION, matchId));
        taskScheduler.cancelSingleRunTask(new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT, matchId));
    }

    private void validateLeaderIsNotSubmitted(CommandMessage commandMessage, MatchPlayer matchPlayer, Leader leader, long matchId) {
        if (leader.equals(matchPlayer.getLeader())) {
            String playerName = matchPlayer.getPlayer().getFriendlyName();
            String message = String.format(ALREADY_ACCEPTED_SUBMIT_MESSAGE_TEMPLATE, leader.getName(), playerName, matchId);
            throw new AnswerableDuneBotException(message, commandMessage);
        }
    }

    private void sendSubmitterSubmitCompletedMessages(CommandMessage commandMessage, List<MatchPlayer> matchPlayers) {
        ExternalMessage message = externalMessageFactory.getFinishedLeadersSubmitMessage(matchPlayers);
        messagingService.sendMessageAsync(new MessageDto(commandMessage, message, null));
    }

    private void sendPlayersSubmitCompletedMessages(Match match) {
        Player submitter = match.getSubmitter();
        Integer acceptSubmitTimeout = appSettingsService.getIntSetting(AppSettingKey.ACCEPT_SUBMIT_TIMEOUT);
        match.getMatchPlayers().stream()
                .filter(matchPlayer -> !matchPlayer.getPlayer().equals(submitter))
                .forEach(matchPlayer -> {
                    String chatId = Long.toString(matchPlayer.getPlayer().getExternalChatId());
                    String submitterName = submitter.getFriendlyName();
                    ExternalMessage message = externalMessageFactory
                            .getFinishedSubmitParticipantMessage(matchPlayer, submitterName, acceptSubmitTimeout);
                    MessageDto messageDto = new MessageDto(chatId, message, null, null);
                    messagingService.sendMessageAsync(messageDto);
                });
    }

    @Override
    public Command getCommand() {
        return Command.LEADER_ACCEPT;
    }

    private record MatchLeaderSubmit(int nextLeaderPlace, MatchPlayer submittedPlayer) {
    }
}
