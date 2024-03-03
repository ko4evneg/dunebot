package ru.trainithard.dunebot.service.messaging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.List;

@Getter
@NoArgsConstructor
public class MessageDto {
    @Setter
    protected String text;
    protected String chatId;
    protected Integer replyMessageId;
    protected List<List<ButtonDto>> keyboard;

    public MessageDto(String chatId, ExternalMessage externalMessage, @Nullable Integer replyMessageId, @Nullable List<List<ButtonDto>> linedButtons) {
        this.text = externalMessage.getText();
        this.chatId = chatId;
        this.replyMessageId = replyMessageId;
        this.keyboard = linedButtons;
    }

    public MessageDto(long chatId, ExternalMessage externalMessage, @Nullable Integer replyMessageId, @Nullable List<List<ButtonDto>> linedButtons) {
        this(Long.toString(chatId), externalMessage, replyMessageId, linedButtons);
    }

    public MessageDto(CommandMessage commandMessage, ExternalMessage externalMessage, @Nullable List<List<ButtonDto>> linedButtons) {
        this(Long.toString(commandMessage.getChatId()), externalMessage, commandMessage.getReplyMessageId(), linedButtons);
    }

    public MessageDto(ExternalMessageId externalMessageId, ExternalMessage externalMessage) {
        this(Long.toString(externalMessageId.getChatId()), externalMessage, externalMessageId.getReplyId(), null);
    }

    public MessageDto(AnswerableDuneBotException exception) {
        this(Long.toString(exception.getTelegramChatId()), new ExternalMessage(exception.getMessage()), exception.getTelegramReplyId(), null);
    }
}
