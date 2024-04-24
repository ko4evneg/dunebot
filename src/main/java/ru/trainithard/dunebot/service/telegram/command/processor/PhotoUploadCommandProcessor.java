package ru.trainithard.dunebot.service.telegram.command.processor;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.trainithard.dunebot.exception.DuneBotException;
import ru.trainithard.dunebot.exception.ScreenshotFileIOException;
import ru.trainithard.dunebot.model.Leader;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.LeaderRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.ScreenshotService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.messaging.dto.TelegramFileDetailsDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ru.trainithard.dunebot.configuration.SettingConstants.MAX_SCREENSHOT_SIZE;

/**
 * Accepts external messaging system photo upload of match results screenshot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoUploadCommandProcessor extends CommandProcessor {
    private static final String FILE_DOWNLOAD_URI_PREFIX = "https://api.telegram.org/file/bot";
    private static final String PATH_SEPARATOR = "/";
    private static final String UNSUPPORTED_UPDATE_TYPE = "Unsupported photo/document update type";
    private static final String SUCCESSFUL_UPLOAD_TEXT = "Скриншот успешно загружен.";

    private final RestTemplate restTemplate;
    private final MatchRepository matchRepository;
    private final ScreenshotService screenshotService;
    private final MatchFinishingService matchFinishingService;
    private final LeaderRepository leaderRepository;

    @Value("${bot.token}")
    private String botToken;

    @Override
    public void process(CommandMessage commandMessage) {
        int logId = logId();
        log.debug("{}: PHOTO started", logId);

        Match match = matchRepository.findLatestPlayerMatchWithMatchPlayerBy(commandMessage.getUserId(), MatchState.ON_SUBMIT)
                .iterator().next();
        log.debug("{}: match {} found", logId, match.getId());

        String fileId = getFileId(commandMessage);
        log.debug("{}: file telegram id: {}", logId, fileId);
        CompletableFuture<TelegramFileDetailsDto> file = messagingService.getFileDetails(fileId);
        file.whenComplete((telegramFileDetailsDto, throwable) ->
                processTelegramFile(commandMessage, telegramFileDetailsDto, logId, match));

        log.debug("{}: PHOTO ended", logId);
    }

    private void processTelegramFile(CommandMessage commandMessage, TelegramFileDetailsDto telegramFileDetailsDto, int logId, Match match) {
        log.debug("{}: file detail callback received from telegram", logId);

        String filePath = telegramFileDetailsDto.path();
        String effectiveFilePath = filePath.startsWith(PATH_SEPARATOR) ? filePath : PATH_SEPARATOR + filePath;
        byte[] photoBytes = restTemplate.getForObject(getFileUri(effectiveFilePath), byte[].class);
        log.debug("{}: file bytes[] received from telegram", logId);

        try {
            if (photoBytes != null) {
                String dottedFileExtension = getFileExtension(filePath);
                String savePath = screenshotService.save(match.getId(), dottedFileExtension, photoBytes);
                log.debug("{}: save path: {}", logId, savePath);
                match.setScreenshotPath(savePath);
                match.setState(MatchState.ON_SUBMIT_SCREENSHOTTED);
                matchRepository.save(match);
                log.debug("{}: match photo flag saved", logId);
                if (match.canBeFinished()) {
                    matchFinishingService.finishSubmittedMatch(match.getId());
                }

                messagingService.sendMessageAsync(new MessageDto(commandMessage, new ExternalMessage(SUCCESSFUL_UPLOAD_TEXT), getLeadersKeyboard(match)));
            }
        } catch (ScreenshotFileIOException exception) {
            log.error(logId + ": file save failed due to an exception", exception);
            messagingService.sendMessageAsync(new MessageDto(commandMessage, new ExternalMessage(exception.getMessage()), null));
        } catch (IOException exception) {
            log.error(logId + ": encountered an exception", exception);
        }
    }

    private String getFileId(CommandMessage commandMessage) {
        if (commandMessage.getPhoto() != null) {
            CommandMessage.Photo largestPhoto = commandMessage.getPhoto().stream()
                    .filter(photo -> photo.size() <= MAX_SCREENSHOT_SIZE)
                    .max(Comparator.comparing(CommandMessage.Photo::size))
                    .orElseThrow();
            return largestPhoto.id();
        } else if (commandMessage.getFile() != null) {
            return commandMessage.getFile().id();
        }
        throw new DuneBotException(UNSUPPORTED_UPDATE_TYPE);
    }

    private String getFileUri(String filePath) {
        String fileUri = FILE_DOWNLOAD_URI_PREFIX + botToken + filePath;
        log.debug("{}: file uri: {}", logId(), fileUri);
        return fileUri;
    }

    private String getFileExtension(String filePath) {
        int lastSlashIndex = filePath.lastIndexOf(".");
        return filePath.substring(lastSlashIndex);
    }

    private List<List<ButtonDto>> getLeadersKeyboard(Match match) {
        String callbackPrefix = match.getId() + "_L_";
        List<Leader> leaders = leaderRepository.findAllByModType(match.getModType(), Sort.sort(Leader.class).by(Leader::getName));
        List<ButtonDto> leadersButtons = leaders.stream()
                .map(leader -> new ButtonDto(leader.getName(), callbackPrefix + leader.getId()))
                .toList();
        return Lists.partition(leadersButtons, 3);
    }

    @Override
    public Command getCommand() {
        return Command.UPLOAD_PHOTO;
    }
}
