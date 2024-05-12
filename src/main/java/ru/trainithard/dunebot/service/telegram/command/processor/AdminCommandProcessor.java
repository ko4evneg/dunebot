package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.AppSettingsService;
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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Process admin commands for bot configuration and management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCommandProcessor extends CommandProcessor {
    private static final String INIT_SUBCOMMAND = "init";
    private static final String SHUTDOWN_SUBCOMMAND = "shutdown";
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

    private final AppSettingsService appSettingsService;
    private final RatingReportPdfService reportService;
    private final DuneBotTaskScheduler taskScheduler;
    private final Clock clock;
    private final ApplicationContext applicationContext;

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
                    appSettingsService.saveSetting(AppSettingKey.CHAT_ID, Long.toString(commandMessage.getChatId()));
            case SET_TOPIC_CLASSIC ->
                    appSettingsService.saveSetting(AppSettingKey.TOPIC_ID_CLASSIC, commandMessage.getReplyMessageId().toString());
            case SET_TOPIC_UPRISING4 ->
                    appSettingsService.saveSetting(AppSettingKey.TOPIC_ID_UPRISING, commandMessage.getReplyMessageId().toString());
            case SET_KEY -> setCustomKeySetting(commandMessage);
            case MESSAGE_KEY -> {
                String messageText = allCommandArguments.substring(MESSAGE_KEY.length() + 1);
                sendTopicsMessages(messageText);
            }
            case REPORT_KEY -> generateReport(commandMessage);
            case SHUTDOWN_SUBCOMMAND -> shutdown(commandMessage);
            default -> {
                log.debug("{}: wrong admin subcommand {}", logId(), subCommand);
                messageDto = new MessageDto(commandMessage, new ExternalMessage(WRONG_COMMAND_EXCEPTION_MESSAGE), null);
            }
        }

        messagingService.sendMessageAsync(messageDto);
        log.debug("{}: ADMIN ended", logId());
    }

    private void sendSetCommands() {
        Map<String, String> commands = Map.of(
                "/help", "Как пользоваться ботом",
                "/new_dune", "Создание новой партии в классическую Дюну",
                "/new_up4", "Создание новой партии в Uprising на 4-х игроков",
                "/new_up6", "Создание новой партии в Uprising на 6-х игроков",
                "/cancel", "Отмена *вашего* последнего незавершенного матча"
        );
        messagingService.sendSetCommands(new SetCommandsDto(commands));
    }

    private void setCustomKeySetting(CommandMessage commandMessage) {
        String settingName = commandMessage.getArgument(2);
        AppSettingKey appSettingKey = AppSettingKey.getByName(settingName);
        if (appSettingKey == null || appSettingKey == AppSettingKey.ADMIN_USER_ID) {
            throw new AnswerableDuneBotException(WRONG_SETTING_TEXT, commandMessage);
        }
        String settingValue = commandMessage.getArgument(3);
        try {
            Integer.parseInt(settingValue);
        } catch (NumberFormatException exception) {
            throw new AnswerableDuneBotException(WRONG_SETTING_VALUE_TEXT, exception, commandMessage);
        }
        appSettingsService.saveSetting(appSettingKey, settingValue);
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

    private void shutdown(CommandMessage commandMessage) {
        String delayArg = commandMessage.getArgument(2);
        DuneTaskId shutdownTaskId = new DuneTaskId(DuneTaskType.SHUTDOWN);
        if ("cancel".equalsIgnoreCase(delayArg)) {
            taskScheduler.cancel(shutdownTaskId);
            sendTopicsMessages("❎ Перезагрузка бота отменена.");
            return;
        }

        try {
            int delay = Integer.parseInt(delayArg.trim());
            taskScheduler.reschedule(() -> {
                        sendTopicsMessages("ℹ️ Бот перезагружается.");
                        SpringApplication.exit(applicationContext, () -> 0);
                    },
                    shutdownTaskId, Instant.now(clock).plus(delay, ChronoUnit.MINUTES));
            sendTopicsMessages("⚠️ Бот будет перезагружен через " + delay + " минут.\n" +
                               "‼️ Все незавершенные матчи будут принудительно завершены без зачета в рейтинг.");
        } catch (NumberFormatException e) {
            throw new AnswerableDuneBotException("Неверный аргумент", commandMessage);
        }
    }

    private void sendTopicsMessages(String message) {
        String chatId = appSettingsService.getStringSetting(AppSettingKey.CHAT_ID);
        int up4Topic = appSettingsService.getIntSetting(AppSettingKey.TOPIC_ID_UPRISING);
        int duneTopic = appSettingsService.getIntSetting(AppSettingKey.TOPIC_ID_CLASSIC);
        ExternalMessage userMessage = new ExternalMessage(message);
        MessageDto up4UserMessageDto = new MessageDto(chatId, userMessage, up4Topic, null);
        messagingService.sendMessageAsync(up4UserMessageDto);
        if (up4Topic != duneTopic) {
            MessageDto duneUserMessageDto = new MessageDto(chatId, userMessage, duneTopic, null);
            messagingService.sendMessageAsync(duneUserMessageDto);
        }
    }

    @Override
    public Command getCommand() {
        return Command.ADMIN;
    }
}
