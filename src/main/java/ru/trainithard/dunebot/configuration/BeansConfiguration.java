package ru.trainithard.dunebot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;

@Configuration
public class BeansConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    TaskScheduler dunebotTaskScheduler(Clock clock) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setClock(clock);
        taskScheduler.setThreadNamePrefix("task-scheduler");
        return taskScheduler;
    }
}
