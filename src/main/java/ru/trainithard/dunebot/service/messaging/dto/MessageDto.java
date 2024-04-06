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
    protected Integer replyMessageId;
    protected String chatId;
    protected Integer topicId;
    protected List<List<ButtonDto>> keyboard;

    public MessageDto(String chatId, ExternalMessage externalMessage, @Nullable Integer topicId, @Nullable Integer replyMessageId, @Nullable List<List<ButtonDto>> linedButtons) {
        this.text = externalMessage.getText();
        this.chatId = chatId;
        this.topicId = topicId;
        this.keyboard = linedButtons;
        this.replyMessageId = replyMessageId;
    }

    public MessageDto(String chatId, ExternalMessage externalMessage, @Nullable Integer topicId, @Nullable List<List<ButtonDto>> linedButtons) {
        this(chatId, externalMessage, topicId, null, linedButtons);
    }

    public MessageDto(long chatId, ExternalMessage externalMessage, @Nullable Integer topicId, @Nullable List<List<ButtonDto>> linedButtons) {
        this(Long.toString(chatId), externalMessage, topicId, null, linedButtons);
    }

    public MessageDto(CommandMessage commandMessage, ExternalMessage externalMessage, @Nullable List<List<ButtonDto>> linedButtons) {
        this(Long.toString(commandMessage.getChatId()), externalMessage, commandMessage.getTopicId(), commandMessage.getReplyMessageId(), linedButtons);
    }

    public MessageDto(ExternalMessageId externalMessageId, ExternalMessage externalMessage) {
        this(Long.toString(externalMessageId.getChatId()), externalMessage, externalMessageId.getReplyId(), null);
    }

    public MessageDto(AnswerableDuneBotException exception) {
        this(Long.toString(exception.getTelegramChatId()), new ExternalMessage(exception.getMessage()), exception.getTelegramReplyId(), null);
    }
}
