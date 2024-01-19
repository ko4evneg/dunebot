package ru.trainithard.dunebot.service.telegram.command.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.MatchNotExistsException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class SubmitValidatedMatchRetriever {
    private static final String SUBMIT_NOT_ALLOWED_EXCEPTION_MESSAGE = "Вы не можете инициировать публикацию этого матча";
    private static final String FINISHED_MATCH_SUBMIT_EXCEPTION_MESSAGE = "Запрещено регистрировать результаты завершенных матчей";
    private static final String NOT_ENOUGH_PLAYERS_EXCEPTION_MESSAGE = "В опросе участвует меньше игроков чем нужно для матча. Все игроки должны войти в опрос";
    private static final String ALREADY_SUBMITTED_EXCEPTION_MESSAGE = "Запрос на публикацию этого матча уже сделан";
    private static final String MATCH_NOT_EXISTS_EXCEPTION = "Матча с таким ID не существует!";

    private final MatchRepository matchRepository;

    public Match getValidatedMatch(CommandMessage commandMessage) {
        long telegramChatId = commandMessage.getChatId();
        try {
            long matchId = Long.parseLong(commandMessage.getArgument(1));
            Match match = matchRepository.findByIdWithMatchPlayers(matchId).orElseThrow(MatchNotExistsException::new);
            validateMatch(telegramChatId, match);
            validateSubmitAllowed(commandMessage, match, telegramChatId);
            return match;
        } catch (NumberFormatException | MatchNotExistsException exception) {
            throw new AnswerableDuneBotException(MATCH_NOT_EXISTS_EXCEPTION, telegramChatId);
        }
    }

    private void validateMatch(long telegramChatId, Match match) {
        if (match.isFinished()) {
            throw new AnswerableDuneBotException(FINISHED_MATCH_SUBMIT_EXCEPTION_MESSAGE, telegramChatId);
        }
        if (match.getPositiveAnswersCount() < match.getModType().getPlayersCount()) {
            throw new AnswerableDuneBotException(NOT_ENOUGH_PLAYERS_EXCEPTION_MESSAGE, telegramChatId);
        }
        if (match.isOnSubmit()) {
            throw new AnswerableDuneBotException(ALREADY_SUBMITTED_EXCEPTION_MESSAGE, telegramChatId);
        }
    }

    private void validateSubmitAllowed(CommandMessage commandMessage, Match match, long telegramChatId) {
        if (!isSubmitAllowed(commandMessage, match)) {
            throw new AnswerableDuneBotException(SUBMIT_NOT_ALLOWED_EXCEPTION_MESSAGE, telegramChatId);
        }
    }

    private boolean isSubmitAllowed(CommandMessage commandMessage, Match match) {
        return match.getMatchPlayers().stream()
                .anyMatch(matchPlayer -> matchPlayer.getPlayer().getExternalId() == commandMessage.getUserId());
    }
}
