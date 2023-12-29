package ru.trainithard.dunebot.service.telegram.command;

import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.telegram.ChatType;

import java.util.Arrays;

public class MessageCommand {
    @Getter
    private final Command command;
    @Getter
    private final ChatType chatType;
    private final Message message;
    private final String[] args;

    public MessageCommand(Message message) {
        this.message = message;
        this.chatType = ChatType.valueOf(message.getChat().getType().toUpperCase());
        String text = message.getText();
        if (text != null && text.length() > 1) {
            String[] commandWithArguments = text.substring(1).split("\\s");
            this.command = Command.getCommand(commandWithArguments[0]);
            this.args = commandWithArguments.length > 1 ? Arrays.copyOfRange(commandWithArguments, 1, commandWithArguments.length) : new String[0];
        } else {
            this.command = null;
            this.args = new String[0];
        }
    }

    public long getTelegramUserId() {
        return message.getFrom().getId();
    }

    public String getTelegramFirstName() {
        return message.getFrom().getFirstName();
    }

    public String getTelegramLastName() {
        return message.getFrom().getLastName();
    }

    public String getTelegramUserName() {
        return message.getFrom().getUserName();
    }

    public long getTelegramChatId() {
        return message.getChatId();
    }

    public Integer getReplyMessageId() {
        Message replyToMessage = message.getReplyToMessage();
        return replyToMessage == null ? null : replyToMessage.getMessageId();
    }

    /**
     * @param argumentNumber number of argument starting from 1
     * @return argument <code>String</code> value
     */
    public String getArgument(int argumentNumber) {
        return args.length > argumentNumber ? null : args[argumentNumber - 1];
    }

    public int getArgumentsCount() {
        return args.length;
    }
}
