package ru.trainithard.dunebot.service.report;

import java.util.HashMap;
import java.util.Map;

class RatingCalculator {
    private static final Map<Integer, Double> efficiencyRateByPlaceNames = new HashMap<>();

    static {
        efficiencyRateByPlaceNames.put(1, 1.0);
        efficiencyRateByPlaceNames.put(2, 0.6);
        efficiencyRateByPlaceNames.put(3, 0.4);
        efficiencyRateByPlaceNames.put(4, 0.1);
    }

    private RatingCalculator() {
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
