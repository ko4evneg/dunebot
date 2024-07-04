package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CallbackSymbol;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;
import ru.trainithard.dunebot.service.telegram.factory.messaging.KeyboardsFactory;

import java.util.List;
import java.util.Objects;

import static ru.trainithard.dunebot.configuration.SettingConstants.NOT_PARTICIPATED_MATCH_PLACE;

@Service
@RequiredArgsConstructor
public class PlayersAcceptCommandProcessor extends AcceptSubmitCommandProcessor {
    private static final String ALREADY_ACCEPTED_SUBMIT_MESSAGE_TEMPLATE =
            "Вы уже назначили игроку %s место %d. Выберите другого игрока, или используйте команду '/resubmit %d', чтобы начать заново.";
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final ExternalMessageFactory externalMessageFactory;
    private final KeyboardsFactory keyboardsFactory;

    @Override
    public void process(CommandMessage commandMessage) {
        String[] callbackData = commandMessage.getCallback().split(CallbackSymbol.SUBMIT_PLAYERS_CALLBACK_SYMBOL.getSymbol());
        long matchId = Long.parseLong(callbackData[0]);
        long matchPlayerId = Long.parseLong(callbackData[1]);
        Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow();
        validateMatchIsNotFinished(commandMessage, match);

        int maxSubmittedPlace = 0;
        MatchPlayer submittedPlayer = null;
        for (MatchPlayer matchPlayer : match.getMatchPlayers()) {
            Integer place = matchPlayer.getPlace();
            if (matchPlayer.getId().equals(matchPlayerId)) {
                submittedPlayer = matchPlayer;
                validatePlayerIsNotSubmitted(commandMessage, matchPlayer, place, matchId);
            }
            if (place != null && place != NOT_PARTICIPATED_MATCH_PLACE) {
                maxSubmittedPlace = Math.max(place, maxSubmittedPlace);
            }
        }

        Objects.requireNonNull(submittedPlayer);
        int nextCandidatePlace = maxSubmittedPlace + 1;
        submittedPlayer.setPlace(nextCandidatePlace);
        matchPlayerRepository.save(submittedPlayer);

        ModType modType = match.getModType();
        if (nextCandidatePlace == modType.getPlayersCount()) {
            sendPlayersSubmitCompletedMessages(commandMessage, match);
        }
    }

    private void validatePlayerIsNotSubmitted(CommandMessage commandMessage, MatchPlayer matchPlayer, Integer place, long matchId) {
        if (place != null) {
            String playerName = matchPlayer.getPlayer().getFriendlyName();
            String message = String.format(ALREADY_ACCEPTED_SUBMIT_MESSAGE_TEMPLATE, playerName, place, matchId);
            throw new AnswerableDuneBotException(message, commandMessage);
        }
    }

    private void sendPlayersSubmitCompletedMessages(CommandMessage commandMessage, Match match) {
        ExternalMessage playerSubmitFinishMessage = externalMessageFactory.getFinishedPlayersSubmitMessage(match.getMatchPlayers());
        messagingService.sendMessageAsync(new MessageDto(commandMessage, playerSubmitFinishMessage, null));
        ExternalMessage leadersSubmitMessage = externalMessageFactory.getLeadersSubmitMessage(match.getId());
        List<List<ButtonDto>> leadersKeyboard = keyboardsFactory.getSubmitLeadersKeyboard(match);
        messagingService.sendMessageAsync(new MessageDto(commandMessage, leadersSubmitMessage, leadersKeyboard));
    }

    @Override
    public Command getCommand() {
        return Command.PLAYER_ACCEPT;
    }
}
