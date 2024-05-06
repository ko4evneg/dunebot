package ru.trainithard.dunebot.service;

import ru.trainithard.dunebot.model.ModType;

public interface MatchCreationService {
    void createMatch(long creatorExternalId, ModType modType);
}
