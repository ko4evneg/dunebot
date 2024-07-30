package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.PlayerRating;
import ru.trainithard.dunebot.repository.PlayerRatingRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.report.v2.RatingStatsComparator;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;
import ru.trainithard.dunebot.util.RatingCloseEntitiesUtil;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatsCommandProcessor extends CommandProcessor {
    private final PlayerRepository playerRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final AppSettingsService appSettingsService;
    private final ExternalMessageFactory messageFactory;
    private final RatingStatsComparator ratingStatsComparator;
    private final Clock clock;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: STATS started", logId());

        LocalDate now = LocalDate.now(clock);
        List<PlayerRating> playerRatings = playerRatingRepository
                .findAllBy(now.withDayOfMonth(1), now.with(TemporalAdjusters.lastDayOfMonth()));
        log.debug("{}: {} ratings found", logId(), playerRatings.size());
        playerRatings.sort(ratingStatsComparator);

        if (playerRatings.isEmpty()) {
            sendEmptyRatingsMessage(commandMessage);
            return;
        }

        Integer rowsCount = appSettingsService.getIntSetting(AppSettingKey.RATING_STAT_ROWS_COUNT);
        Player player = playerRepository.findByExternalId(commandMessage.getUserId()).orElseThrow();
        if (playerRatings.size() < rowsCount) {
            log.debug("{}: ratings count lesser then selection count, returning full rating", logId());
            sendPlayerStats(playerRatings, commandMessage, player.getId());
        } else {
            getPlayerIndex(playerRatings, player)
                    .ifPresentOrElse(index -> {
                        List<PlayerRating> closestEntitiesList =
                                RatingCloseEntitiesUtil.getClosestEntitiesList(playerRatings, index, rowsCount);
                        sendPlayerStats(closestEntitiesList, commandMessage, player.getId());
                    }, () ->
                            sendNoOwnedRatingsMessage(commandMessage));
        }

        log.debug("{}: STATS ended", logId());
    }

    private void sendEmptyRatingsMessage(CommandMessage commandMessage) {
        ExternalMessage noRatingsMessage = messageFactory.getNoRatingsMessage();
        MessageDto messageDto = new MessageDto(commandMessage, noRatingsMessage, null);
        messagingService.sendMessageAsync(messageDto);
    }

    private void sendNoOwnedRatingsMessage(CommandMessage commandMessage) {
        ExternalMessage noRatingsMessage = messageFactory.getNoOwnedRatingsMessage();
        MessageDto messageDto = new MessageDto(commandMessage, noRatingsMessage, null);
        messagingService.sendMessageAsync(messageDto);
    }

    private void sendPlayerStats(List<PlayerRating> closestEntitiesList, CommandMessage commandMessage, long requestingPlayerId) {
        ExternalMessage noRatingsMessage = messageFactory.getRatingStatsMessage(closestEntitiesList, requestingPlayerId);
        MessageDto messageDto = new MessageDto(commandMessage, noRatingsMessage, null);
        messagingService.sendMessageAsync(messageDto);
    }

    private Optional<Integer> getPlayerIndex(List<PlayerRating> playerRatings, Player player) {
        for (int i = 0; i < playerRatings.size(); i++) {
            if (playerRatings.get(i).getPlayer().equals(player)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    @Override
    public Command getCommand() {
        return Command.STATS;
    }
}
