package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class AcceptSubmitCommandProcessor extends CommandProcessor {
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final MatchFinishingService matchFinishingService;

    @Override
    public void process(CommandMessage commandMessage) {
        Callback callback = new Callback(commandMessage.getCallback());
        Match match = matchRepository.findByIdWithMatchPlayers(callback.matchId).orElseThrow();
        MatchPlayer matchPlayer = match.getMatchPlayers().stream()
                .filter(mPlayer -> mPlayer.getPlayer().getExternalId() == commandMessage.getUserId())
                .findFirst().orElseThrow();
        if (matchPlayer.getCandidatePlace() == null) {
            matchPlayer.setCandidatePlace(callback.candidatePlace);
            match.setSubmitsCount(match.getSubmitsCount() + 1);
            transactionTemplate.executeWithoutResult(status -> {
                matchRepository.save(match);
                matchPlayerRepository.saveAll(match.getMatchPlayers());
            });
            if (match.areAllSubmitsReceived()) {
                //validate
                matchFinishingService.finishMatch(match.getId());
            }
        }
    }

    @Override
    public Command getCommand() {
        return Command.ACCEPT_SUBMIT;
    }

    private class Callback {
        private final long matchId;
        private final int candidatePlace;

        public Callback(String callbackText) {
            String[] callbackData = callbackText.split("__");
            this.matchId = Long.parseLong(callbackData[0]);
            this.candidatePlace = Integer.parseInt(callbackData[1]);
        }
    }
}
