package ru.trainithard.dunebot.service.report;

import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.FileMessageDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

//TODO delete after tests
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyRatingReportTask implements Runnable {
    private final RatingReportPdfService ratingReportPdfService;
    private final AppSettingsService appSettingsService;
    private final MessagingService messagingService;
    private final Clock clock;

    @Value("${bot.pdf-directory}")
    private String pdfPath;

    @Override
    public void run() {
        log.info("Start execution DailyRatingReportTask#run...");

        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(1);
        LocalDate to = from;

        try {
            reportRating(from, to, ModType.CLASSIC, YearMonth.of(2000, 1));
        } catch (Exception exception) {
            log.error("Failed to execute DailyRatingReportTask#run for CLASSIC mod", exception);
        }

        try {
            reportLeaders(from, to, ModType.CLASSIC, YearMonth.of(2000, 1));
        } catch (Exception exception) {
            log.error("Failed to execute DailyRatingReportTask#run for CLASSIC leaders", exception);
        }

        try {
            reportRating(from, to, ModType.UPRISING_4, YearMonth.of(2000, 1));
        } catch (Exception exception) {
            log.error("Failed to execute DailyRatingReportTask#run for UPRISING_4 mod", exception);
        }
        log.info("Successfully executed MonthlyRatingReportTask#run");

    }

    private void reportRating(LocalDate from, LocalDate to, ModType classic, YearMonth previousMonth)
            throws DocumentException, IOException {
        RatingReportPdf monthlyRatingPdf = ratingReportPdfService.createPlayersReport(from, to, classic, getReportName(previousMonth));
        byte[] ratingBytes = monthlyRatingPdf.getPdfBytes();
        saveRating(ratingBytes, getPdfFileName(previousMonth, classic));
        sendTopicNotifications(previousMonth, ratingBytes, classic);
    }

    private void reportLeaders(LocalDate from, LocalDate to, ModType classic, YearMonth previousMonth)
            throws DocumentException, IOException {
        RatingReportPdf monthlyRatingPdf = ratingReportPdfService.createLeadersReport(from, to, classic, getReportName(previousMonth));
        byte[] ratingBytes = monthlyRatingPdf.getPdfBytes();
        saveRating(ratingBytes, getPdfFileName(previousMonth, classic));
        sendTopicNotifications(previousMonth, ratingBytes, classic);
    }

    private void saveRating(byte[] ratingPdfBytes, String fileName) throws IOException {
        Path savingPath = Path.of(pdfPath);
        Files.createDirectories(savingPath);
        Files.write(savingPath.resolve(fileName), ratingPdfBytes);
    }

    private String getReportName(YearMonth month) {
        return "РЕЙТИНГ " + getDateString(month);
    }

    private String getDateString(YearMonth month) {
        return month.getMonth().getValue() + "." + month.getYear();
    }

    private String getPdfFileName(YearMonth month, ModType modType) {
        return String.format("%s_%s_%s.pdf", modType.getAlias(), month.getYear(), month.getMonth());
    }

    private void sendTopicNotifications(YearMonth month, byte[] pdfFile, ModType modType) {
        String ratingName = "Рейтинг за " + getDateString(month);
        String chatId = appSettingsService.getStringSetting(AppSettingKey.CHAT_ID);
        FileMessageDto fileMessageDto =
                new FileMessageDto(chatId, new ExternalMessage(ratingName).append(":"), getTopicId(modType), pdfFile, ratingName + ".pdf");
        messagingService.sendFileAsync(fileMessageDto);
    }

    private int getTopicId(ModType modType) {
        return switch (modType) {
            case CLASSIC -> appSettingsService.getIntSetting(AppSettingKey.TOPIC_ID_CLASSIC);
            case UPRISING_4, UPRISING_6 -> appSettingsService.getIntSetting(AppSettingKey.TOPIC_ID_UPRISING);
        };
    }
}
