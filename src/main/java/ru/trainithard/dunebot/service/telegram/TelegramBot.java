package ru.trainithard.dunebot.service.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TelegramBot extends TelegramLongPollingBot {
    private final String botUserName;
    private final ConcurrentLinkedQueue<Update> updates = new ConcurrentLinkedQueue<>();

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

    public Update poll() {
        return updates.poll();
    }
}
