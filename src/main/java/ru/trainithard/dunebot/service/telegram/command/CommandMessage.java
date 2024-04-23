package ru.trainithard.dunebot.service.telegram.command;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import ru.trainithard.dunebot.model.messaging.ChatType;

import java.util.Arrays;
import java.util.List;

/**
 * Core DTO representing external messaging system commands.
 */
@Getter
@EqualsAndHashCode
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
     * External system user first name
     */
    private Integer topicId;
    private String externalFirstName;
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
     * If a message is a document, this field contains such a document.
     */
    private File file;
    /**
     * If a message is a photo, this field contains such a photo.
     */
    private List<Photo> photo;
    /**
     * Contains all arguments of the command. Parsed from the whole command text using any space as arguments separator.
     */
    @Getter(AccessLevel.NONE)
    private String[] args;

    private CommandMessage(PollAnswer pollAnswer) {
        this.command = Command.VOTE;
        User user = pollAnswer.getUser();
        this.userId = user.getId();
        this.externalFirstName = user.getFirstName();
        this.chatId = user.getId();
        this.pollVote = new PollVote(pollAnswer);
    }

    private CommandMessage(CallbackQuery callbackQuery) {
        this.command = Command.ACCEPT_SUBMIT;
        this.userId = callbackQuery.getFrom().getId();
        this.callback = callbackQuery.getData();
    }

    private CommandMessage(Message message) {
        User user = message.getFrom();
        this.userId = user.getId();
        this.userName = user.getUserName();
        this.externalFirstName = user.getFirstName();
        this.chatId = message.getChatId();
        this.messageId = message.getMessageId();
        this.chatType = ChatType.valueOf(message.getChat().getType().toUpperCase());
        this.topicId = message.getMessageThreadId();
        Message replyToMessage = message.getReplyToMessage();
        if (replyToMessage != null) {
            this.replyMessageId = replyToMessage.getMessageId();
        }
        assignTextSourcedValues(message);
        if (message.hasDocument()) {
            this.file = new File(message.getDocument());
            this.command = Command.UPLOAD_PHOTO;
        }
        if (message.hasPhoto()) {
            this.photo = message.getPhoto().stream().map(Photo::new).toList();
            this.command = Command.UPLOAD_PHOTO;
        }
    }

    private void assignTextSourcedValues(Message message) {
        String text = message.getText();
        if (text != null && text.length() > 1) {
            String[] commandWithArguments = text.substring(1).split("\\s+");
            this.command = Command.getCommand(commandWithArguments[0]);
            this.args = commandWithArguments.length > 1
                    ? Arrays.copyOfRange(commandWithArguments, 1, commandWithArguments.length)
                    : new String[0];
        } else {
            this.args = new String[0];
        }
    }

    public static CommandMessage getMessageInstance(Message message) {
        return new CommandMessage(message);
    }

    public static CommandMessage getCallbackInstance(CallbackQuery callbackQuery) {
        return new CommandMessage(callbackQuery);
    }

    public static CommandMessage getPollAnswerInstance(PollAnswer pollAnswer) {
        return new CommandMessage(pollAnswer);
    }

    /**
     * @param argumentNumber number of argument starting from 1
     * @return argument <code>String</code> value
     */
    public String getArgument(int argumentNumber) {
        return argumentNumber > args.length || argumentNumber < 1 ? null : args[argumentNumber - 1];
    }

    /**
     * @return all provided arguments. Empty string if no arguments in command
     */
    public String getAllArguments() {
        return args.length == 0 ? "" : String.join(" ", args);
    }

    public int getArgumentsCount() {
        return args.length;
    }

    public record PollVote(String pollId, List<Integer> selectedAnswerId) {
        private PollVote(PollAnswer pollAnswer) {
            this(pollAnswer.getPollId(), pollAnswer.getOptionIds());
        }
    }

    public record File(String id, Long size, String mimeType) {
        private File(Document document) {
            this(document.getFileId(), document.getFileSize(), document.getMimeType());
        }
    }

    public record Photo(String id, Integer size) {
        private Photo(PhotoSize photo) {
            this(photo.getFileId(), photo.getFileSize());
        }
    }
}
