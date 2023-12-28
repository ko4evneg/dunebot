package ru.trainithard.dunebot.service.messaging.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ButtonDto {
    private final String text;
    private final String callback;
}
