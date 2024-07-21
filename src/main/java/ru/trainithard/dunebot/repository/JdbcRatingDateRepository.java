package ru.trainithard.dunebot.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.exception.DuneBotException;
import ru.trainithard.dunebot.model.AbstractRating;
import ru.trainithard.dunebot.model.LeaderRating;
import ru.trainithard.dunebot.model.PlayerRating;
import ru.trainithard.dunebot.model.RatingDate;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JdbcRatingDateRepository implements RatingDateRepository {
    private static final RowMapper<RatingDate> RATING_DATE_RAW_MAPPER = (rs, rowNum) ->
            new RatingDate(rs.getLong(1), rs.getDate(2).toLocalDate());

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<RatingDate> findLatestRatings(Class<? extends AbstractRating> aClass) {
        String[] names = getNames(aClass);
        String query = String.format("""
                with max_dates(%1$s, max_date) AS
                    (SELECT %1$s, max(rating_date)
                    from %2$s
                    group by %1$s)
                select r.%1s, r.rating_date
                from %2$s r, max_dates md
                where r.%1$s = md.%1$s and r.rating_date = md.max_date""", names[0], names[1]);
        return jdbcTemplate.query(query, RATING_DATE_RAW_MAPPER);
    }

    private String[] getNames(Class<? extends AbstractRating> aClass) {
        String[] names = new String[2];
        if (aClass == PlayerRating.class) {
            names[0] = "PLAYER_ID";
            names[1] = "PLAYER_RATINGS";
        } else if (aClass == LeaderRating.class) {
            names[0] = "LEADER_ID";
            names[1] = "LEADER_RATINGS";
        } else {
            throw new DuneBotException("Query for non-existent entity");
        }
        return names;
    }
}
