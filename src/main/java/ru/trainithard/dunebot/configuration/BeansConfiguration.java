package ru.trainithard.dunebot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class BeansConfiguration {
    @Bean
    Map<Command, CommandProcessor> commandProcessors(List<CommandProcessor> processors) {
        return processors.stream().collect(Collectors.toMap(CommandProcessor::getCommand, Function.identity()));
    }

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
