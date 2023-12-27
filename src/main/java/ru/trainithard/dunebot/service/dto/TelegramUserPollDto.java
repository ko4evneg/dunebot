package ru.trainithard.dunebot.service.dto;

public record TelegramUserPollDto(long telegramUserId, String telegramPollId, int positiveAnswersCount) {
}
