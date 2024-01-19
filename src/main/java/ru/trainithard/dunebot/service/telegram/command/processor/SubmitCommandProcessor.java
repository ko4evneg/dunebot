package ru.trainithard.dunebot.service.telegram.command.processor;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.validator.SubmitValidatedMatchRetriever;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class SubmitCommandProcessor extends CommandProcessor {
    private static final String TIMEOUT_MATCH_FINISH_MESSAGE = "Матч %d завершен без результата, так как превышено максимальное количество попыток регистрации мест";
    private static final String MATCH_PLACE_SELECTION_MESSAGE_TEMPLATE = "Выберите место, которое вы заняли в матче %s:";

    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;
    private final MatchRepository matchRepository;
    private final MatchFinishingService matchFinishingService;
    private final TaskScheduler dunebotTaskScheduler;
    private final SubmitValidatedMatchRetriever validatedMatchRetriever;
    private final Clock clock;

    @Override
    public void process(CommandMessage commandMessage) {
        process(validatedMatchRetriever.getValidatedMatch(commandMessage));
    }

    void process(Match match) {
        List<MatchPlayer> registeredMatchPlayers = match.getMatchPlayers();
        for (MatchPlayer matchPlayer : registeredMatchPlayers) {
            deleteOldSubmitMessage(matchPlayer);
            MessageDto submitCallbackMessage = getSubmitCallbackMessage(matchPlayer, registeredMatchPlayers, match.getId().toString());
            CompletableFuture<ExternalMessageDto> messageCompletableFuture = messagingService.sendMessageAsync(submitCallbackMessage);
            messageCompletableFuture.whenComplete((message, throwable) -> {
                // TODO: handle throwable (rollback)
                matchPlayer.setSubmitMessageId(new ExternalMessageId(message));
                matchPlayerRepository.save(matchPlayer);
                if (!match.isOnSubmit()) {
                    match.setOnSubmit(true);
                    matchRepository.save(match);
                }
            });
        }

        Instant forcedFinishTime = Instant.now(clock).plus(SettingConstants.FINISH_MATCH_TIMEOUT, ChronoUnit.MINUTES);
        String forcedFinishMessage = String.format(TIMEOUT_MATCH_FINISH_MESSAGE, match.getId());
        dunebotTaskScheduler.schedule(() -> matchFinishingService
                .finishUnsuccessfullySubmittedMatch(match.getId(), forcedFinishMessage), forcedFinishTime);
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
        for (int i = 0; i < registeredMatchPlayers.size(); i++) {
            ButtonDto buttonDto = new ButtonDto(Integer.toString(i + 1), matchIdString + "__" + (i + 1));
            buttons.add(buttonDto);
        }
        buttons.add(new ButtonDto("не участвовал(а)", matchIdString + "__-1"));
        return Lists.partition(buttons, 2);
    }

    @Override
    public Command getCommand() {
        return Command.SUBMIT;
    }
}
