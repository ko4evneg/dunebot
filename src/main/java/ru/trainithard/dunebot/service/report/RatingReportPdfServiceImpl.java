package ru.trainithard.dunebot.service.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.util.DoubleToStringUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingReportPdfServiceImpl implements RatingReportPdfService {
    private final MatchPlayerRepository matchPlayerRepository;
    private final AppSettingsService appSettingsService;

    @Override
    public RatingReportPdf createPlayersReport(LocalDate from, LocalDate to, ModType modType, String reportName) {
        List<MatchPlayer> monthMatchPlayers = matchPlayerRepository.findByMatchDates(from, to, MatchState.FINISHED, modType);
        int matchesThreshold = appSettingsService.getIntSetting(AppSettingKey.MONTHLY_MATCHES_THRESHOLD);
        PlayersRatingReport playersRatingReport = new PlayersRatingReport(monthMatchPlayers, modType, matchesThreshold);

        return new RatingReportPdf(reportName, convertToReportRows(playersRatingReport));
    }

    @Override
    public RatingReportPdf createLeadersReport(LocalDate from, LocalDate to, ModType modType, String reportName) {
        List<MatchPlayer> monthMatchPlayers = matchPlayerRepository.findByMatchDates(from, to, MatchState.FINISHED, modType);
        LeadersRatingReport leadersRatingReport = new LeadersRatingReport(monthMatchPlayers, modType, 1);

        return new RatingReportPdf(reportName, convertToReportRows(leadersRatingReport));
    }

    private List<List<String>> convertToReportRows(RatingReport monthlyRating) {
        int ratingPlace = 1;
        List<List<String>> rows = new ArrayList<>();
        for (RatingReport.EntityRating playerEntityRating : monthlyRating.getPlayerEntityRatings()) {
            List<String> columnValues = new ArrayList<>();
            columnValues.add(Integer.toString(ratingPlace));
            columnValues.add(playerEntityRating.getName());
            columnValues.add(Long.toString(playerEntityRating.getMatchesCount()));
            playerEntityRating.getOrderedPlaceCountByPlaceNames()
                    .forEach((place, count) -> columnValues.add(count.toString()));
            columnValues.add(DoubleToStringUtil.getStrippedZeroesString(playerEntityRating.getEfficiency()));
            columnValues.add(DoubleToStringUtil.getStrippedZeroesString(playerEntityRating.getWinRate()) + "%");

            rows.add(columnValues);
            ratingPlace++;
        }
        return rows;
    }
}
