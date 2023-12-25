package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.MatchCommandProcessor;
import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;

@Service
@RequiredArgsConstructor
public class TelegramUpdateProcessor {
    private final TelegramBot telegramBot;
    private final TelegramUpdateMessageValidator telegramUpdateMessageValidator;
    private final MatchCommandProcessor matchCommandProcessor;

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
            } catch (AnswerableDuneBotException answerableException) {
                sendUserNotificationMessage(answerableException);
            } catch (Exception e) {
                // TODO:
                System.out.println(e.getMessage());
            } finally {
                update = telegramBot.poll();
            }
        }
    }

    private void processCommand(Message message) throws TelegramApiException {
        long telegramUserId = message.getFrom().getId();
        String text = message.getText();
        String[] commandWithArguments = text.substring(1).split("\\s");
        Command command = Command.getCommand(commandWithArguments[0]);
        switch (command) {
            case DUNE -> matchCommandProcessor.registerNewMatch(telegramUserId, ModType.CLASSIC);
            case UP4 -> matchCommandProcessor.registerNewMatch(telegramUserId, ModType.UPRISING_4);
            case UP6 -> matchCommandProcessor.registerNewMatch(telegramUserId, ModType.UPRISING_6);
            case CANCEL -> matchCommandProcessor.cancelMatch(telegramUserId);
            case SUBMIT -> matchCommandProcessor.getSubmitMessage(telegramUserId, message.getChatId(), text);
            case REGISTER ->
                    matchCommandProcessor.registerNewPlayer(new PlayerRegistrationDto(message, commandWithArguments[1]));
        }
    }

    private void sendUserNotificationMessage(AnswerableDuneBotException answerableException) {
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
