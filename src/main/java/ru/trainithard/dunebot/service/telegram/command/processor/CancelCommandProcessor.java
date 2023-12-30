package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.MatchMakingService;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CancelCommandProcessor implements CommandProcessor {
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MessagingService messagingService;
    private final MatchMakingService matchMakingService;

    @Override
    public void process(CommandMessage commandMessage) {
        playerRepository.findByExternalId(commandMessage.getUserId()).ifPresent(player -> {
            Optional<Match> latestOwnedMatchOptional = matchRepository.findLatestOwnedMatch(player.getId());
            if (latestOwnedMatchOptional.isPresent()) {
                Match latestOwnedMatch = latestOwnedMatchOptional.get();
                if (latestOwnedMatch.isFinished()) {
                    throw new AnswerableDuneBotException("Запрещено отменять завершенные матчи!", player.getExternalChatId());
                }
                messagingService.deleteMessageAsync(latestOwnedMatch.getExternalPollId());
                matchMakingService.cancelMatch(latestOwnedMatch);
            }
        });
    }

    @Override
    public Command getCommand() {
        return Command.CANCEL;
    }
}
