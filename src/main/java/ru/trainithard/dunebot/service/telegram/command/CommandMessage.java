package ru.trainithard.dunebot.service.telegram.command;

import lombok.AccessLevel;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.telegram.ChatType;

import java.util.Arrays;
import java.util.List;

@Getter
public class CommandMessage {
    private final long userId;
    private Command command;
    private ChatType chatType;
    private Integer replyMessageId;
    private String firstName;
    private String lastName;
    private String userName;
    private long chatId;
    private int messageId;
    private PollVote pollVote;
    @Getter(AccessLevel.NONE)
    private String[] args;

    public CommandMessage(Message message) {
        User user = message.getFrom();
        this.userId = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.userName = user.getUserName();
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.chatType = ChatType.valueOf(message.getChat().getType().toUpperCase());
        Message replyToMessage = message.getReplyToMessage();
        if (replyToMessage != null) {
            this.replyMessageId = replyToMessage.getMessageId();
        }
        String text = message.getText();
        if (text != null && text.length() > 1) {
            String[] commandWithArguments = text.substring(1).split("\\s");
            this.command = Command.getCommand(commandWithArguments[0]);
            this.args = commandWithArguments.length > 1 ? Arrays.copyOfRange(commandWithArguments, 1, commandWithArguments.length) : new String[0];
        } else {
            this.args = new String[0];
        }
    }

    public CommandMessage(PollAnswer pollAnswer) {
        this.command = Command.VOTE;
        this.userId = pollAnswer.getUser().getId();
        this.pollVote = new PollVote(pollAnswer);
    }

    /**
     * @param argumentNumber number of argument starting from 1
     * @return argument <code>String</code> value
     */
    public String getArgument(int argumentNumber) {
        return args.length > argumentNumber ? null : args[argumentNumber - 1];
    }

    public String getAllArguments() {
        return args.length == 0 ? null : String.join(" ", args);
    }

    public int getArgumentsCount() {
        return args.length;
    }

    public record PollVote(String pollId, List<Integer> selectedAnswerId) {
        private PollVote(PollAnswer pollAnswer) {
            this(pollAnswer.getPollId(), pollAnswer.getOptionIds());
        }
    }
}
