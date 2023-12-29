package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.MatchCommandProcessor;
import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;
import ru.trainithard.dunebot.service.telegram.command.dto.MessageCommand;

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
                    MessageCommand messageCommand = new MessageCommand(message);
                    telegramUpdateMessageValidator.validate(messageCommand);
                    processCommand(messageCommand);
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

    private void processCommand(MessageCommand messageCommand) {
        long telegramUserId = messageCommand.getTelegramUserId();
        switch (messageCommand.getCommand()) {
            case DUNE -> matchCommandProcessor.registerNewMatch(telegramUserId, ModType.CLASSIC);
            case UP4 -> matchCommandProcessor.registerNewMatch(telegramUserId, ModType.UPRISING_4);
            case UP6 -> matchCommandProcessor.registerNewMatch(telegramUserId, ModType.UPRISING_6);
            case CANCEL -> matchCommandProcessor.cancelMatch(telegramUserId);
//            case SUBMIT ->
//                    matchCommandProcessor.getSubmitMessage(telegramUserId, message.getChatId(), commandWithArguments[1]);
            case REGISTER -> matchCommandProcessor.registerNewPlayer(new PlayerRegistrationDto(messageCommand));
        }
    }

    private void sendUserNotificationMessage(AnswerableDuneBotException answerableException) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(answerableException.getTelegramChatId());
            sendMessage.setText(answerableException.getMessage());
            if (answerableException.getTelegramTopicId() != null) {
                sendMessage.setReplyToMessageId(answerableException.getTelegramTopicId());
            }
            telegramBot.executeAsync(sendMessage);
        } catch (Exception e) {
            // TODO:
            throw new RuntimeException(e);
        }
    }
}
