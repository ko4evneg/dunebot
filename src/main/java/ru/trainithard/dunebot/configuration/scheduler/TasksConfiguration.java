package ru.trainithard.dunebot.configuration.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.trainithard.dunebot.model.scheduler.StateRunnable;
import ru.trainithard.dunebot.repository.DunebotTaskRepository;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.task.StartMatchTask;
import ru.trainithard.dunebot.service.task.SubmitAcceptTimeoutTask;
import ru.trainithard.dunebot.service.task.SubmitTimeoutNotificationTask;
import ru.trainithard.dunebot.service.task.SubmitTimeoutTask;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;

import java.time.Clock;
import java.util.function.BiFunction;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class TasksConfiguration {
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;
    private final MatchFinishingService matchFinishingService;
    private final ExternalMessageFactory messageFactory;
    private final DunebotTaskRepository taskRepository;
    private final AppSettingsService appSettingsService;
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

    @Bean
    public Function<Long, SubmitTimeoutTask> submitTimeoutTaskFactory() {
        return this::submitTimeoutTask;
    }

    @Bean
    @Scope("prototype")
    public SubmitTimeoutTask submitTimeoutTask(long matchId) {
        return new SubmitTimeoutTask(matchFinishingService, matchId);
    }

    @Bean
    public BiFunction<Runnable, DuneBotTaskId, StateRunnable> stateTaskFactory() {
        return this::stateTask;
    }

    @Bean
    @Scope("prototype")
    public StateRunnable stateTask(Runnable runnable, DuneBotTaskId taksId) {
        return new StateRunnable(taskRepository, taksId, runnable);
    }

    @Bean
    public Function<Long, SubmitTimeoutNotificationTask> submitTimeoutNotificationTaskFactory() {
        return this::submitTimeoutNotificationTask;
    }

    @Bean
    @Scope("prototype")
    public SubmitTimeoutNotificationTask submitTimeoutNotificationTask(long matchId) {
        return new SubmitTimeoutNotificationTask(matchRepository, messagingService, messageFactory, appSettingsService, matchId);
    }

    @Bean
    public Function<Long, SubmitAcceptTimeoutTask> submitAcceptTimeoutTaskFactory() {
        return this::submitAcceptTimeoutTask;
    }

    @Bean
    @Scope("prototype")
    public SubmitAcceptTimeoutTask submitAcceptTimeoutTask(long matchId) {
        return new SubmitAcceptTimeoutTask(matchFinishingService, matchId);
    }
}
