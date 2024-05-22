package ru.trainithard.dunebot.configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.service.MatchExpirationService;
import ru.trainithard.dunebot.service.report.DailyRatingReportTask;
import ru.trainithard.dunebot.service.telegram.TelegramUpdateProcessor;

import java.time.*;

@Slf4j
@Profile(value = "!test")
@Component
@RequiredArgsConstructor
public class ScheduledTasksConfiguration {
    private final TaskScheduler dunebotTaskScheduler;
    private final TelegramUpdateProcessor updateProcessor;
    private final DailyRatingReportTask dailyRatingReportTask;
    private final MatchExpirationService expirationService;
    private final Clock clock;

    @PostConstruct
    void createScheduledTasks() {
        Instant now = Instant.now(clock);
        LocalDateTime localNow = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        LocalDateTime localMonthlyReportStartTime = localNow.plusDays(1).withHour(1).withMinute(0).withSecond(0).withNano(0);
        Instant monthlyReportStartTime = localMonthlyReportStartTime.toInstant(OffsetDateTime.now().getOffset());

        dunebotTaskScheduler.scheduleWithFixedDelay(updateProcessor::process, Duration.ofMillis(5));
        log.info("Scheduled TelegramUpdateProcessor#process for execution every 5 ms");

        //TODO replace with mothly reporting
        dunebotTaskScheduler.scheduleWithFixedDelay(dailyRatingReportTask, monthlyReportStartTime, Duration.ofDays(1));
        log.info("Scheduled MonthlyRatingReportTask#run for execution every 1 day, starting at {}", monthlyReportStartTime);
//        dunebotTaskScheduler.scheduleWithFixedDelay(monthlyRatingReportTask, monthlyReportStartTime, Duration.ofDays(1));
//        log.info("Scheduled MonthlyRatingReportTask#run for execution every 1 day, starting at {}", monthlyReportStartTime);

        Instant expirationServiceStartTime = getExpirationServiceStartTime(localNow).toInstant(OffsetDateTime.now().getOffset());
        dunebotTaskScheduler.scheduleAtFixedRate(expirationService::expireUnusedMatches, expirationServiceStartTime, Duration.ofHours(12));
        log.info("Scheduled MatchExpirationService#run for execution every 12 hours, starting at {}", expirationServiceStartTime);
    }

    private LocalDateTime getExpirationServiceStartTime(LocalDateTime localNow) {
        return localNow.getHour() < 12 ? localNow.withHour(12) : localNow.plusDays(1).
                withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
}
