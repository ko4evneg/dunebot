package ru.trainithard.dunebot.service.report;

import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.FileMessageDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlyRatingCalculationServiceImpl implements MonthlyRatingCalculationService {
    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;
    private final SettingsService settingsService;

    @Override
    public void storeAndSendMonthRating(YearMonth month, ModType modType, Path dir) throws DocumentException, IOException {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        List<MatchPlayer> monthMatchPlayers = matchPlayerRepository.findByMatchDates(from, to, MatchState.FINISHED, modType);
        int matchesThreshold = settingsService.getIntSetting(SettingKey.MONTHLY_MATCHES_THRESHOLD);
        MonthlyRating monthlyRating = new MonthlyRating(monthMatchPlayers, modType, matchesThreshold);

        MonthlyRatingPdf monthlyRatingPdf = new MonthlyRatingPdf(getReportName(month), convertToReportRows(monthlyRating));

        Files.createDirectories(dir);
        byte[] pdfFile = monthlyRatingPdf.getPdfBytes();
        Files.write(dir.resolve(getPdfFileName(month)), pdfFile);

        String ratingName = "Рейтинг за " + getDateString(month);
        String chatId = settingsService.getStringSetting(SettingKey.CHAT_ID);
        FileMessageDto fileMessageDto =
                new FileMessageDto(chatId, new ExternalMessage(ratingName).append(":"), getTopicId(modType), pdfFile, ratingName);
        messagingService.sendFileAsync(fileMessageDto);
    }

    private List<List<String>> convertToReportRows(MonthlyRating monthlyRating) {
        int ratingPlace = 1;
        List<List<String>> rows = new ArrayList<>();
        for (MonthlyRating.PlayerMonthlyRating playerRating : monthlyRating.getPlayerRatings()) {
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

    private int getTopicId(ModType modType) {
        return switch (modType) {
            case CLASSIC -> settingsService.getIntSetting(SettingKey.TOPIC_ID_CLASSIC);
            case UPRISING_4, UPRISING_6 -> settingsService.getIntSetting(SettingKey.TOPIC_ID_UPRISING);
        };
    }

    private String getReportName(YearMonth month) {
        return "РЕЙТИНГ " + getDateString(month);
    }

    private String getDateString(YearMonth month) {
        return month.getMonth().getValue() + "." + month.getYear();
    }

    private String getPdfFileName(YearMonth month) {
        return month.getYear() + "_" + month.getMonth() + ".pdf";
    }

}
