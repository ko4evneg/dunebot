package ru.trainithard.dunebot.service.telegram.command.processor;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.MatchNotExistsException;
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

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class SubmitCommandProcessor extends CommandProcessor {
    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;
    private final MatchRepository matchRepository;
    private final MatchFinishingService matchFinishingService;
    private final Clock clock;

    private static final int FINISH_MATCH_TIMEOUT = 120;

    @Override
    public void process(CommandMessage commandMessage) {
        Match match = getValidatedMatch(commandMessage);
        List<MatchPlayer> registeredMatchPlayers = match.getMatchPlayers();
        for (MatchPlayer matchPlayer : registeredMatchPlayers) {
            MessageDto submitCallbackMessage = getSubmitCallbackMessage(matchPlayer, registeredMatchPlayers, commandMessage.getArgument(1));
            CompletableFuture<ExternalMessageDto> messageCompletableFuture = messagingService.sendMessageAsync(submitCallbackMessage);
            messageCompletableFuture.whenComplete((message, throwable) -> {
                matchPlayer.setSubmitMessageId(new ExternalMessageId(message));
                matchPlayerRepository.save(matchPlayer);
                if (!match.isOnSubmit()) {
                    match.setOnSubmit(true);
                    matchRepository.save(match);
                }
            });
        }
        matchFinishingService.scheduleForceFinishMatch(match.getId(), Instant.now(clock).plus(FINISH_MATCH_TIMEOUT, ChronoUnit.MINUTES));
    }

    private Match getValidatedMatch(CommandMessage commandMessage) {
        long telegramChatId = commandMessage.getChatId();
        try {
            long matchId = Long.parseLong(commandMessage.getArgument(1));
            Match match = matchRepository.findByIdWithMatchPlayers(matchId).orElseThrow(MatchNotExistsException::new);
            validate(telegramChatId, match);

            boolean isSubmitAllowed = match.getMatchPlayers().stream()
                    .anyMatch(matchPlayer -> matchPlayer.getPlayer().getExternalId() == commandMessage.getUserId());
            if (!isSubmitAllowed) {
                throw new AnswerableDuneBotException("Вы не можете инициировать публикацию этого матча", telegramChatId);
            }
            return match;
        } catch (NumberFormatException | MatchNotExistsException exception) {
            throw new AnswerableDuneBotException("Матча с таким ID не существует!", telegramChatId);
        }
    }

    private static void validate(long telegramChatId, Match match) {
        if (match.isFinished()) {
            throw new AnswerableDuneBotException("Запрещено регистрировать результаты завершенных матчей", telegramChatId);
        }
        if (match.getPositiveAnswersCount() < match.getModType().getPlayersCount()) {
            throw new AnswerableDuneBotException("В опросе участвует меньше игроков чем нужно для матча. Все игроки должны войти в опрос", telegramChatId);
        }
        if (match.isOnSubmit()) {
            throw new AnswerableDuneBotException("Запрос на публикацию этого матча уже сделан", telegramChatId);
        }
    }

    private MessageDto getSubmitCallbackMessage(MatchPlayer matchPlayer, List<MatchPlayer> registeredMatchPlayers, String matchIdString) {
        String text = String.format("Выберите место, которое вы заняли в матче %s:", matchIdString);
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
