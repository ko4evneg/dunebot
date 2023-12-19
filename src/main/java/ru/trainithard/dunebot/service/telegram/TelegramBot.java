package ru.trainithard.dunebot.service.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class TelegramBot extends TelegramLongPollingBot {
    private final String botUserName;

    public TelegramBot(String botUserName, String botToken) {
        super(botToken);
        this.botUserName = botUserName;
    }

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // TODO: 18.12.2023
        System.out.println(update.getMessage().getText());
    }
}
