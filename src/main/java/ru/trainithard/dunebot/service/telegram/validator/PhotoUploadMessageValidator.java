package ru.trainithard.dunebot.service.telegram.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.CommandType;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Comparator;
import java.util.List;

import static ru.trainithard.dunebot.configuration.SettingConstants.MAX_FILE_SIZE;

@Service
@RequiredArgsConstructor
public class PhotoUploadMessageValidator implements ValidationStrategy {
    private static final String FILE_SIZE_LIMIT_EXCEPTION_MESSAGE_TEMPLATE = "Файл дюже большой. Разрешенный максимальный размер: %s КБ";
    private static final String MULTIPLE_ONSUBMIT_MATCHES_EXCEPTION_MESSAGE_TEMPLATE =
            "У вас более одного матча (%s) в процессе регистрации результата. Выйдите из неактуальных опросов.";
    private static final String NO_ONSUBMIT_MATCHES_EXCEPTION_MESSAGE_TEMPLATE =
            "У вас нет матчей в процессе регистрации результата. Для запуска регистрации выполните команду: '/submit *ID матча*'";

    private final MatchRepository matchRepository;

    @Override
    public void validate(CommandMessage commandMessage) {
        validateFileSize(commandMessage);
        validateOnSubmitMatchesCount(commandMessage);
    }

    private void validateFileSize(CommandMessage commandMessage) {
        if (commandMessage.getPhoto() != null && !hasPhotoBelowSizeLimit(commandMessage) ||
                commandMessage.getFile() != null && commandMessage.getFile().size() > MAX_FILE_SIZE) {
            throw new AnswerableDuneBotException(getFileSizeLimitExceptionMessage(), commandMessage);
        }
    }

    private void validateOnSubmitMatchesCount(CommandMessage commandMessage) {
        List<Match> onSubmitMatches = matchRepository.findLatestPlayerOnSubmitMatch(commandMessage.getUserId());
        if (onSubmitMatches.isEmpty()) {
            throw new AnswerableDuneBotException(NO_ONSUBMIT_MATCHES_EXCEPTION_MESSAGE_TEMPLATE, commandMessage);
        }
        List<String> matchesStrings = onSubmitMatches.stream()
                .sorted(Comparator.comparing(Match::getId))
                .map(match -> match.getId().toString())
                .toList();
        if (onSubmitMatches.size() > 1) {
            String exception = String.format(MULTIPLE_ONSUBMIT_MATCHES_EXCEPTION_MESSAGE_TEMPLATE, String.join(", ", matchesStrings));
            throw new AnswerableDuneBotException(exception, commandMessage);
        }
    }

    private boolean hasPhotoBelowSizeLimit(CommandMessage commandMessage) {
        return commandMessage.getPhoto().stream().anyMatch(photo -> photo.size() <= MAX_FILE_SIZE);
    }

    private String getFileSizeLimitExceptionMessage() {
        int effectiveMaxFileSize = MAX_FILE_SIZE > 1000 ? MAX_FILE_SIZE / 1000 : MAX_FILE_SIZE;
        return String.format(FILE_SIZE_LIMIT_EXCEPTION_MESSAGE_TEMPLATE, effectiveMaxFileSize);
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.FILE_UPLOAD;
    }
}
