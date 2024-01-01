package ru.trainithard.dunebot.configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.service.telegram.TelegramUpdateProcessor;

import java.time.Duration;

@Profile(value = "!test")
@Component
@RequiredArgsConstructor
public class ScheduledTasksConfiguration {
    private final TaskScheduler taskScheduler;
    private final TelegramUpdateProcessor updateProcessor;

    @PostConstruct
    void createScheduledTasks() {
        taskScheduler.scheduleWithFixedDelay(updateProcessor::process, Duration.ofMillis(5));
    }
}
