package ru.trainithard.dunebot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.trainithard.dunebot.telegram.TelegramBot;

//todo
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class DunebotApplication implements CommandLineRunner {
    @Value("${bot.token}")
    private String botToken;
    @Value("${bot.username}")
    private String botUsername;

    public static void main(String[] args) {
        SpringApplication.run(DunebotApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new TelegramBot(botUsername, botToken));
    }
}
