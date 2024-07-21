package ru.trainithard.dunebot.repository;

import ru.trainithard.dunebot.model.AbstractRating;
import ru.trainithard.dunebot.model.RatingDate;

import java.util.List;

public interface RatingDateRepository {
    List<RatingDate> findLatestRatings(Class<? extends AbstractRating> aClass);
}
