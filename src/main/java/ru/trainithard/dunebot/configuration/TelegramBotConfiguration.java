package ru.trainithard.dunebot.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

@Configuration
@Profile(value = "!test")
public class TelegramBotConfiguration {
    @Value("${bot.token}")
    private String botToken;
    @Value("${bot.username}")
    private String botUsername;

    @Bean
    TelegramBot telegramBot() {
        return new TelegramBot(botUsername, botToken);
    }
}
