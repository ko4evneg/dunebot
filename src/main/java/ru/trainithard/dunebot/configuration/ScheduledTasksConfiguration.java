package ru.trainithard.dunebot.configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.service.telegram.TelegramUpdateProcessor;

import java.time.*;

@Profile(value = "!test")
@Component
@RequiredArgsConstructor
public class ScheduledTasksConfiguration {
    private final TaskScheduler dunebotTaskScheduler;
    private final TelegramUpdateProcessor updateProcessor;
    private final MonthlyRatingReportTask monthlyRatingReportTask;
    private final Clock clock;

    @PostConstruct
    void createScheduledTasks() {
        Instant now = Instant.now(clock);
        LocalDateTime localNow = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        LocalDateTime localMonthlyReportStartTime = localNow.plusDays(1).withHour(1).withMinute(0).withSecond(0).withNano(0);
        Instant monthlyReportStartTime = localMonthlyReportStartTime.toInstant(OffsetDateTime.now().getOffset());

        dunebotTaskScheduler.scheduleWithFixedDelay(updateProcessor::process, Duration.ofMillis(5));
        dunebotTaskScheduler.scheduleWithFixedDelay(monthlyRatingReportTask, monthlyReportStartTime, Duration.ofDays(1));
    }
}
