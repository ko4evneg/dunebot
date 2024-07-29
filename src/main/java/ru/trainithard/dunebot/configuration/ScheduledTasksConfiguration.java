package ru.trainithard.dunebot.configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.service.MatchExpirationService;
import ru.trainithard.dunebot.service.report.WeeklyRatingReportTask;
import ru.trainithard.dunebot.service.report.v2.RatingService;
import ru.trainithard.dunebot.service.telegram.TelegramUpdateProcessor;
import ru.trainithard.dunebot.util.TimeUtil;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Slf4j
@Profile(value = "!test")
@Component
@RequiredArgsConstructor
public class ScheduledTasksConfiguration {
    private static final Duration DAY_INTERVAL = Duration.ofDays(1);
    private final TaskScheduler taskScheduler;
    private final TelegramUpdateProcessor updateProcessor;
    private final WeeklyRatingReportTask weeklyRatingReportTask;
    private final MatchExpirationService expirationService;
    private final RatingService ratingService;
    private final Clock clock;

    @PostConstruct
    void createScheduledTasks() {
        Instant now = Instant.now(clock);

        taskScheduler.scheduleWithFixedDelay(updateProcessor::process, Duration.ofMillis(5));
        log.info("Scheduled TelegramUpdateProcessor#process for execution every 5 ms");

        //TODO replace with mothly reporting
        Instant ratingReportStartTime = TimeUtil.getRounded(now, 23, 0);
        taskScheduler.scheduleWithFixedDelay(weeklyRatingReportTask, ratingReportStartTime, DAY_INTERVAL);
        log.info("Scheduled MonthlyRatingReportTask#run for execution every 1 day, starting at {}", ratingReportStartTime);
//        dunebotTaskScheduler.scheduleWithFixedDelay(monthlyRatingReportTask, ratingReportStartTime, Duration.ofDays(1));
//        log.info("Scheduled MonthlyRatingReportTask#run for execution every 1 day, starting at {}", ratingReportStartTime);

        Instant ratingUpdateStartTime = getClosestExecutionTime(now, 3);
        taskScheduler.scheduleWithFixedDelay(ratingService::buildFullRating, ratingUpdateStartTime, DAY_INTERVAL);

        Instant expirationServiceStartTime = getClosestExecutionTime(now, 12);
        taskScheduler.scheduleAtFixedRate(expirationService::expireUnusedMatches, expirationServiceStartTime, Duration.ofHours(12));
        log.info("Scheduled MatchExpirationService#run for execution every 12 hours, starting at {}", expirationServiceStartTime);
    }

    private Instant getClosestExecutionTime(Instant now, int desiredHour) {
        return now.atZone(ZoneId.systemDefault()).getHour() < desiredHour
                ? TimeUtil.getRounded(now, desiredHour, 0) :
                TimeUtil.getRounded(now.plus(1, ChronoUnit.DAYS), desiredHour, 0);
    }
}
