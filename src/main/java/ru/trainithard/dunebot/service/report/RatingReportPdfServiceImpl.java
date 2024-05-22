package ru.trainithard.dunebot.service.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.service.AppSettingsService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingReportPdfServiceImpl implements RatingReportPdfService {
    private final MatchPlayerRepository matchPlayerRepository;
    private final AppSettingsService appSettingsService;

    @Override
    public RatingReportPdf createRating(LocalDate from, LocalDate to, ModType modType, String reportName) {
        List<MatchPlayer> monthMatchPlayers = matchPlayerRepository.findByMatchDates(from, to, MatchState.FINISHED, modType);
        int matchesThreshold = appSettingsService.getIntSetting(AppSettingKey.MONTHLY_MATCHES_THRESHOLD);
        PlayersRatingReport monthlyRating = new PlayersRatingReport(monthMatchPlayers, modType, matchesThreshold);

        return new RatingReportPdf(reportName, convertToReportRows(monthlyRating));
    }

    private List<List<String>> convertToReportRows(PlayersRatingReport monthlyRating) {
        int ratingPlace = 1;
        List<List<String>> rows = new ArrayList<>();
        for (RatingReport.EntityRating playerEntityRating : monthlyRating.getPlayerEntityRatings()) {
            List<String> columnValues = new ArrayList<>();
            columnValues.add(Integer.toString(ratingPlace));
            columnValues.add(playerEntityRating.getName());
            columnValues.add(Long.toString(playerEntityRating.getMatchesCount()));
            playerEntityRating.getOrderedPlaceCountByPlaceNames()
                    .forEach((place, count) -> columnValues.add(count.toString()));
            columnValues.add(getStrippedZeroesString(playerEntityRating.getEfficiency()));
            columnValues.add(getStrippedZeroesString(playerEntityRating.getWinRate()) + "%");

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
