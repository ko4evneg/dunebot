package ru.trainithard.dunebot.service.telegram.command.processor.submit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.MatchNotExistsException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.validator.SubmitMatchValidator;

import static ru.trainithard.dunebot.exception.MatchNotExistsException.MATCH_NOT_EXISTS_EXCEPTION;

/**
 * Initiates first match results requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitCommandProcessor extends AbstractSubmitCommandProcessor {
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final SubmitMatchValidator submitMatchValidator;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: SUBMIT started", logId());
        try {
            long matchId = Long.parseLong(commandMessage.getArgument(1));
            Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow(MatchNotExistsException::new);
            submitMatchValidator.validateSubmitMatch(commandMessage, match);
            long chatId = commandMessage.getChatId();

            Player submitter = playerRepository.findByExternalId(chatId).orElseThrow();
            match.setState(MatchState.ON_SUBMIT);
            match.setSubmitter(submitter);
            matchRepository.save(match);
            log.debug("{}: match {} saved state: ON_SUBMIT, submitter: {}", logId(), match.getId(), submitter.getId());

            sendSubmitMessages(match, chatId);
            rescheduleSubmitTasks(match.getId());
        } catch (NumberFormatException | MatchNotExistsException exception) {
            throw new AnswerableDuneBotException(MATCH_NOT_EXISTS_EXCEPTION, exception, commandMessage.getChatId());
        }
        log.debug("{}: SUBMIT ended", logId());
    }

    @Override
    public Command getCommand() {
        return Command.SUBMIT;
    }
}
