package ru.trainithard.dunebot.service.messaging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.messaging.dto.PollMessageDto;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TelegramMessagingServiceTest {
    private static final String SEND_POLL_CALLBACK_EXCEPTION_MESSAGE = "sendPollAsync() call encounters API exception";
    private static final String SEND_MESSAGE_CALLBACK_EXCEPTION_MESSAGE = "sendMessageAsync() call encounters API exception";
    private static final String DELETE_MESSAGE_CALLBACK_EXCEPTION_MESSAGE = "deleteMessageAsync() call encounters API exception";
    private static final String GET_FILE_DETAILS_EXCEPTION_MESSAGE = "getFile() call encounters API exception";

    private static final Integer MESSAGE_ID = 123;
    private static final Long CHAT_ID = 456L;
    private static final Integer REPLY_ID = 789;
    private static final String POLL_ID = "9000";
    private static final String FILE_ID = "randomFileId";

    private final TelegramBot telegramBot = mock(TelegramBot.class);
    private final TelegramMessagingService telegramMessagingService = new TelegramMessagingService(telegramBot);

    @Test
    void shouldInvokeDeleteCall() throws TelegramApiException {
        ExternalMessageId externalMessageId = new ExternalMessageId(MESSAGE_ID, CHAT_ID, REPLY_ID);

        telegramMessagingService.deleteMessageAsync(externalMessageId);

        ArgumentCaptor<DeleteMessage> deleteMessageCaptor = ArgumentCaptor.forClass(DeleteMessage.class);
        verify(telegramBot, times(1)).executeAsync(deleteMessageCaptor.capture());
        DeleteMessage actualDeleteMessage = deleteMessageCaptor.getValue();

        assertEquals(MESSAGE_ID, actualDeleteMessage.getMessageId());
        assertEquals(CHAT_ID.toString(), actualDeleteMessage.getChatId());
    }

    @Test
    void shouldWrapApiExceptionOnDeleteCall() throws TelegramApiException {
        doThrow(new TelegramApiException("abc")).when(telegramBot).executeAsync(ArgumentMatchers.any(DeleteMessage.class));
        ExternalMessageId externalMessageId = new ExternalMessageId(MESSAGE_ID, CHAT_ID, REPLY_ID);

        TelegramApiCallException actualException = assertThrows(TelegramApiCallException.class, () -> telegramMessagingService.deleteMessageAsync(externalMessageId));
        assertEquals(DELETE_MESSAGE_CALLBACK_EXCEPTION_MESSAGE, actualException.getMessage());
        assertEquals("abc", actualException.getCause().getMessage());
    }

    @Test
    void shouldInvokeSendPollCall() throws TelegramApiException {
        doReturn(CompletableFuture.completedFuture(getPollMessageReply())).when(telegramBot).executeAsync(ArgumentMatchers.any(SendPoll.class));
        PollMessageDto pollMessageDto = new PollMessageDto(MESSAGE_ID.toString(), "Poll question", REPLY_ID, List.of("1", "2", "3"));

        telegramMessagingService.sendPollAsync(pollMessageDto);

        ArgumentCaptor<SendPoll> sendPollCaptor = ArgumentCaptor.forClass(SendPoll.class);
        verify(telegramBot, times(1)).executeAsync(sendPollCaptor.capture());
        SendPoll actualSendPoll = sendPollCaptor.getValue();

        assertEquals(REPLY_ID, actualSendPoll.getReplyToMessageId());
        assertEquals("Poll question", actualSendPoll.getQuestion());
        assertNotNull(actualSendPoll.getIsAnonymous());
        assertFalse(actualSendPoll.getIsAnonymous());
        assertNotNull(actualSendPoll.getAllowMultipleAnswers());
        assertFalse(actualSendPoll.getAllowMultipleAnswers());
        assertThat(actualSendPoll.getOptions(), contains("1", "2", "3"));
    }

    private Message getPollMessageReply() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(REPLY_ID);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        Poll poll = new Poll();
        poll.setId(POLL_ID);
        Message message = new Message();
        message.setMessageId(100500);
        message.setChat(chat);
        message.setReplyToMessage(replyMessage);
        message.setPoll(poll);
        return message;
    }

    @Test
    void shouldWrapApiExceptionOnSendPollCall() throws TelegramApiException {
        doThrow(new TelegramApiException("abc")).when(telegramBot).executeAsync(ArgumentMatchers.any(SendPoll.class));
        PollMessageDto pollMessageDto = new PollMessageDto(MESSAGE_ID.toString(), "Poll question", REPLY_ID, List.of("1", "2", "3"));

        TelegramApiCallException actualException = assertThrows(TelegramApiCallException.class, () -> telegramMessagingService.sendPollAsync(pollMessageDto));

        assertEquals(SEND_POLL_CALLBACK_EXCEPTION_MESSAGE, actualException.getMessage());
        assertEquals("abc", actualException.getCause().getMessage());
    }

    @Test
    void shouldInvokeSendMessageCall() throws TelegramApiException {
        doReturn(CompletableFuture.completedFuture(getTextMessageReply())).when(telegramBot).executeAsync(ArgumentMatchers.any(SendMessage.class));
        MessageDto messageDto = new MessageDto(CHAT_ID, "la text", REPLY_ID, getKeyboard());

        telegramMessagingService.sendMessageAsync(messageDto);

        ArgumentCaptor<SendMessage> sendMessageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(1)).executeAsync(sendMessageCaptor.capture());
        SendMessage actualSendMessage = sendMessageCaptor.getValue();
        InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) actualSendMessage.getReplyMarkup();
        List<InlineKeyboardButton> buttons = replyMarkup.getKeyboard().stream().flatMap(Collection::stream).toList();

        assertEquals(REPLY_ID, actualSendMessage.getReplyToMessageId());
        assertEquals("la text", actualSendMessage.getText());
        assertEquals(CHAT_ID.toString(), actualSendMessage.getChatId());
        assertThat(buttons, contains(
                both(hasProperty("text", is("t1"))).and(hasProperty("callbackData", is("c1"))),
                both(hasProperty("text", is("t2"))).and(hasProperty("callbackData", is("c2"))),
                both(hasProperty("text", is("t3"))).and(hasProperty("callbackData", is("c3")))
        ));
    }

    private Message getTextMessageReply() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(REPLY_ID);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        Message message = new Message();
        message.setMessageId(100500);
        message.setText("the text");
        message.setReplyToMessage(replyMessage);
        return message;
    }

    @Test
    void shouldWrapApiExceptionOnSendMessageCall() throws TelegramApiException {
        doThrow(new TelegramApiException("abc")).when(telegramBot).executeAsync(ArgumentMatchers.any(SendMessage.class));
        MessageDto messageDto = new MessageDto(CHAT_ID, "la text", REPLY_ID, getKeyboard());

        TelegramApiCallException actualException = assertThrows(TelegramApiCallException.class, () -> telegramMessagingService.sendMessageAsync(messageDto));

        assertEquals(SEND_MESSAGE_CALLBACK_EXCEPTION_MESSAGE, actualException.getMessage());
        assertEquals("abc", actualException.getCause().getMessage());
    }

    private List<List<ButtonDto>> getKeyboard() {
        return List.of(
                List.of(new ButtonDto("t1", "c1")),
                List.of(new ButtonDto("t2", "c2"), new ButtonDto("t3", "c3"))
        );
    }

    @Test
    void shouldInvokeGetFileCall() throws TelegramApiException {
        doReturn(CompletableFuture.completedFuture(new GetFile(FILE_ID))).when(telegramBot).executeAsync(ArgumentMatchers.any(GetFile.class));

        telegramMessagingService.getFileDetails(FILE_ID);

        ArgumentCaptor<GetFile> getFileCaptor = ArgumentCaptor.forClass(GetFile.class);
        verify(telegramBot, times(1)).executeAsync(getFileCaptor.capture());
        GetFile actualGetFile = getFileCaptor.getValue();

        assertEquals(FILE_ID, actualGetFile.getFileId());
    }

    @Test
    void shouldWrapApiExceptionOnGetFileCall() throws TelegramApiException {
        doThrow(new TelegramApiException("abc")).when(telegramBot).executeAsync(ArgumentMatchers.any(GetFile.class));

        TelegramApiCallException actualException = assertThrows(TelegramApiCallException.class, () -> telegramMessagingService.getFileDetails(FILE_ID));

        assertEquals(GET_FILE_DETAILS_EXCEPTION_MESSAGE, actualException.getMessage());
        assertEquals("abc", actualException.getCause().getMessage());
    }
}
