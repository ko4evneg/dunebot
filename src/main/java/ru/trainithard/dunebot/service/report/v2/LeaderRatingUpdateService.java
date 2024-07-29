package ru.trainithard.dunebot.service.report.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.LeaderRating;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MetaDataKey;
import ru.trainithard.dunebot.repository.LeaderRatingRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class LeaderRatingUpdateService extends RatingUpdateService<LeaderRating> {
    private final LeaderRatingRepository leaderRatingRepository;

    @Override
    Function<MatchPlayer, Long> getEntityIdSupplier() {
        return matchPlayer -> matchPlayer.getLeader().getId();
    }

    @Override
    LeaderRating createNewRating(MatchPlayer matchPlayer) {
        LocalDate finishDate = matchPlayer.getMatch().getFinishDate();
        return new LeaderRating(matchPlayer.getLeader(), Objects.requireNonNull(finishDate));
    }

    @Override
    void saveRatings(Collection<LeaderRating> ratings) {
        leaderRatingRepository.saveAll(ratings);
    }

    @Override
    MetaDataKey getMetaDataKey() {
        return MetaDataKey.LEADER_RATING_DATE;
    }
}
