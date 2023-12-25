package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.exception.AnswerableDubeBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.TextCommandProcessor;
import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;

@Service
@RequiredArgsConstructor
public class TelegramUpdateProcessor {
    private final TelegramBot telegramBot;
    private final TelegramUpdateMessageValidator telegramUpdateMessageValidator;
    private final TextCommandProcessor textCommandProcessor;

    @Scheduled(fixedDelay = 500)
    public void process() {
        Update update = telegramBot.poll();
        while (update != null) {
            try {
                Message message = update.getMessage();
                // TODO: validate only valid messages goes further [!!!] commands should contain all complete actions with valid arguments
                if (message != null && message.getText() != null && message.getText().startsWith("/")) {
                    telegramUpdateMessageValidator.validate(message);
                    processCommand(message);
                }
            } catch (AnswerableDubeBotException answerableException) {
                sendUserNotificationMessage(answerableException);
            } catch (Exception e) {
                // TODO:
            } finally {
                update = telegramBot.poll();
            }
        }
    }

    private void processCommand(Message message) {
        long telegramUserId = message.getFrom().getId();
        String text = message.getText();
        String[] commandWithArguments = text.substring(1).split("\\s");
        Command command = Command.getCommand(commandWithArguments[0]);
        switch (command) {
            case DUNE -> textCommandProcessor.registerNewMatch(telegramUserId, ModType.CLASSIC);
            case UP4 -> textCommandProcessor.registerNewMatch(telegramUserId, ModType.UPRISING_4);
            case UP6 -> textCommandProcessor.registerNewMatch(telegramUserId, ModType.UPRISING_6);
            case CANCEL -> textCommandProcessor.cancelMatch(telegramUserId);
            case REGISTER ->
                    textCommandProcessor.registerNewPlayer(new PlayerRegistrationDto(message, commandWithArguments[1]));
        }
    }

    private void sendUserNotificationMessage(AnswerableDubeBotException answerableException) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(answerableException.getTelegramChatId());
            sendMessage.setText(answerableException.getMessage());
            telegramBot.executeAsync(sendMessage);
        } catch (Exception e) {
            // TODO:
            throw new RuntimeException(e);
        }
    }
}
