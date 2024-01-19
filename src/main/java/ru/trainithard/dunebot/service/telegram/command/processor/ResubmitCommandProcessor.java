package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.MatchNotExistsException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResubmitCommandProcessor extends CommandProcessor {
    private static final String TIMEOUT_MATCH_FINISH_MESSAGE = "Матч %d завершен без результата, так как превышено максимальное количество попыток регистрации мест (%d)";

    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;
    private final MatchRepository matchRepository;
    private final MatchFinishingService matchFinishingService;
    private final Clock clock;
    private final SubmitCommandProcessor submitCommandProcessor;


    @Override
    public void process(CommandMessage commandMessage) {
        Match match = getValidatedMatch(commandMessage);
        if (!match.isResubmitAllowed(SettingConstants.RESUBMITS_LIMIT)) {
            matchFinishingService.finishUnsuccessfullySubmittedMatch(match.getId(), String.format(TIMEOUT_MATCH_FINISH_MESSAGE, match.getId(), SettingConstants.RESUBMITS_LIMIT));
        }

        process(match);
    }

    // TODO:  extract to abstact class
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

    void process(Match match) {
        List<MatchPlayer> registeredMatchPlayers = match.getMatchPlayers();
        updateSubmitsData(match);
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.saveAll(registeredMatchPlayers);
        });
        submitCommandProcessor.process(match);
    }

    private void updateSubmitsData(Match match) {
        match.setSubmitsRetryCount(match.getSubmitsRetryCount() + 1);
        match.setSubmitsCount(0);
        match.getMatchPlayers().forEach(matchPlayer -> {
            matchPlayer.setCandidatePlace(null);
            matchPlayer.setSubmitMessageId(null);
        });
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

    @Override
    public Command getCommand() {
        return Command.RESUBMIT;
    }
}
