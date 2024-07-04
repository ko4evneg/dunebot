package ru.trainithard.dunebot.service.telegram.command.processor.submit;

import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;

abstract class AcceptSubmitCommandProcessor extends CommandProcessor {
    private static final String FINISHED_MATCH_SUBMIT_MESSAGE_TEMPLATE = "Матч %d уже завершен. Регистрация результата более невозможна.";

    void validateMatchIsNotFinished(CommandMessage commandMessage, Match match) {
        if (MatchState.getEndedMatchStates().contains(match.getState())) {
            String message = String.format(FINISHED_MATCH_SUBMIT_MESSAGE_TEMPLATE, match.getId());
            throw new AnswerableDuneBotException(message, commandMessage);
        }
    }
}
