package ru.trainithard.dunebot.service.telegram.command.processor;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.service.MatchSubmitService;
import ru.trainithard.dunebot.service.dto.MatchSubmitDto;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class SubmitCommandProcessor implements CommandProcessor {
    private final MatchSubmitService matchSubmitService;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage) {
        String matchIdString = commandMessage.getArgument(1);
        MatchSubmitDto matchSubmit = matchSubmitService.getMatchSubmit(commandMessage.getUserId(), commandMessage.getChatId(), matchIdString);
        List<MatchPlayer> registeredMatchPlayers = matchSubmit.activePlayers();
        for (MatchPlayer matchPlayer : registeredMatchPlayers) {
            MessageDto pollMessage = getPollMessage(matchPlayer, registeredMatchPlayers, matchIdString);
            CompletableFuture<ExternalMessageDto> messageCompletableFuture = messagingService.sendMessageAsync(pollMessage);
            messageCompletableFuture.whenComplete((message, throwable) -> {
                matchPlayer.setSubmitMessageId(new ExternalMessageId(message.getMessageId(), message.getChatId(), message.getReplyId()));
                matchPlayerRepository.save(matchPlayer);
            });
        }
    }

    private MessageDto getPollMessage(MatchPlayer matchPlayer, List<MatchPlayer> registeredMatchPlayers, String matchIdString) {
        String text = String.format("Выберите место, которое вы заняли в матче %s:", matchIdString);
        List<List<ButtonDto>> pollKeyboard = getPollKeyboard(registeredMatchPlayers, matchIdString);
        String playersChatId = Long.toString(matchPlayer.getPlayer().getExternalChatId());
        return new MessageDto(playersChatId, text, null, pollKeyboard);
    }

    private List<List<ButtonDto>> getPollKeyboard(List<MatchPlayer> registeredMatchPlayers, String matchIdString) {
        List<ButtonDto> buttons = new ArrayList<>();
        for (int i = 0; i < registeredMatchPlayers.size(); i++) {
            ButtonDto buttonDto = new ButtonDto(Integer.toString(i + 1), matchIdString + "__" + (i + 1));
            buttons.add(buttonDto);
        }
        buttons.add(new ButtonDto("не участвовал(а)", matchIdString + "__-1"));
        return Lists.partition(buttons, 2);
    }

    @Override
    public Command getCommand() {
        return Command.SUBMIT;
    }
}
