package ru.trainithard.dunebot.service.telegram.command.processor;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.SubmitValidatedMatchRetriever;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
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
    private static final String MATCH_PLACE_SELECTION_MESSAGE_TEMPLATE = "Выберите место, которое вы заняли в матче %d:";

    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;
    private final MatchRepository matchRepository;
    private final MatchFinishingService matchFinishingService;
    private final TaskScheduler dunebotTaskScheduler;
    private final SubmitValidatedMatchRetriever validatedMatchRetriever;
    private final SettingsService settingsService;
    private final Clock clock;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: SUBMIT started", logId());
        process(validatedMatchRetriever.getValidatedSubmitMatch(commandMessage));
        log.debug("{}: SUBMIT ended", logId());
    }

    void process(Match match) {
        log.debug("{}: SUBMIT(internal) started", logId());

        List<MatchPlayer> registeredMatchPlayers = match.getMatchPlayers();
        for (MatchPlayer matchPlayer : registeredMatchPlayers) {
            log.debug("{}: matchPlayer {} processing...", logId(), matchPlayer.getId());

            MessageDto submitCallbackMessage = getSubmitCallbackMessage(matchPlayer, registeredMatchPlayers, match.getId());
            CompletableFuture<ExternalMessageDto> messageCompletableFuture = messagingService.sendMessageAsync(submitCallbackMessage);
            messageCompletableFuture.whenComplete((message, throwable) -> {
                if (throwable == null) {
                    //todo: transaction?
                    matchPlayer.setSubmitMessageId(new ExternalMessageId(message));
                    matchPlayerRepository.save(matchPlayer);
                    log.debug("{}: matchPlayer {} saved", logId(), matchPlayer.getId());
                    if (match.getState() == MatchState.NEW) {
                        match.setState(MatchState.ON_SUBMIT);
                        matchRepository.save(match);
                        log.debug("{}: match {} saved", logId(), match.getId());
                    }
                } else {
                    log.error(logId() + ": sending external message encountered an exception", throwable);
                }
            });
        }

        int finishMatchTimeout = settingsService.getIntSetting(SettingKey.FINISH_MATCH_TIMEOUT);
        Instant forcedFinishTime = Instant.now(clock).plus(finishMatchTimeout, ChronoUnit.MINUTES);
        ExternalMessage forcedFinishMessage = getForcedFinishMessage(match.getId());
        dunebotTaskScheduler.schedule(() -> matchFinishingService
                .finishNotSubmittedMatch(match.getId(), forcedFinishMessage), forcedFinishTime);
        log.debug("{}: forced finish match task scheduled to {}", logId(), forcedFinishTime);

        log.debug("{}: SUBMIT(internal) ended", logId());
    }

    private MessageDto getSubmitCallbackMessage(MatchPlayer matchPlayer, List<MatchPlayer> registeredMatchPlayers, long matchId) {
        String text = String.format(MATCH_PLACE_SELECTION_MESSAGE_TEMPLATE, matchId);
        List<List<ButtonDto>> pollKeyboard = getSubmitCallbackKeyboard(registeredMatchPlayers, matchId);
        String playersChatId = Long.toString(matchPlayer.getPlayer().getExternalChatId());
        return new MessageDto(playersChatId, new ExternalMessage(text), null, pollKeyboard);
    }

    private List<List<ButtonDto>> getSubmitCallbackKeyboard(List<MatchPlayer> registeredMatchPlayers, long matchIdString) {
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

    private ExternalMessage getForcedFinishMessage(Long matchId) {
        return new ExternalMessage()
                .startBold().append("Матч ").append(matchId).endBold()
                .append(" завершен без результата, так как превышено максимальное количество попыток регистрации мест");
    }

    @Override
    public Command getCommand() {
        return Command.SUBMIT;
    }
}
