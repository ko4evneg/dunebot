package ru.trainithard.dunebot.service.messaging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.service.messaging.dto.*;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.*;

class TelegramMessagingServiceTest {
    private static final Integer MESSAGE_ID = 123;
    private static final Long CHAT_ID = 456L;
    private static final Integer TOPIC_ID = 789;
    private static final Integer REPLY_ID = 3000;
    private static final String POLL_ID = "9000";
    private static final String FILE_ID = "randomFileId";

    private final TelegramBot telegramBot = mock(TelegramBot.class);
    private final TelegramMessagingService telegramMessagingService = new TelegramMessagingService(telegramBot);

    @Test
    void shouldInvokeDeleteCall() throws TelegramApiException {
        ExternalMessageId externalMessageId = new ExternalMessageId(MESSAGE_ID, CHAT_ID, TOPIC_ID);

        telegramMessagingService.deleteMessageAsync(externalMessageId);

        ArgumentCaptor<DeleteMessage> deleteMessageCaptor = ArgumentCaptor.forClass(DeleteMessage.class);
        verify(telegramBot, times(1)).executeAsync(deleteMessageCaptor.capture());
        DeleteMessage actualDeleteMessage = deleteMessageCaptor.getValue();

        assertThat(actualDeleteMessage)
                .extracting(DeleteMessage::getMessageId, DeleteMessage::getChatId)
                .containsExactly(MESSAGE_ID, CHAT_ID.toString());
    }

    @Test
    void shouldInvokeSendPollCall() throws TelegramApiException {
        doReturn(CompletableFuture.completedFuture(getPollMessageReply())).when(telegramBot).executeAsync(ArgumentMatchers.any(SendPoll.class));
        PollMessageDto pollMessageDto = new PollMessageDto(MESSAGE_ID.toString(), new ExternalMessage("Poll question"), TOPIC_ID, List.of("1", "2", "3"));

        telegramMessagingService.sendPollAsync(pollMessageDto);

        ArgumentCaptor<SendPoll> sendPollCaptor = ArgumentCaptor.forClass(SendPoll.class);
        verify(telegramBot, times(1)).executeAsync(sendPollCaptor.capture());
        SendPoll actualSendPoll = sendPollCaptor.getValue();

        assertThat(actualSendPoll.getReplyToMessageId()).isEqualTo(TOPIC_ID);
        assertThat(actualSendPoll.getQuestion()).isEqualTo("Poll question");
        assertThat(actualSendPoll.getIsAnonymous()).isNotNull().isFalse();
        assertThat(actualSendPoll.getAllowMultipleAnswers()).isNotNull().isFalse();
        assertThat(actualSendPoll.getOptions()).containsExactly("1", "2", "3");
    }

    private Message getPollMessageReply() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(TOPIC_ID);
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
    void shouldInvokeSendMessageCall() throws TelegramApiException {
        doReturn(CompletableFuture.completedFuture(getTextMessageReply())).when(telegramBot).executeAsync(ArgumentMatchers.any(SendMessage.class));
        MessageDto messageDto = new MessageDto(CHAT_ID.toString(), new ExternalMessage("la text"), TOPIC_ID, REPLY_ID, getKeyboard());

        telegramMessagingService.sendMessageAsync(messageDto);

        ArgumentCaptor<SendMessage> sendMessageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot, times(1)).executeAsync(sendMessageCaptor.capture());

        SendMessage actualSendMessage = sendMessageCaptor.getValue();
        assertThat(actualSendMessage)
                .extracting(SendMessage::getMessageThreadId, SendMessage::getText, SendMessage::getChatId)
                .containsExactly(TOPIC_ID, "la text", CHAT_ID.toString());

        InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) actualSendMessage.getReplyMarkup();
        List<InlineKeyboardButton> buttons = replyMarkup.getKeyboard().stream().flatMap(Collection::stream).toList();
        assertThat(buttons).extracting(InlineKeyboardButton::getText, InlineKeyboardButton::getCallbackData)
                .containsExactly(
                        tuple("t1", "c1"),
                        tuple("t2", "c2"),
                        tuple("t3", "c3")
                );
    }

    @Test
    void shouldRetrySendMessageOnCallbackException() throws TelegramApiException, InterruptedException, ReflectiveOperationException {
        Map<Integer, Long> testRetryDelays = Map.of(1, 50L, 2, 100L, 3, 250L, 4, 1L);
        Field field = TelegramMessagingService.class.getDeclaredField("retryDelayByTryNumber");
        field.setAccessible(true);
        field.set(telegramMessagingService, testRetryDelays);

        when(telegramBot.executeAsync(any(SendMessage.class)))
                .thenThrow(new TelegramApiException("xxx"))
                .thenReturn(CompletableFuture.failedFuture(new TelegramApiException("yyy")))
                .thenReturn(CompletableFuture.failedFuture(new TelegramApiException("zzz")))
                .thenReturn(CompletableFuture.completedFuture(getTextMessageReply()));

        MessageDto messageDto = new MessageDto(CHAT_ID.toString(), new ExternalMessage("la text"), TOPIC_ID, REPLY_ID, getKeyboard());

        CompletableFuture<ExternalMessageDto> actualFeature = telegramMessagingService.sendMessageAsync(messageDto);
        Thread.sleep(300);
        assertThat(actualFeature.isDone()).isFalse();

        Thread.sleep(300);
        verify(telegramBot, times(4)).executeAsync(ArgumentMatchers.any(SendMessage.class));
        assertThat(actualFeature.isDone()).isTrue();
        assertThat(actualFeature.isCompletedExceptionally()).isFalse();

        reset(telegramBot);
        Thread.sleep(500);
        verifyNoInteractions(telegramBot);
    }

    @Test
    void shouldNotRetrySendMessageWithoutException() throws TelegramApiException, InterruptedException, ReflectiveOperationException {
        Map<Integer, Long> testRetryDelays = Map.of(1, 50L, 2, 100L, 3, 250L, 4, 1L);
        Field field = TelegramMessagingService.class.getDeclaredField("retryDelayByTryNumber");
        field.setAccessible(true);
        field.set(telegramMessagingService, testRetryDelays);

        when(telegramBot.executeAsync(any(SendMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(getTextMessageReply()));

        MessageDto messageDto = new MessageDto(CHAT_ID.toString(), new ExternalMessage("la text"), TOPIC_ID, REPLY_ID, getKeyboard());

        CompletableFuture<ExternalMessageDto> actualFeature = telegramMessagingService.sendMessageAsync(messageDto);
        Thread.sleep(300);

        verify(telegramBot).executeAsync(ArgumentMatchers.any(SendMessage.class));
        assertThat(actualFeature.isDone()).isTrue();
        assertThat(actualFeature.isCompletedExceptionally()).isFalse();
    }

    @Test
    void shouldDoTheOnlyRetryWhenOnlyOneExceptionOccurs() throws TelegramApiException, InterruptedException, ReflectiveOperationException {
        Map<Integer, Long> testRetryDelays = Map.of(1, 50L, 2, 100L, 3, 250L, 4, 1L);
        Field field = TelegramMessagingService.class.getDeclaredField("retryDelayByTryNumber");
        field.setAccessible(true);
        field.set(telegramMessagingService, testRetryDelays);

        when(telegramBot.executeAsync(any(SendMessage.class)))
                .thenThrow(new TelegramApiException("xxx"))
                .thenReturn(CompletableFuture.completedFuture(getTextMessageReply()))
                .thenReturn(CompletableFuture.failedFuture(new TelegramApiException("zzz")));

        MessageDto messageDto = new MessageDto(CHAT_ID.toString(), new ExternalMessage("la text"), TOPIC_ID, REPLY_ID, getKeyboard());

        CompletableFuture<ExternalMessageDto> actualFeature = telegramMessagingService.sendMessageAsync(messageDto);
        Thread.sleep(250);

        verify(telegramBot, times(2)).executeAsync(ArgumentMatchers.any(SendMessage.class));
        assertThat(actualFeature.isDone()).isTrue();
        assertThat(actualFeature.isCompletedExceptionally()).isFalse();
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

        assertThat(actualGetFile.getFileId()).isEqualTo(FILE_ID);
    }

    @Test
    void shouldInvokeSendMessageCallOnFileSend() throws IOException {
        byte[] referenceFileContent = "la_file_content".getBytes();
        doReturn(CompletableFuture.completedFuture(getTextMessageReply())).when(telegramBot).executeAsync(ArgumentMatchers.any(SendDocument.class));
        FileMessageDto fileMessageDto = new FileMessageDto(CHAT_ID.toString(), new ExternalMessage("la text"), TOPIC_ID, referenceFileContent, "file.txt");

        telegramMessagingService.sendFileAsync(fileMessageDto);

        ArgumentCaptor<SendDocument> sendDocumentCaptor = ArgumentCaptor.forClass(SendDocument.class);
        verify(telegramBot, times(1)).executeAsync(sendDocumentCaptor.capture());
        SendDocument actualDocument = sendDocumentCaptor.getValue();
        byte[] actualFile = actualDocument.getFile().getNewMediaStream().readAllBytes();

        assertThat(actualDocument.getCaption()).isEqualTo("la text");
        assertThat(actualDocument.getReplyToMessageId()).isEqualTo(TOPIC_ID);
        assertThat(actualDocument.getChatId()).isEqualTo(CHAT_ID.toString());
        assertThat(actualFile).containsExactly(referenceFileContent);
    }

    @Test
    void shouldInvokeSendCommands() throws TelegramApiException {
        SetCommandsDto setCommandsDto = new SetCommandsDto(Map.of("1", "a", "2", "b"));

        telegramMessagingService.sendSetCommands(setCommandsDto);

        ArgumentCaptor<SetMyCommands> setMyCommandsCaptor = ArgumentCaptor.forClass(SetMyCommands.class);
        verify(telegramBot, times(1)).executeAsync(setMyCommandsCaptor.capture());
        SetMyCommands actualSetMyCommands = setMyCommandsCaptor.getValue();

        assertThat(actualSetMyCommands.getScope()).isEqualTo(new BotCommandScopeDefault());
        assertThat(actualSetMyCommands.getCommands())
                .extracting(BotCommand::getCommand, BotCommand::getDescription)
                .containsExactlyInAnyOrder(
                        tuple("1", "a"),
                        tuple("2", "b"));
    }

    private Message getTextMessageReply() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(REPLY_ID);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        Message message = new Message();
        message.setMessageId(100500);
        message.setChat(chat);
        message.setText("the text");
        message.setReplyToMessage(replyMessage);
        message.setMessageThreadId(TOPIC_ID);
        return message;
    }
}
