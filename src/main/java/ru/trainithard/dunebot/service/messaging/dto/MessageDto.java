package ru.trainithard.dunebot.service.messaging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.util.MarkdownEscaper;

import java.util.List;

@Getter
@NoArgsConstructor
public class MessageDto {
    @Setter
    protected String text;
    protected String chatId;
    protected Integer replyMessageId;
    protected List<List<ButtonDto>> keyboard;

    public MessageDto(String chatId, String text, @Nullable Integer replyMessageId, @Nullable List<List<ButtonDto>> linedButtons) {
        this.text = text;
        this.chatId = chatId;
        this.replyMessageId = replyMessageId;
        this.keyboard = linedButtons;
    }

    public MessageDto(long chatId, String text, @Nullable Integer replyMessageId, @Nullable List<List<ButtonDto>> linedButtons) {
        this(Long.toString(chatId), MarkdownEscaper.getEscaped(text), replyMessageId, linedButtons);
    }

    public MessageDto(CommandMessage commandMessage, ExternalMessage externalMessage, @Nullable List<List<ButtonDto>> linedButtons) {
        this(Long.toString(commandMessage.getChatId()), externalMessage.getText(), commandMessage.getReplyMessageId(), linedButtons);
    }

    public MessageDto(ExternalMessageId externalMessageId, String text) {
        this(Long.toString(externalMessageId.getChatId()), MarkdownEscaper.getEscaped(text), externalMessageId.getReplyId(), null);
    }

    public MessageDto(AnswerableDuneBotException exception) {
        this(Long.toString(exception.getTelegramChatId()), MarkdownEscaper.getEscaped(exception.getMessage()), exception.getTelegramReplyId(), null);
    }
}
