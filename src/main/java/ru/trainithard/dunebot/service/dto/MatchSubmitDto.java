package ru.trainithard.dunebot.service.dto;

import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;

import java.util.List;

public record MatchSubmitDto(long matchId, ModType modType, List<Player> activePlayers) {
}
