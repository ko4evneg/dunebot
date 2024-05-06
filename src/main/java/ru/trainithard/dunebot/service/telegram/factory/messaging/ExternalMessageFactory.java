package ru.trainithard.dunebot.service.telegram.factory.messaging;

import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;

import java.util.List;

public interface ExternalMessageFactory {
    ExternalMessage getGuestMessageDto(Player player);

    ExternalMessage getFinishReasonMessage(Match match, boolean isFailedByResubmitsLimit);

    ExternalMessage getStartMessage(Match match, List<String> regularPlayerMentions,
                                    List<String> guestPlayerMentions, List<String> blockedChatGuests);

    ExternalMessage getHelpMessage();
}
