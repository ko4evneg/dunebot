package ru.trainithard.dunebot.service.telegram.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommandMessageFactoryImpl implements CommandMessageFactory {
    private final MatchRepository matchRepository;

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
