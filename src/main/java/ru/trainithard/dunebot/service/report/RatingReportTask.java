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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

//TODO delete after tests
@Slf4j
@Component
@RequiredArgsConstructor
public class RatingReportTask implements Runnable {
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

        if (isReportDay(today)) {
            LocalDate from = today.withDayOfMonth(1);

            try {
                reportRating(from, today, ModType.CLASSIC, YearMonth.of(2000, 1));
            } catch (Exception exception) {
                log.error("Failed to execute DailyRatingReportTask#run for CLASSIC mod", exception);
            }

            try {
                reportLeaders(from, today, ModType.CLASSIC, YearMonth.of(2000, 1));
            } catch (Exception exception) {
                log.error("Failed to execute DailyRatingReportTask#run for CLASSIC leaders", exception);
            }

//        try {
//            reportRating(from, to, ModType.UPRISING_4, YearMonth.of(2000, 1));
//        } catch (Exception exception) {
//            log.error("Failed to execute DailyRatingReportTask#run for UPRISING_4 mod", exception);
//        }
        }
        log.info("Successfully executed MonthlyRatingReportTask#run");
    }

    private boolean isReportDay(LocalDate today) {
        int lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        int lastWeekOfMonthStartDay = lastDayOfMonth - 6;
        return today.getDayOfWeek() == DayOfWeek.SUNDAY || today.getDayOfMonth() == lastDayOfMonth
               || today.getDayOfMonth() >= lastWeekOfMonthStartDay && today.getDayOfWeek() == DayOfWeek.WEDNESDAY;
    }

    private void reportRating(LocalDate from, LocalDate to, ModType classic, YearMonth previousMonth)
            throws DocumentException, IOException {
        RatingReportPdf monthlyRatingPdf = ratingReportPdfService.createPlayersReport(from, to, classic, getReportName(from, to, true));
        byte[] ratingBytes = monthlyRatingPdf.getPdfBytes();
        saveRating(ratingBytes, getPdfFileName(previousMonth, classic));
        sendTopicNotifications(getReportName(from, to, true), ratingBytes, classic);
    }

    private void reportLeaders(LocalDate from, LocalDate to, ModType classic, YearMonth previousMonth)
            throws DocumentException, IOException {
        RatingReportPdf monthlyRatingPdf = ratingReportPdfService.createLeadersReport(from, to, classic, getReportName(from, to, false));
        byte[] ratingBytes = monthlyRatingPdf.getPdfBytes();
        saveRating(ratingBytes, getPdfFileName(previousMonth, classic));
        sendTopicNotifications(getReportName(from, to, false), ratingBytes, classic);
    }

    private void saveRating(byte[] ratingPdfBytes, String fileName) throws IOException {
        Path savingPath = Path.of(pdfPath);
        Files.createDirectories(savingPath);
        Files.write(savingPath.resolve(fileName), ratingPdfBytes);
    }

    private String getReportName(LocalDate from, LocalDate to, boolean isPlayers) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        String fromText = from.format(dateTimeFormatter);
        String toText = to.format(dateTimeFormatter);
        String ratingType = isPlayers ? "ИГРОКОВ" : "ЛИДЕРОВ";
        return String.format("РЕЙТИНГ %s %s - %s", ratingType, fromText, toText);
    }

    private String getPdfFileName(YearMonth month, ModType modType) {
        return String.format("%s_%s_%s.pdf", modType.getAlias(), month.getYear(), month.getMonth());
    }

    private void sendTopicNotifications(String ratingName, byte[] pdfFile, ModType modType) {
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
