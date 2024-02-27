package ru.trainithard.dunebot.service.telegram.command.processor;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.SubmitValidatedMatchRetriever;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ru.trainithard.dunebot.configuration.SettingConstants.NOT_PARTICIPATED_MATCH_PLACE;

/**
 * Initiates first match results requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitCommandProcessor extends CommandProcessor {
    private static final String TIMEOUT_MATCH_FINISH_MESSAGE = "*Матч %d* завершен без результата, так как превышено максимальное количество попыток регистрации мест";
    private static final String MATCH_PLACE_SELECTION_MESSAGE_TEMPLATE = "Выберите место, которое вы заняли в матче %s:";

    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;
    private final MatchRepository matchRepository;
    private final MatchFinishingService matchFinishingService;
    private final TaskScheduler dunebotTaskScheduler;
    private final SubmitValidatedMatchRetriever validatedMatchRetriever;
    private final SettingsService settingsService;
    private final Clock clock;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        process(validatedMatchRetriever.getValidatedSubmitMatch(commandMessage), loggingId);
    }

    void process(Match match, int loggingId) {
        log.debug("{}: submit started", loggingId);

        List<MatchPlayer> registeredMatchPlayers = match.getMatchPlayers();
        for (MatchPlayer matchPlayer : registeredMatchPlayers) {
            log.debug("{}: matchPlayer processing started, id: {}", loggingId, matchPlayer.getId());

            deleteOldSubmitMessage(matchPlayer);
            MessageDto submitCallbackMessage = getSubmitCallbackMessage(matchPlayer, registeredMatchPlayers, match.getId().toString());
            CompletableFuture<ExternalMessageDto> messageCompletableFuture = messagingService.sendMessageAsync(submitCallbackMessage);
            messageCompletableFuture.whenComplete((message, throwable) -> {
                if (throwable == null) {
                    matchPlayer.setSubmitMessageId(new ExternalMessageId(message));
                    matchPlayerRepository.save(matchPlayer);
                    if (match.getState() == MatchState.NEW) {
                        match.setState(MatchState.ON_SUBMIT);
                        matchRepository.save(match);
                    }
                } else {
                    log.error(loggingId + ": sending external message encountered an exception", throwable);
                }
            });
        }

        int finishMatchTimeout = settingsService.getIntSetting(SettingsService.FINISH_MATCH_TIMEOUT_KEY);
        Instant forcedFinishTime = Instant.now(clock).plus(finishMatchTimeout, ChronoUnit.MINUTES);
        String forcedFinishMessage = String.format(TIMEOUT_MATCH_FINISH_MESSAGE, match.getId());
        dunebotTaskScheduler.schedule(() -> matchFinishingService
                .finishUnsuccessfullySubmittedMatch(match.getId(), forcedFinishMessage, loggingId), forcedFinishTime);
        log.debug("{}: forced finish match task scheduled", loggingId);

        log.debug("{}: submit ended", loggingId);
    }

    private void deleteOldSubmitMessage(MatchPlayer matchPlayer) {
        if (matchPlayer.getSubmitMessageId() != null) {
            messagingService.deleteMessageAsync(matchPlayer.getSubmitMessageId());
        }
    }

    private MessageDto getSubmitCallbackMessage(MatchPlayer matchPlayer, List<MatchPlayer> registeredMatchPlayers, String matchIdString) {
        String text = String.format(MATCH_PLACE_SELECTION_MESSAGE_TEMPLATE, matchIdString);
        List<List<ButtonDto>> pollKeyboard = getSubmitCallbackKeyboard(registeredMatchPlayers, matchIdString);
        String playersChatId = Long.toString(matchPlayer.getPlayer().getExternalChatId());
        return new MessageDto(playersChatId, text, null, pollKeyboard);
    }

    private List<List<ButtonDto>> getSubmitCallbackKeyboard(List<MatchPlayer> registeredMatchPlayers, String matchIdString) {
        List<ButtonDto> buttons = new ArrayList<>();
        String callbackPrefix = matchIdString + "__";
        for (int i = 0; i < registeredMatchPlayers.size(); i++) {
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
