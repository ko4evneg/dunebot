package ru.trainithard.dunebot.service.messaging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;

@Getter
@NoArgsConstructor
public class FileMessageDto extends MessageDto {
    private byte[] file;
    private String fileName;

    public FileMessageDto(String chatId, ExternalMessage externalMessage, @Nullable Integer replyMessageId, byte[] file, String fileName) {
        super(chatId, externalMessage, replyMessageId, null);
        this.file = file;
        this.fileName = fileName;
    }
}
