package ru.trainithard.dunebot.service.telegram.command.processor.submit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.MatchNotExistsException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;
import ru.trainithard.dunebot.service.telegram.factory.messaging.KeyboardsFactory;
import ru.trainithard.dunebot.service.telegram.validator.SubmitMatchValidator;

import java.util.List;

import static ru.trainithard.dunebot.exception.MatchNotExistsException.MATCH_NOT_EXISTS_EXCEPTION;

/**
 * Resets current results and initiates match results requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResubmitCommandProcessor extends CommandProcessor {
    private final MatchRepository matchRepository;
    private final SubmitMatchValidator submitMatchValidator;
    private final KeyboardsFactory keyboardsFactory;
    private final ExternalMessageFactory messageFactory;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: RESUBMIT started", logId());
        try {
            long matchId = Long.parseLong(commandMessage.getArgument(1));
            Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow(MatchNotExistsException::new);
            submitMatchValidator.validateReSubmitMatch(commandMessage, match);

            ExternalMessage resubmitMessage = messageFactory.getResubmitMessage();
            long submitterId = match.getSubmitter().getExternalId();
            List<List<ButtonDto>> resubmitKeyboard = keyboardsFactory.getResubmitKeyboard(matchId, commandMessage.getUserId(), submitterId);
            MessageDto messageDto = new MessageDto(commandMessage, resubmitMessage, resubmitKeyboard);
            messagingService.sendMessageAsync(messageDto);
        } catch (NumberFormatException | MatchNotExistsException exception) {
            throw new AnswerableDuneBotException(MATCH_NOT_EXISTS_EXCEPTION, exception, commandMessage.getChatId());
        }
        log.debug("{}: RESUBMIT ended", logId());
    }

    @Override
    public Command getCommand() {
        return Command.RESUBMIT;
    }
}
