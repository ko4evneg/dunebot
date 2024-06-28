package ru.trainithard.dunebot.service.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShutdownTask implements DunebotRunnable {
    private final ConfigurableApplicationContext applicationContext;
    private final MessagingService messagingService;
    private final AppSettingsService appSettingsService;

    @Override
    public void run() {
        sendTopicsMessages("⚠️ Бот перезагружается.");
        log.info("Bot shutdown...");
        SpringApplication.exit(applicationContext, () -> 0);
    }

    private void sendTopicsMessages(String message) {
        String chatId = appSettingsService.getStringSetting(AppSettingKey.CHAT_ID);
        int up4Topic = appSettingsService.getIntSetting(AppSettingKey.TOPIC_ID_UPRISING);
        int duneTopic = appSettingsService.getIntSetting(AppSettingKey.TOPIC_ID_CLASSIC);
        ExternalMessage userMessage = new ExternalMessage(message);
        MessageDto up4UserMessageDto = new MessageDto(chatId, userMessage, up4Topic, null);
        messagingService.sendMessageAsync(up4UserMessageDto);
        if (up4Topic != duneTopic) {
            MessageDto duneUserMessageDto = new MessageDto(chatId, userMessage, duneTopic, null);
            messagingService.sendMessageAsync(duneUserMessageDto);
        }
    }
}
