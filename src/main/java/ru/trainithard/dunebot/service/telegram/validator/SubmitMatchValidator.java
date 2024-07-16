package ru.trainithard.dunebot.service.telegram.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.service.LogId;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitMatchValidator {
    private static final String SUBMIT_NOT_ALLOWED_EXCEPTION_MESSAGE = "Вы не можете инициировать публикацию этого матча";
    private static final String RESUBMIT_NOT_ALLOWED_BEFORE_SUBMIT =
            "Команда '/resubmit' разрешена только для матчей, уже прошедших регистрацию результатов. " +
            "Для регистрации результатов используйте команду '/submit'";
    private static final String FINISHED_MATCH_SUBMIT_EXCEPTION_MESSAGE = "Запрещено регистрировать результаты завершенных матчей";
    private static final String NOT_ENOUGH_PLAYERS_EXCEPTION_MESSAGE =
            "В опросе участвует меньше игроков чем нужно для матча. Все игроки должны войти в опрос";
    private static final String ALREADY_SUBMITTED_EXCEPTION_MESSAGE = "Запрос на публикацию этого матча уже сделан";
    private static final String FINISHED_SUBMIT_EXCEPTION_MESSAGE_TEMPLATE =
            "Результаты матча уже зарегистрированы. При ошибке в результатах, используйте команду '/resubmit %d'";

    public void validateSubmitMatch(CommandMessage commandMessage, Match match) {
        Long matchId = match.getId();
        log.debug("{}: match {} submit validation...", LogId.get(), matchId);
        long telegramChatId = commandMessage.getChatId();
        MatchState matchState = match.getState();
        commonValidation(commandMessage, match);
        if (MatchState.ON_SUBMIT == matchState) {
            throw new AnswerableDuneBotException(ALREADY_SUBMITTED_EXCEPTION_MESSAGE, telegramChatId);
        }
        if (MatchState.SUBMITTED == matchState) {
            throw new AnswerableDuneBotException(String.format(FINISHED_SUBMIT_EXCEPTION_MESSAGE_TEMPLATE, matchId), telegramChatId);
        }
        log.debug("{}: match {} successfully validated", LogId.get(), matchId);
    }

    public void validateReSubmitMatch(CommandMessage commandMessage, Match match) {
        Long matchId = match.getId();
        log.debug("{}: match {} resubmit validation...", LogId.get(), matchId);
        commonValidation(commandMessage, match);
        if (MatchState.ON_SUBMIT == match.getState() || MatchState.NEW == match.getState()) {
            throw new AnswerableDuneBotException(RESUBMIT_NOT_ALLOWED_BEFORE_SUBMIT, commandMessage.getChatId());
        }
        log.debug("{}: match {} successfully validated", LogId.get(), matchId);
    }

    private void commonValidation(CommandMessage commandMessage, Match match) {
        Long matchId = match.getId();
        log.debug("{}: match {} validation...", LogId.get(), matchId);
        long telegramChatId = commandMessage.getChatId();
        long submitterExternalId = commandMessage.getUserId();
        MatchState matchState = match.getState();

        if (MatchState.getEndedMatchStates().contains(matchState)) {
            throw new AnswerableDuneBotException(FINISHED_MATCH_SUBMIT_EXCEPTION_MESSAGE, telegramChatId);
        }
        if (match.hasMissingPlayers()) {
            getMatchPlayersList(match);
            log.debug("{}: match {} has not enough players to end: {}", LogId.get(), matchId, getMatchPlayersList(match));
            throw new AnswerableDuneBotException(NOT_ENOUGH_PLAYERS_EXCEPTION_MESSAGE, telegramChatId);
        }
        if (!isSubmitFromParticipant(submitterExternalId, match)) {
            throw new AnswerableDuneBotException(SUBMIT_NOT_ALLOWED_EXCEPTION_MESSAGE, telegramChatId);
        }
    }

    private String getMatchPlayersList(Match match) {
        return match.getMatchPlayers().stream()
                .map(matchPlayer -> matchPlayer.getPlayer().getExternalId())
                .map(extId -> Long.toString(extId))
                .collect(Collectors.joining(", "));
    }

    private boolean isSubmitFromParticipant(long submitterExternalId, Match match) {
        return match.getMatchPlayers().stream()
                .anyMatch(matchPlayer -> matchPlayer.getPlayer().getExternalId() == submitterExternalId);
    }
}
