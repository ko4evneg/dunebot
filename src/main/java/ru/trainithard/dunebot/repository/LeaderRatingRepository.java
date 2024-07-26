package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.trainithard.dunebot.model.LeaderRating;

import java.util.List;

public interface LeaderRatingRepository extends JpaRepository<LeaderRating, Long> {
    @Query(value = """
            with max_dates(lid, max_date) as
                (select lr1.leader_id, max(rating_date)
                from leader_ratings lr1
                group by lr1.leader_id)
            select lr2.*
            from leader_ratings lr2, max_dates md
            where lr2.leader_id = md.lid and lr2.rating_date = md.max_date""", nativeQuery = true)
    List<LeaderRating> findLatestLeaderRatings();
}
