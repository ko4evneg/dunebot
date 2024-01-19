package ru.trainithard.dunebot.service.telegram.command;

import lombok.AccessLevel;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.telegram.ChatType;

import java.util.Arrays;
import java.util.List;

@Getter
public class CommandMessage {
    /**
     * Represents external user ID.
     */
    private final long userId;
    /**
     * Command to execute.
     */
    private Command command;
    /**
     * Represent external messaging system chat type.
     */
    private ChatType chatType;
    /**
     * External ID of the message, for which this message is a reply.
     */
    private Integer replyMessageId;
    /**
     * External user first name.
     */
    private String firstName;
    /**
     * External user last name.
     */
    private String lastName;
    /**
     * External system user identifier (like tag, login etc...)
     */
    private String userName;
    /**
     * External system chat ID to which message is belong.
     */
    private long chatId;
    /**
     * External system message ID.
     */
    private int messageId;
    /**
     * If a message is an answer to a poll, this field contains such an answer.
     */
    private PollVote pollVote;
    /**
     * If a message is a callback, this field contains such a callback.
     */
    private String callback;
    /**
     * Contains all arguments of the command. Parsed from the whole command text using any space as arguments separator.
     */
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

    public CommandMessage(CallbackQuery callbackQuery) {
        this.command = Command.ACCEPT_SUBMIT;
        this.userId = callbackQuery.getFrom().getId();
        this.callback = callbackQuery.getData();
        this.replyMessageId = callbackQuery.getMessage().getMessageId();
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
