package ru.trainithard.dunebot.service.telegram.command.processor.deprecated;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;

/**
 * Accepts external messaging system photo upload of match results screenshot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Deprecated(since = "0.3.58", forRemoval = true)
public class PhotoUploadCommandProcessor extends CommandProcessor {
    /*    private static final String FILE_DOWNLOAD_URI_PREFIX = "https://api.telegram.org/file/bot";
        private static final String PATH_SEPARATOR = "/";
        private static final String UNSUPPORTED_UPDATE_TYPE = "Unsupported photo/document update type";
        private static final String SUCCESSFUL_UPLOAD_TEXT = "Скриншот успешно загружен.";

        private final RestTemplate restTemplate;
        private final MatchRepository matchRepository;
        private final MatchPlayerRepository matchPlayerRepository;
        private final ScreenshotService screenshotService;
        private final MatchFinishingService matchFinishingService;
        private final KeyboardsFactory keyboardsFactory;

        @Value("${bot.token}")
        private String botToken;*/
    private static final ExternalMessage DEPRECATION_REPLY =
            new ExternalMessage("Функция загрузки скриншота была удалена из бота. Переходите сразу к выбору лидеров!");

    @Override
    public void process(CommandMessage commandMessage) {
        int logId = logId();
        log.debug("{}: PHOTO started", logId);

        messagingService.sendMessageAsync(new MessageDto(commandMessage, DEPRECATION_REPLY, null));
//        List<MatchPlayer> matchPlayersWithOnSubmitMatches =
//                matchPlayerRepository.findByPlayerExternalIdAndMatchState(commandMessage.getUserId(), MatchState.ON_SUBMIT);
//        if (matchPlayersWithOnSubmitMatches.size() > 1) {
//            throw new AnswerableDuneBotException("У вас несколько матчей в состоянии /submit. Вероятно это баг.", commandMessage);
//        }
//        MatchPlayer matchPlayer = matchPlayersWithOnSubmitMatches.get(0);
//        log.debug("{}: matchPlayer {}, match {} found", logId, matchPlayer.getId(), matchPlayer.getMatch().getId());
//
//        String fileId = getFileId(commandMessage);
//        log.debug("{}: file telegram id: {}", logId, fileId);
//        CompletableFuture<TelegramFileDetailsDto> file = messagingService.getFileDetails(fileId);
//        file.whenComplete((telegramFileDetailsDto, throwable) ->
//                processTelegramFile(commandMessage, telegramFileDetailsDto, logId, matchPlayer));

        log.debug("{}: PHOTO ended", logId);
    }
/*

    private void processTelegramFile(CommandMessage commandMessage, TelegramFileDetailsDto telegramFileDetailsDto,
                                     int logId, MatchPlayer matchPlayer) {
        log.debug("{}: file detail callback received from telegram", logId);

        String filePath = telegramFileDetailsDto.path();
        byte[] photoBytes = restTemplate.getForObject(getFileUri(filePath), byte[].class);
        log.debug("{}: file bytes[] received from telegram", logId);

        try {
            if (photoBytes != null) {
                Optional<Match> matchOptional = matchRepository.findWithMatchPlayersBy(matchPlayer.getMatch().getId());
                if (matchOptional.isEmpty()) {
                    return;
                }
                Match match = matchOptional.get();
                saveMatchInfo(filePath, match, photoBytes);
                if (match.canBePreliminaryFinished()) {
                    matchFinishingService.finishSubmittedMatch(match.getId());
                }

                List<List<ButtonDto>> leadersKeyboard = keyboardsFactory.getLeadersKeyboard(matchPlayer);
                MessageDto message = new MessageDto(commandMessage, new ExternalMessage(SUCCESSFUL_UPLOAD_TEXT), leadersKeyboard);
                messagingService.sendMessageAsync(message);
            }
        } catch (ScreenshotFileIOException exception) {
            log.error(logId + ": file save failed due to an exception", exception);
            messagingService.sendMessageAsync(new MessageDto(commandMessage, new ExternalMessage(exception.getMessage()), null));
        } catch (IOException exception) {
            log.error(logId + ": encountered an exception", exception);
        }
    }

    private void saveMatchInfo(String filePath, Match match, byte[] photoBytes) throws IOException {
        String dottedFileExtension = getFileExtension(filePath);
        String savePath = screenshotService.save(match.getId(), dottedFileExtension, photoBytes);
        log.debug("{}: save path: {}", LogId.get(), savePath);
        match.setScreenshotPath(savePath);
        //match.setState(MatchState.ON_SUBMIT_SCREENSHOTTED);
        matchRepository.save(match);
        log.debug("{}: match photo flag saved", LogId.get());
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
        String effectiveFilePath = filePath.startsWith(PATH_SEPARATOR) ? filePath : PATH_SEPARATOR + filePath;
        String fileUri = FILE_DOWNLOAD_URI_PREFIX + botToken + effectiveFilePath;
        log.debug("{}: file uri: {}", logId(), fileUri);
        return fileUri;
    }

    private String getFileExtension(String filePath) {
        int lastSlashIndex = filePath.lastIndexOf(".");
        return filePath.substring(lastSlashIndex);
    }
*/

    @Override
    public Command getCommand() {
        return Command.UPLOAD_PHOTO;
    }
}
