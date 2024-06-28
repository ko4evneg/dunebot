package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.scheduler.DuneBotTask;
import ru.trainithard.dunebot.model.scheduler.TaskStatus;
import ru.trainithard.dunebot.repository.DunebotTaskRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.task.DuneScheduledTaskFactory;
import ru.trainithard.dunebot.service.task.DunebotRunnable;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartupServiceImpl implements StartupService {
    private static final String MATCH_FAIL_MESSAGE_TEMPLATE =
            "Бот был перезапущен, возможны задержки до двух минут в обработке команд или отправке сообщений бота.";

    private final DuneBotTaskScheduler taskScheduler;
    private final DunebotTaskRepository taskRepository;
    private final DuneScheduledTaskFactory taskFactory;
    private final AppSettingsService appSettingsService;
    private final MessagingService messagingService;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void startUp() {
        log.info("Starting bot...");
        taskRepository.findAllByStatusIn(List.of(TaskStatus.SCHEDULED, TaskStatus.RUN)).forEach(task -> {
            if (task.getDuneBotTaskId().getTaskType() == DuneTaskType.SHUTDOWN) {
                task.setStatus(TaskStatus.FINISHED);
                log.debug("Startup: shutdown task {} status set to FINISHED", task.getId());
            } else {
                DuneBotTaskId taskId = task.getDuneBotTaskId();
                DunebotRunnable taskRunnable = taskFactory.createInstance(taskId);
                taskScheduler.rescheduleSingleRunTask(taskRunnable, taskId, getRescheduleTime(task));
                log.debug("Startup: {} task {} rescheduled", task.getDuneBotTaskId().getTaskType(), task.getId());
            }
        });

        sendMessages();
        log.info("Bot started");
    }

    private void sendMessages() {
        String chatId = appSettingsService.getStringSetting(AppSettingKey.CHAT_ID);
        if (chatId != null) {
            ExternalMessage externalMessage = new ExternalMessage(MATCH_FAIL_MESSAGE_TEMPLATE);
            Integer classicTopicId = appSettingsService.getIntSetting(AppSettingKey.TOPIC_ID_CLASSIC);
            Integer uprisingTopicId = appSettingsService.getIntSetting(AppSettingKey.TOPIC_ID_UPRISING);

            if (Objects.equals(classicTopicId, uprisingTopicId) && classicTopicId != null) {
                MessageDto messageDto = new MessageDto(chatId, externalMessage, classicTopicId, null);
                messagingService.sendMessageAsync(messageDto);
                return;
            }

            if (classicTopicId != null) {
                MessageDto messageDto = new MessageDto(chatId, externalMessage, classicTopicId, null);
                messagingService.sendMessageAsync(messageDto);
            }
            if (uprisingTopicId != null) {
                MessageDto messageDto = new MessageDto(chatId, externalMessage, uprisingTopicId, null);
                messagingService.sendMessageAsync(messageDto);
            }
        }
    }

    private Instant getRescheduleTime(DuneBotTask task) {
        Instant startTime = task.getStartTime();
        Instant now = Instant.now(clock);
        return now.compareTo(startTime) < 0 ? task.getStartTime() : now.plus(1, ChronoUnit.MINUTES);
    }
}
