package ru.trainithard.dunebot.service.telegram.command;

import lombok.AccessLevel;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.telegram.ChatType;

import java.util.Arrays;

@Getter
public class CommandMessage {
    private final Command command;
    private final ChatType chatType;
    private final long userId;
    private final long chatId;
    private final Integer replyMessageId;
    private final String firstName;
    private final String lastName;
    private final String userName;
    @Getter(AccessLevel.NONE)
    private final String[] args;

    public CommandMessage(Message message) {
        User user = message.getFrom();
        this.userId = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.userName = user.getUserName();
        this.chatId = message.getChatId();
        this.chatType = ChatType.valueOf(message.getChat().getType().toUpperCase());
        Message replyToMessage = message.getReplyToMessage();
        this.replyMessageId = replyToMessage == null ? null : replyToMessage.getMessageId();
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
