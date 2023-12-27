package ru.trainithard.dunebot.service.dto;

import ru.trainithard.dunebot.model.ModType;

import java.util.List;

public record MatchSubmitDto(long matchId, ModType modType, List<Long> telegramPlayerIds) {
}
