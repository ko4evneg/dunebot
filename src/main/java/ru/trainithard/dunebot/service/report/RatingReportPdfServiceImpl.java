package ru.trainithard.dunebot.service.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.service.SettingsService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingReportPdfServiceImpl implements RatingReportPdfService {
    private final MatchPlayerRepository matchPlayerRepository;
    private final SettingsService settingsService;

    @Override
    public RatingReportPdf createRating(LocalDate from, LocalDate to, ModType modType, String reportName) {
        List<MatchPlayer> monthMatchPlayers = matchPlayerRepository.findByMatchDates(from, to, MatchState.FINISHED, modType);
        int matchesThreshold = settingsService.getIntSetting(SettingKey.MONTHLY_MATCHES_THRESHOLD);
        RatingReport monthlyRating = new RatingReport(monthMatchPlayers, modType, matchesThreshold);

        return new RatingReportPdf(reportName, convertToReportRows(monthlyRating));
    }

    private List<List<String>> convertToReportRows(RatingReport monthlyRating) {
        int ratingPlace = 1;
        List<List<String>> rows = new ArrayList<>();
        for (RatingReport.PlayerMonthlyRating playerRating : monthlyRating.getPlayerRatings()) {
            List<String> columnValues = new ArrayList<>();
            columnValues.add(Integer.toString(ratingPlace));
            columnValues.add(playerRating.getPlayerFriendlyName());
            columnValues.add(Long.toString(playerRating.getMatchesCount()));
            playerRating.getOrderedPlaceCountByPlaceNames()
                    .forEach((place, count) -> columnValues.add(count.toString()));
            columnValues.add(getStrippedZeroesString(playerRating.getEfficiency()));
            columnValues.add(getStrippedZeroesString(playerRating.getWinRate()) + "%");

            rows.add(columnValues);
            ratingPlace++;
        }
        return rows;
    }

    private String getStrippedZeroesString(double number) {
        String numberString = Double.toString(number);
        return numberString.replace(".00", "").replace(".0", "");
    }
}
