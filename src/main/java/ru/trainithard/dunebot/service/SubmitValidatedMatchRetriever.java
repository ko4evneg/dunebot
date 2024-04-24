package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.MatchNotExistsException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitValidatedMatchRetriever {
    private static final String SUBMIT_NOT_ALLOWED_EXCEPTION_MESSAGE = "Вы не можете инициировать публикацию этого матча";
    private static final String FINISHED_MATCH_SUBMIT_EXCEPTION_MESSAGE = "Запрещено регистрировать результаты завершенных матчей";
    private static final String NOT_ENOUGH_PLAYERS_EXCEPTION_MESSAGE =
            "В опросе участвует меньше игроков чем нужно для матча. Все игроки должны войти в опрос";
    private static final String ALREADY_SUBMITTED_EXCEPTION_MESSAGE = "Запрос на публикацию этого матча уже сделан";
    private static final String MATCH_NOT_EXISTS_EXCEPTION = "Матча с таким ID не существует!";

    private final MatchRepository matchRepository;

    public Match getValidatedResubmitMatch(CommandMessage commandMessage) {
        log.debug("{}: match for RESUBMIT retrieval...", LogId.get());
        long telegramChatId = commandMessage.getChatId();
        try {
            long matchId = Long.parseLong(commandMessage.getArgument(1));
            Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow(MatchNotExistsException::new);
            log.debug("{}: match {} found...", LogId.get(), matchId);
            validateMatch(commandMessage, match, true);
            return match;
        } catch (NumberFormatException | MatchNotExistsException exception) {
            throw new AnswerableDuneBotException(MATCH_NOT_EXISTS_EXCEPTION, exception, telegramChatId);
        }
    }

    public Match getValidatedSubmitMatch(CommandMessage commandMessage) {
        log.debug("{}: match for SUBMIT retrieval...", LogId.get());
        long telegramChatId = commandMessage.getChatId();
        try {
            long matchId = Long.parseLong(commandMessage.getArgument(1));
            Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow(MatchNotExistsException::new);
            log.debug("{}: match {} found...", LogId.get(), matchId);
            validateMatch(commandMessage, match, false);
            return match;
        } catch (NumberFormatException | MatchNotExistsException exception) {
            throw new AnswerableDuneBotException(MATCH_NOT_EXISTS_EXCEPTION, exception, telegramChatId);
        }
    }

    private void validateMatch(CommandMessage commandMessage, Match match, boolean isResubmit) {
        log.debug("{}: match {} validation...", LogId.get(), match.getId());
        long telegramChatId = commandMessage.getChatId();
        MatchState matchState = match.getState();
        if (MatchState.getEndedMatchStates().contains(matchState)) {
            throw new AnswerableDuneBotException(FINISHED_MATCH_SUBMIT_EXCEPTION_MESSAGE, telegramChatId);
        }
        if (!match.hasEnoughPlayers()) {
            throw new AnswerableDuneBotException(NOT_ENOUGH_PLAYERS_EXCEPTION_MESSAGE, telegramChatId);
        }
        if (!isResubmit && MatchState.getSubmitStates().contains(matchState)) {
            throw new AnswerableDuneBotException(ALREADY_SUBMITTED_EXCEPTION_MESSAGE, telegramChatId);
        }
        if (!isSubmitAllowed(commandMessage, match)) {
            throw new AnswerableDuneBotException(SUBMIT_NOT_ALLOWED_EXCEPTION_MESSAGE, telegramChatId);
        }
        log.debug("{}: match {} successfully validated", LogId.get(), match.getId());
    }

    private boolean isSubmitAllowed(CommandMessage commandMessage, Match match) {
        return match.getMatchPlayers().stream()
                .anyMatch(matchPlayer -> matchPlayer.getPlayer().getExternalId() == commandMessage.getUserId());
    }
}
