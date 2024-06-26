package ru.trainithard.dunebot.service.telegram.command.processor;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.SubmitValidatedMatchRetriever;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.task.DuneScheduledTaskFactory;
import ru.trainithard.dunebot.service.task.DunebotRunnable;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static ru.trainithard.dunebot.configuration.SettingConstants.NOT_PARTICIPATED_MATCH_PLACE;

/**
 * Initiates first match results requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitCommandProcessor extends CommandProcessor {
    private static final String MATCH_PLACE_SELECTION_MESSAGE_TEMPLATE = "Выберите место, которое вы заняли в матче %d:";
    private static final int RESUBMIT_TIME_LIMIT_STEP = 60 * 7;

    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final DuneBotTaskScheduler taskScheduler;
    private final SubmitValidatedMatchRetriever validatedMatchRetriever;
    private final AppSettingsService appSettingsService;
    private final Clock clock;
    private final DuneScheduledTaskFactory taskFactory;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: SUBMIT started", logId());
        process(validatedMatchRetriever.getValidatedSubmitMatch(commandMessage));
        log.debug("{}: SUBMIT ended", logId());
    }

    void process(Match match) {
        int logId = logId();
        log.debug("{}: SUBMIT(internal) started", logId);

        if (match.getState() == MatchState.NEW) {
            match.setState(MatchState.ON_SUBMIT);
            matchRepository.save(match);
            log.debug("{}: match {} saved state ON_SUBMIT", logId, match.getId());
        }

        List<MatchPlayer> registeredMatchPlayers = match.getMatchPlayers();
        for (MatchPlayer matchPlayer : registeredMatchPlayers) {
            log.debug("{}: matchPlayer {} processing...", logId, matchPlayer.getId());

            MessageDto submitCallbackMessage = getSubmitCallbackMessage(matchPlayer, match);
            CompletableFuture<ExternalMessageDto> messageCompletableFuture = messagingService.sendMessageAsync(submitCallbackMessage);
            messageCompletableFuture.whenComplete((message, throwable) -> {
                if (throwable == null) {
                    //todo: transaction?
                    matchPlayerRepository.findById(matchPlayer.getId())
                            .ifPresent(callbackMatchPlayer -> {
                                matchPlayer.setSubmitMessageId(new ExternalMessageId(message));
                                matchPlayerRepository.save(matchPlayer);
                                log.debug("{}: matchPlayer {} (player {}) submitId saved",
                                        logId, matchPlayer.getId(), matchPlayer.getPlayer().getId());
                            });
                } else {
                    //TODO: retry
                    log.error(logId + ": sending external message encountered an exception", throwable);
                }
            });
        }

        rescheduleForcedFailFinish(match.getId());

        log.debug("{}: SUBMIT(internal) ended", logId);
    }

    private void rescheduleForcedFailFinish(long matchId) {
        int finishMatchTimeout = appSettingsService.getIntSetting(AppSettingKey.FINISH_MATCH_TIMEOUT);
        Instant forcedFinishTime = Instant.now(clock).plus(finishMatchTimeout, ChronoUnit.MINUTES);
        DuneBotTaskId submitTimeoutTaskId = new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT, matchId);
        ScheduledFuture<?> oldFailFinishTask = taskScheduler.get(submitTimeoutTaskId);
        if (oldFailFinishTask != null) {
            long delay = oldFailFinishTask.getDelay(TimeUnit.SECONDS);
            forcedFinishTime = Instant.now(clock).plus(RESUBMIT_TIME_LIMIT_STEP + delay, ChronoUnit.SECONDS);
        }
        DunebotRunnable submitTimeoutTask = taskFactory.createInstance(submitTimeoutTaskId);
        taskScheduler.rescheduleSingleRunTask(submitTimeoutTask, submitTimeoutTaskId, forcedFinishTime);
    }

    private MessageDto getSubmitCallbackMessage(MatchPlayer matchPlayer, Match match) {
        Long matchId = match.getId();
        String text = String.format(MATCH_PLACE_SELECTION_MESSAGE_TEMPLATE, matchId);
        List<List<ButtonDto>> pollKeyboard = getSubmitCallbackKeyboard(match);
        String playersChatId = Long.toString(matchPlayer.getPlayer().getExternalChatId());
        return new MessageDto(playersChatId, new ExternalMessage(text), null, pollKeyboard);
    }

    private List<List<ButtonDto>> getSubmitCallbackKeyboard(Match match) {
        List<ButtonDto> buttons = new ArrayList<>();
        String callbackPrefix = match.getId() + "__";
        for (int i = 0; i < match.getModType().getPlayersCount(); i++) {
            int callbackCandidatePlace = i + 1;
            ButtonDto buttonDto = new ButtonDto(Integer.toString(callbackCandidatePlace), callbackPrefix + callbackCandidatePlace);
            buttons.add(buttonDto);
        }
        buttons.add(new ButtonDto("не участвовал(а)", callbackPrefix + NOT_PARTICIPATED_MATCH_PLACE));
        return Lists.partition(buttons, 2);
    }

    @Override
    public Command getCommand() {
        return Command.SUBMIT;
    }
}
