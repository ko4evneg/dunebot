package ru.trainithard.dunebot.service.report.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MetaDataKey;
import ru.trainithard.dunebot.model.PlayerRating;
import ru.trainithard.dunebot.repository.PlayerRatingRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class PlayerRatingUpdateService extends RatingUpdateService<PlayerRating> {
    private final PlayerRatingRepository playerRatingRepository;

    @Override
    Function<MatchPlayer, Long> getEntityIdSupplier() {
        return matchPlayer -> matchPlayer.getPlayer().getId();
    }

    @Override
    PlayerRating createNewRating(MatchPlayer matchPlayer) {
        LocalDate finishDate = matchPlayer.getMatch().getFinishDate();
        return new PlayerRating(matchPlayer.getPlayer(), Objects.requireNonNull(finishDate));
    }

    @Override
    void saveRatings(Collection<PlayerRating> ratings) {
        playerRatingRepository.saveAll(ratings);
    }

    @Override
    MetaDataKey getMetaDataKey() {
        return MetaDataKey.PLAYER_RATING_DATE;
    }
}
