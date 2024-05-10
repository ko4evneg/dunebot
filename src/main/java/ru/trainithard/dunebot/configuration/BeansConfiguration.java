package ru.trainithard.dunebot.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class BeansConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public DuneBotTaskScheduler dunebotTaskScheduler(Clock clock) {
        return new DuneBotTaskScheduler(clock);
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .setReadTimeout(Duration.ofMillis(3000))
                .setConnectTimeout(Duration.ofMillis(5000))
                .build();
    }
}
