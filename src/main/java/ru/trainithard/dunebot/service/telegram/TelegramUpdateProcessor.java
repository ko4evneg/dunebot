package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.TextCommandProcessor;

@Service
@RequiredArgsConstructor
public class TelegramUpdateProcessor {
    private final TelegramBot telegramBot;
    private final TelegramUpdateValidator telegramUpdateValidator;
    private final TextCommandProcessor textCommandProcessor;

    @Scheduled(fixedDelay = 500)
    public void process() {
        Update update = telegramBot.poll();
        while (update != null) {
            try {
                Message message = update.getMessage();
                if (message != null && message.getText() != null && telegramUpdateValidator.isValidCommand(message)) {
                    String text = message.getText();
                    long telegramUserId = message.getFrom().getId();
                    Command command = Command.getCommand(text.substring(1));
                    switch (command) {
                        case DUNE -> textCommandProcessor.registerNewMatch(telegramUserId, ModType.CLASSIC);
                        case UP4 -> textCommandProcessor.registerNewMatch(telegramUserId, ModType.UPRISING_4);
                        case UP6 -> textCommandProcessor.registerNewMatch(telegramUserId, ModType.UPRISING_6);
                        case CANCEL -> textCommandProcessor.cancelMatch(telegramUserId);
                    }
                }
                update = telegramBot.poll();
            } catch (Exception e) {
                // TODO:
                throw new RuntimeException(e);
            }
        }
    }
}
