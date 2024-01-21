package ru.trainithard.dunebot.service.messaging.dto;

import org.telegram.telegrambots.meta.api.objects.File;

public record TelegramFileDetailsDto(String id, String path, Long size) {
    public TelegramFileDetailsDto(File messageFile) {
        this(messageFile.getFileId(), messageFile.getFilePath(), messageFile.getFileSize());
    }
}
