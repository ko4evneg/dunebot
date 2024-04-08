package ru.trainithard.dunebot.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.FileMessageDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyRatingReportTask implements Runnable {
    private final RatingReportPdfService ratingReportPdfService;
    private final SettingsService settingsService;
    private final MessagingService messagingService;
    private final Clock clock;

    @Value("${bot.pdf-directory}")
    private String pdfPath;

    @Override
    public void run() {
        log.info("Start execution MonthlyRatingReportTask#run...");

        LocalDate today = LocalDate.now(clock);
        YearMonth todayYearMonth = YearMonth.from(today);
        LocalDate firstMonthDay = todayYearMonth.atDay(1);

        if (today.equals(firstMonthDay)) {
            YearMonth previousMonth = todayYearMonth.minusMonths(1);
            LocalDate from = previousMonth.atDay(1);
            LocalDate to = previousMonth.atEndOfMonth();
            try {
                RatingReportPdf monthlyRatingPdf = ratingReportPdfService.createRating(from, to, ModType.CLASSIC, getReportName(previousMonth));
                byte[] ratingBytes = monthlyRatingPdf.getPdfBytes();
                saveRating(ratingBytes, previousMonth);
                sendTopicNotifications(previousMonth, ratingBytes, ModType.CLASSIC);
            } catch (Exception exception) {
                log.error("Failed to execute MonthlyRatingReportTask#run for CLASSIC mod", exception);
            }

            try {
                RatingReportPdf monthlyRatingPdf = ratingReportPdfService.createRating(from, to, ModType.UPRISING_4, getReportName(previousMonth));
                byte[] ratingBytes = monthlyRatingPdf.getPdfBytes();
                saveRating(ratingBytes, previousMonth);
                sendTopicNotifications(previousMonth, ratingBytes, ModType.UPRISING_4);
            } catch (Exception exception) {
                log.error("Failed to execute MonthlyRatingReportTask#run for UPRISING_4 mod", exception);
            }
            log.info("Successfully executed MonthlyRatingReportTask#run");
        }
    }

    private void saveRating(byte[] ratingPdfBytes, YearMonth month) throws IOException {
        Path savingPath = Path.of(pdfPath);
        Files.createDirectories(savingPath);
        Files.write(savingPath.resolve(getPdfFileName(month)), ratingPdfBytes);
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

    private void sendTopicNotifications(YearMonth month, byte[] pdfFile, ModType modType) {
        String ratingName = "Рейтинг за " + getDateString(month);
        String chatId = settingsService.getStringSetting(SettingKey.CHAT_ID);
        FileMessageDto fileMessageDto =
                new FileMessageDto(chatId, new ExternalMessage(ratingName).append(":"), getTopicId(modType), pdfFile, ratingName);
        messagingService.sendFileAsync(fileMessageDto);
    }

    private int getTopicId(ModType modType) {
        return switch (modType) {
            case CLASSIC -> settingsService.getIntSetting(SettingKey.TOPIC_ID_CLASSIC);
            case UPRISING_4, UPRISING_6 -> settingsService.getIntSetting(SettingKey.TOPIC_ID_UPRISING);
        };
    }
}
