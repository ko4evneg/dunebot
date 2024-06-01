package ru.trainithard.dunebot;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.trainithard.dunebot.service.StartupService;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

@Configuration
@Profile(value = "!test")
@RequiredArgsConstructor
public class TelegramBotInitializer implements CommandLineRunner {
    private final TelegramBot telegramBot;
    private final StartupService startupService;

    @Override
    public void run(String... args) throws Exception {
        startupService.startUp();
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(telegramBot);
    }
}
