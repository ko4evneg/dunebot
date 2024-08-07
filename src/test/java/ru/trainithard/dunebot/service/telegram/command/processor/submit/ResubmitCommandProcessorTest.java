package ru.trainithard.dunebot.service.telegram.command.processor.submit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.validator.SubmitMatchValidator;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

@SpringBootTest
class ResubmitCommandProcessorTest extends TestContextMock {
    private static final Long USER_ID = 11000L;
    private static final int RESUBMITS_LIMIT = 3;

    private final Match match = new Match();
    private final CommandMessage resubmitCommandMessage = getCommandMessage(USER_ID);

    @Autowired
    private ResubmitCommandProcessor processor;
    @MockBean
    private SubmitMatchValidator submitMatchValidator;
    @MockBean
    private MatchRepository matchRepository;
    @MockBean
    private MatchFinishingService matchFinishingService;
    @MockBean
    private AppSettingsService appSettingsService;

    @BeforeEach
    void beforeEach() {
        Player player = new Player();
        player.setExternalId(12345L);
        match.setId(15000L);
        match.setSubmitter(player);
        match.setExternalSubmitId(new ExternalMessageId(12345, 10000, null));

        doReturn(Optional.of(match)).when(matchRepository).findWithMatchPlayersBy(15000L);
        doReturn(RESUBMITS_LIMIT).when(appSettingsService).getIntSetting(AppSettingKey.RESUBMITS_LIMIT);
    }

    @Test
    void shouldInvokePartialMatchFinishWhenResubmitsLimitReached() {
        match.setSubmitsRetryCount(RESUBMITS_LIMIT);

        processor.process(resubmitCommandMessage);

        verify(matchFinishingService).finishPartiallySubmittedMatch(match.getId(), true);
    }

    @Test
    void shouldNotInvokePartialMatchFinishWhenResubmitsLimitNotReached() {
        match.setSubmitsRetryCount(RESUBMITS_LIMIT - 1);

        processor.process(resubmitCommandMessage);

        verifyNoInteractions(matchFinishingService);
    }

    @Test
    void shouldSendFailedByResubmitMessageWhenResubmitsLimitReached() {
        match.setSubmitsRetryCount(RESUBMITS_LIMIT);
        processor.process(resubmitCommandMessage);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertThat(messageDto)
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly(USER_ID.toString(), "Превышено максимальное количество попыток регистрации результата матча \\(3\\)");
    }

    @Test
    void shouldInvokeSubmitMatchValidator() {
        processor.process(resubmitCommandMessage);

        InOrder inOrder = inOrder(messagingService, submitMatchValidator);
        inOrder.verify(submitMatchValidator).validateReSubmitMatch(same(resubmitCommandMessage), same(match));
        inOrder.verify(messagingService).sendMessageAsync(any());
    }

    @Test
    void shouldSendCorrectResubmitMessage() {
        processor.process(resubmitCommandMessage);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertThat(messageDto)
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly(USER_ID.toString(), "Если вы знаете все места и лидеров, вы можете выполнить перерегистрацию результата самостоятельно, " +
                                                     "иначе \\- запрос будет отправлен игроку, выполнившему предыдущую регистрацию результатов\\.");
        assertThat(messageDto.getKeyboard())
                .flatExtracting(buttonDtos -> buttonDtos)
                .extracting(ButtonDto::getText, ButtonDto::getCallback)
                .containsExactlyInAnyOrder(
                        tuple("Хочу сам", "15000_RSC_" + USER_ID),
                        tuple("Передам прошлому", "15000_RSC_12345")
                );
    }

    private CommandMessage getCommandMessage(long userId) {
        User user = new User();
        user.setId(userId);
        Chat chat = new Chat();
        chat.setId(userId);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(100_500);
        message.setFrom(user);
        message.setChat(chat);
        message.setText("/" + Command.RESUBMIT.name() + " 15000");
        return CommandMessage.getMessageInstance(message);
    }
}
