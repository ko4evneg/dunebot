package ru.trainithard.dunebot.util;

import ru.trainithard.dunebot.exception.DuneBotException;
import ru.trainithard.dunebot.model.AbstractRating;

import java.util.ArrayList;
import java.util.List;

public class RatingCloseEntitiesUtil {
    private RatingCloseEntitiesUtil() {
    }

    public static <T extends AbstractRating> List<T> getClosestEntitiesList(List<T> allRatings, int entityIndex, int selectionSize) {
        if (selectionSize % 2 == 0) {
            throw new DuneBotException("Even selection size prohibited");
        }
        int sideSize = selectionSize / 2;
        List<T> selectedRatings = new ArrayList<>();
        if (entityIndex <= sideSize) {
            selectedRatings.addAll(allRatings.subList(0, selectionSize));
        } else if (entityIndex >= allRatings.size() - sideSize - 1) {
            selectedRatings.addAll(allRatings.subList(allRatings.size() - 1 - selectionSize, allRatings.size() - 1));
        } else {
            selectedRatings.addAll(allRatings.subList(entityIndex - sideSize, entityIndex + sideSize + 1));
        }

        return selectedRatings;
    }
}
