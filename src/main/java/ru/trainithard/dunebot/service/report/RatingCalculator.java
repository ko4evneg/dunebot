package ru.trainithard.dunebot.service.report;

import ru.trainithard.dunebot.model.AbstractRating;

import java.util.HashMap;
import java.util.Map;

public class RatingCalculator {
    private static final Map<Integer, Double> efficiencyRateByPlaceNames = new HashMap<>();

    static {
        efficiencyRateByPlaceNames.put(1, 1.0);
        efficiencyRateByPlaceNames.put(2, 0.6);
        efficiencyRateByPlaceNames.put(3, 0.4);
        efficiencyRateByPlaceNames.put(4, 0.1);
    }

    private RatingCalculator() {
    }

    public static double calculateEfficiency(AbstractRating rating) {
        Map<Integer, Integer> placeCountByPlaceNames = new HashMap<>();
        placeCountByPlaceNames.put(1, rating.getFirstPlaceCount());
        placeCountByPlaceNames.put(2, rating.getSecondPlaceCount());
        placeCountByPlaceNames.put(3, rating.getThirdPlaceCount());
        placeCountByPlaceNames.put(4, rating.getFourthPlaceCount());
        return calculateEfficiencyNew(placeCountByPlaceNames, rating.getMatchesCount());
    }

    private static double calculateEfficiencyNew(Map<Integer, Integer> placeCountByPlaceNames, long allPlacesCount) {
        double efficiencesSum = 0;
        for (Map.Entry<Integer, Integer> entry : placeCountByPlaceNames.entrySet()) {
            int place = entry.getKey();
            double placeEfficiency = efficiencyRateByPlaceNames.getOrDefault(place, 1.0);
            double placesCount = placeCountByPlaceNames.getOrDefault(place, 0);
            efficiencesSum += placesCount / allPlacesCount * placeEfficiency;
        }
        return ((int) (efficiencesSum * 100)) / 100.0;
    }

    static double calculateEfficiency(Map<Integer, Long> placeCountByPlaceNames, long allPlacesCount) {
        double efficiencesSum = 0;
        for (Map.Entry<Integer, Long> entry : placeCountByPlaceNames.entrySet()) {
            int place = entry.getKey();
            double placeEfficiency = efficiencyRateByPlaceNames.getOrDefault(place, 1.0);
            double placesCount = placeCountByPlaceNames.getOrDefault(place, 0L);
            efficiencesSum += placesCount / allPlacesCount * placeEfficiency;
        }
        return ((int) (efficiencesSum * 100)) / 100.0;
    }

    static double calculateWinRate(double firstPlacesCount, long playerMatchesCount) {
        double winRate = firstPlacesCount / playerMatchesCount * 100;
        return (int) (winRate * 100) / 100.0;
    }
}
