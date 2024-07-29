package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.trainithard.dunebot.model.PlayerRating;

import java.util.List;

public interface PlayerRatingRepository extends JpaRepository<PlayerRating, Long> {
    @Query(value = """
            with max_dates(pid, max_date) as
                (select pr1.player_id, max(rating_date)
                from player_ratings pr1
                group by pr1.player_id)
            select pr2.*
            from player_ratings pr2, max_dates md
            where pr2.player_id = md.pid and pr2.rating_date = md.max_date""", nativeQuery = true)
    List<PlayerRating> findLatestPlayerRatings();
}
