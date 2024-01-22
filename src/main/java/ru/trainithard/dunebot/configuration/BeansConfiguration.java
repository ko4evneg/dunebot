package ru.trainithard.dunebot.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class BeansConfiguration {
    private static final int THREAD_POOL_SIZE = 4;

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    TaskScheduler dunebotTaskScheduler(Clock clock) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(THREAD_POOL_SIZE);
        taskScheduler.setClock(clock);
        taskScheduler.setThreadNamePrefix("dunebot-scheduler");
        return taskScheduler;
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .setReadTimeout(Duration.ofMillis(3000))
                .setConnectTimeout(Duration.ofMillis(5000))
                .build();
    }
}
