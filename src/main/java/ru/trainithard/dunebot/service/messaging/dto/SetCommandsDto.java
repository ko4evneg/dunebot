package ru.trainithard.dunebot.service.messaging.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class SetCommandsDto {
    private final Map<String, String> commandDescriptionsByName;
}
