package ru.trainithard.dunebot.service.messaging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Getter
@NoArgsConstructor
public class FileMessageDto extends MessageDto {
    private byte[] file;
    private String fileName;

    public FileMessageDto(String chatId, String text, @Nullable Integer replyMessageId, byte[] file, String fileName) {
        super(chatId, text, replyMessageId, null);
        this.file = file;
        this.fileName = fileName;
    }
}
