package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CancelCommandProcessor extends CommandProcessor {
    private static final String FINISHED_MATCH_EXCEPTION_MESSAGE = "Запрещено отменять завершенные матчи!";

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage) {
        playerRepository.findByExternalId(commandMessage.getUserId()).ifPresent(player -> {
            Optional<Match> latestOwnedMatchOptional = matchRepository.findLatestOwnedMatch(player.getId());
            if (latestOwnedMatchOptional.isPresent()) {
                Match latestOwnedMatch = latestOwnedMatchOptional.get();
                if (latestOwnedMatch.isFinished()) {
                    throw new AnswerableDuneBotException(FINISHED_MATCH_EXCEPTION_MESSAGE, player.getExternalChatId());
                }
                messagingService.deleteMessageAsync(latestOwnedMatch.getExternalPollId());
                transactionTemplate.executeWithoutResult(status -> {
                    matchRepository.delete(latestOwnedMatch);
                    matchPlayerRepository.deleteAll(latestOwnedMatch.getMatchPlayers());
                });
            }
        });
    }

    @Override
    public Command getCommand() {
        return Command.CANCEL;
    }
}
