package ru.trainithard.dunebot.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.report.MonthlyRatingCalculationService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyRatingReportTask implements Runnable {

    private final MonthlyRatingCalculationService service;
    private final Clock clock;

    @Value("${bot.pdf-directory}")
    private String pdfPath;

    @Override
    public void run() {
        LocalDate today = LocalDate.now(clock);
        YearMonth todayYearMonth = YearMonth.from(today);
        LocalDate firstMonthDay = todayYearMonth.atDay(1);

        if (today.equals(firstMonthDay)) {
            YearMonth previousMonth = todayYearMonth.minusMonths(1);

            try {
                service.storeAndSendMonthRating(previousMonth, ModType.CLASSIC, Path.of(pdfPath));
            } catch (Exception exception) {
                log.error("Failed to execute MonthlyRatingReportTask#run for CLASSIC mod", exception);
            }

            try {
                service.storeAndSendMonthRating(previousMonth, ModType.UPRISING_4, Path.of(pdfPath));
            } catch (Exception exception) {
                log.error("Failed to execute MonthlyRatingReportTask#run for UPRISING_4 mod", exception);
            }
            log.info("Successfully executed MonthlyRatingReportTask#run");
        }
    }
}
