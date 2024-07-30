package ru.trainithard.dunebot.service.telegram.factory.messaging;

import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.PlayerRating;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;

import java.util.Collection;
import java.util.List;

public interface ExternalMessageFactory {
    ExternalMessage getGuestMessageDto(Player player);

    ExternalMessage getPartialSubmittedMatchFinishMessage(Match match);

    ExternalMessage getStartMessage(Match match, List<String> regularPlayerMentions,
                                    List<String> guestPlayerMentions, List<String> blockedChatGuests);

    ExternalMessage getHostMessage(Player hoster, Match match, String server);

    ExternalMessage getMatchSuccessfulFinishMessage(Match match);

    ExternalMessage getPreSubmitTimeoutNotificationMessage(Match match, int timeout);

    ExternalMessage getLeadersSubmitMessage(long matchId);

    ExternalMessage getFinishedPlayersSubmitMessage(Collection<MatchPlayer> matchPlayers);

    ExternalMessage getFinishedLeadersSubmitMessage(Collection<MatchPlayer> matchPlayers);

    ExternalMessage getFinishedSubmitParticipantMessage(MatchPlayer matchPlayer, String submitter, int acceptSubmitTimeout);

    ExternalMessage getNoRatingsMessage();

    ExternalMessage getNoOwnedRatingsMessage();

    ExternalMessage getRatingStatsMessage(List<PlayerRating> playerRatings, long requestingPlayerId);

    ExternalMessage getHelpMessage();

    ExternalMessage getPlayersSubmitMessage(long matchId);

    ExternalMessage getFailByResubmitLimitExceededMessage(long matchId);

    ExternalMessage getResubmitMessage();
}
