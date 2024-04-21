package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.FileMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.messaging.dto.SetCommandsDto;
import ru.trainithard.dunebot.service.report.RatingReportPdf;
import ru.trainithard.dunebot.service.report.RatingReportPdfService;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Process admin commands for bot configuration and management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCommandProcessor extends CommandProcessor {
    private static final String HELP_COMMAND_DESCRIPTION = "Как пользоваться ботом";
    private static final String COMMANDS_LIST_DESCRIPTION = "Показать список доступных команд";
    private static final String HELP_COMMAND_TEXT = "/help";
    private static final String COMMANDS_LIST_COMMAND_TEXT = "/commands";
    private static final String INIT_SUBCOMMAND = "init";
    private static final String SET_CHAT_SUBCOMMAND = "set_chat";
    private static final String SET_TOPIC_CLASSIC = "set_topic_dune";
    private static final String SET_TOPIC_UPRISING4 = "set_topic_up4";
    private static final String WRONG_COMMAND_EXCEPTION_MESSAGE = "Неверная команда!";
    private static final String SUCCESSFUL_COMMAND_TEXT = "Команда успешно выполнена.";
    private static final String SET_KEY = "set";
    private static final String MESSAGE_KEY = "message";
    private static final String REPORT_KEY = "report";
    private static final String WRONG_SETTING_TEXT = "Неверное название настройки!";
    private static final String WRONG_SETTING_VALUE_TEXT = "Значение настройки должно быть числом!";
    private static final DateTimeFormatter DATE_FORAMTTER = DateTimeFormatter.ofPattern("dd.MM.yy");

    private final SettingsService settingsService;
    private final RatingReportPdfService reportService;

    @Value("${bot.admin-pdf-directory}")
    private String adminPdfPath;

    @Override
    public void process(CommandMessage commandMessage) {
        String allCommandArguments = commandMessage.getAllArguments();
        log.debug("{}: ADMIN started. Args: '{}'", logId(), allCommandArguments);

        String subCommand = commandMessage.getArgument(1).toLowerCase();

        MessageDto messageDto = new MessageDto(commandMessage, new ExternalMessage(SUCCESSFUL_COMMAND_TEXT), null);

        switch (subCommand) {
            case INIT_SUBCOMMAND -> sendSetCommands();
            case SET_CHAT_SUBCOMMAND ->
                    settingsService.saveSetting(SettingKey.CHAT_ID, Long.toString(commandMessage.getChatId()));
            case SET_TOPIC_CLASSIC ->
                    settingsService.saveSetting(SettingKey.TOPIC_ID_CLASSIC, commandMessage.getReplyMessageId().toString());
            case SET_TOPIC_UPRISING4 ->
                    settingsService.saveSetting(SettingKey.TOPIC_ID_UPRISING, commandMessage.getReplyMessageId().toString());
            case SET_KEY -> setCustomKeySetting(commandMessage);
            case MESSAGE_KEY -> sendTopicsMessages(allCommandArguments);
            case REPORT_KEY -> generateReport(commandMessage);
            default -> {
                log.debug("{}: wrong admin subcommand {}", logId(), subCommand);
                messageDto = new MessageDto(commandMessage, new ExternalMessage(WRONG_COMMAND_EXCEPTION_MESSAGE), null);
            }
        }

        messagingService.sendMessageAsync(messageDto);
        log.debug("{}: ADMIN ended", logId());
    }

    private void sendSetCommands() {
        Map<String, String> commands = Map.of(HELP_COMMAND_TEXT, HELP_COMMAND_DESCRIPTION,
                COMMANDS_LIST_COMMAND_TEXT, COMMANDS_LIST_DESCRIPTION);
        messagingService.sendSetCommands(new SetCommandsDto(commands));
    }

    private void setCustomKeySetting(CommandMessage commandMessage) {
        String settingName = commandMessage.getArgument(2);
        SettingKey settingKey = SettingKey.getByName(settingName);
        if (settingKey == null || settingKey == SettingKey.ADMIN_USER_ID) {
            throw new AnswerableDuneBotException(WRONG_SETTING_TEXT, commandMessage);
        }
        String settingValue = commandMessage.getArgument(3);
        try {
            Integer.parseInt(settingValue);
        } catch (NumberFormatException exception) {
            throw new AnswerableDuneBotException(WRONG_SETTING_VALUE_TEXT, exception, commandMessage);
        }
        settingsService.saveSetting(settingKey, settingValue);
    }

    private void sendTopicsMessages(String allCommandArguments) {
        String chatId = settingsService.getStringSetting(SettingKey.CHAT_ID);
        int up4Topic = settingsService.getIntSetting(SettingKey.TOPIC_ID_UPRISING);
        int duneTopic = settingsService.getIntSetting(SettingKey.TOPIC_ID_CLASSIC);
        String message = allCommandArguments.substring(MESSAGE_KEY.length() + 1);
        ExternalMessage userMessage = new ExternalMessage(message);
        MessageDto up4UserMessageDto = new MessageDto(chatId, userMessage, up4Topic, null);
        messagingService.sendMessageAsync(up4UserMessageDto);
        if (up4Topic != duneTopic) {
            MessageDto duneUserMessageDto = new MessageDto(chatId, userMessage, duneTopic, null);
            messagingService.sendMessageAsync(duneUserMessageDto);
        }
    }

    private void generateReport(CommandMessage commandMessage) {
        String[] arguments = commandMessage.getAllArguments().split("\\s+");
        try {
            LocalDate from = LocalDate.parse(arguments[1], DATE_FORAMTTER);
            LocalDate to = LocalDate.parse(arguments[2], DATE_FORAMTTER);
            String ratingName = String.format("РЕЙТИНГ %s - %s", arguments[1], arguments[2]);

            RatingReportPdf classicRating = reportService.createRating(from, to, ModType.CLASSIC, ratingName);
            byte[] classicRatingContent = classicRating.getPdfBytes();
            String classicFileName = getPdfFileName(from, to, ModType.CLASSIC);
            saveRating(classicRatingContent, classicFileName);
            sendRating(ModType.CLASSIC, commandMessage, classicRatingContent, classicFileName);

            RatingReportPdf uprisingRating = reportService.createRating(from, to, ModType.UPRISING_4, ratingName);
            byte[] uprisingRatingContent = uprisingRating.getPdfBytes();
            String uprisingFileName = getPdfFileName(from, to, ModType.UPRISING_4);
            saveRating(uprisingRatingContent, uprisingFileName);
            sendRating(ModType.UPRISING_4, commandMessage, uprisingRatingContent, uprisingFileName);
        } catch (Exception exception) {
            throw new AnswerableDuneBotException("Ошибка генерации отчета", exception, commandMessage);
        }
    }

    private void saveRating(byte[] ratingPdfBytes, String fileName) throws IOException {
        Path savingPath = Path.of(adminPdfPath);
        Files.createDirectories(savingPath);
        Files.write(savingPath.resolve(fileName), ratingPdfBytes);
    }

    private void sendRating(ModType uprising4, CommandMessage commandMessage, byte[] classicRatingContent, String classicFileName) {
        String chatId = Long.toString(commandMessage.getChatId());
        Integer topicId = commandMessage.getTopicId();
        String uprisingMessageText = uprising4.getAlias();
        FileMessageDto uprisingFileMessageDto =
                new FileMessageDto(chatId, new ExternalMessage(uprisingMessageText), topicId, classicRatingContent, classicFileName);
        messagingService.sendFileAsync(uprisingFileMessageDto);
    }

    private String getPdfFileName(LocalDate from, LocalDate to, ModType modType) {
        return String.format("%s_%s_%s.pdf", modType.getAlias(), from.format(DATE_FORAMTTER), to.format(DATE_FORAMTTER));
    }

    @Override
    public Command getCommand() {
        return Command.ADMIN;
    }
}
