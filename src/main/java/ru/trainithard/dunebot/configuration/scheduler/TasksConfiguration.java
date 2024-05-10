package ru.trainithard.dunebot.configuration.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.telegram.command.task.StartMatchTask;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;

import java.time.Clock;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class TasksConfiguration {
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;
    private final ExternalMessageFactory messageFactory;
    private final Clock clock;

    @Bean
    public Function<Long, StartMatchTask> startMatchTaskFactory() {
        return this::startMatchTask;
    }

    @Bean
    @Scope("prototype")
    public StartMatchTask startMatchTask(long matchId) {
        return new StartMatchTask(matchRepository, matchPlayerRepository, messagingService, messageFactory, clock, matchId);
    }
}
