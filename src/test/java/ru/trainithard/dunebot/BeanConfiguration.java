package ru.trainithard.dunebot;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

@Configuration
@Profile(value = "test")
public class BeanConfiguration {
    @Bean
    TelegramBot telegramBot() {
        return Mockito.mock(TelegramBot.class);
    }
}
