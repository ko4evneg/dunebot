package ru.trainithard.dunebot.service.telegram.factory.messaging;

import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;

import java.util.Collection;
import java.util.List;

public interface ExternalMessageFactory {
    ExternalMessage getGuestMessageDto(Player player);

    ExternalMessage getFinishReasonMessage(Match match, boolean isFailedByResubmitsLimit);

    ExternalMessage getStartMessage(Match match, List<String> regularPlayerMentions,
                                    List<String> guestPlayerMentions, List<String> blockedChatGuests);

    ExternalMessage getHostMessage(Player hoster, Match match, String server);

    ExternalMessage getNonClonflictSubmitMessage(long matchId, int candidatePlace);

    ExternalMessage getConflictSubmitMessage(Collection<MatchPlayer> matchPlayers, MatchPlayer candidate, int candidatePlace);

    ExternalMessage getMatchSuccessfulFinishMessage(Match match);

    ExternalMessage getHelpMessage();

    ExternalMessage getAcceptSubmitRejectedDueToMatchFinishMessage(long matchId);
}
