package ru.trainithard.dunebot.service.telegram.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.LogId;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandMessageFactoryImpl implements CommandMessageFactory {
    private final MatchRepository matchRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public CommandMessage getInstance(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (hasSlashPrefixedText(message) || hasAttachedPhoto(message)) {
                return CommandMessage.getMessageInstance(message);
            }
        } else if (hasPollAnswerOption(update)) {
            String pollId = update.getPollAnswer().getPollId();
            Optional<Match> matchOptional = matchRepository.findByExternalPollIdPollId(pollId);
            if (matchOptional.isPresent()) {
                return CommandMessage.getPollAnswerInstance(update.getPollAnswer());
            }
        } else if (hasNotBlankCallbackQuery(update)) {
            //TODO: add validation for callback owner (to avoid excessive processing when multiple bots in channel exist)
            return CommandMessage.getCallbackInstance(update.getCallbackQuery());
        }
        //TODO: remove
        try {
            log.debug("{}: null command detected: {}", LogId.get(), mapper.writeValueAsString(update));
        } catch (JsonProcessingException e) {
            log.error(LogId.get() + ": can't serialize command", e);
        }
        return null;
    }

    private boolean hasSlashPrefixedText(Message message) {
        return message.getText() != null && message.getText().startsWith("/");
    }

    private boolean hasPollAnswerOption(Update update) {
        return update.hasPollAnswer();
    }

    private boolean hasNotBlankCallbackQuery(Update update) {
        return update.hasCallbackQuery() && update.getCallbackQuery().getData() != null && !update.getCallbackQuery().getData().isBlank();
    }

    private boolean hasAttachedPhoto(Message message) {
        return message.hasDocument() || message.hasPhoto();
    }
}
