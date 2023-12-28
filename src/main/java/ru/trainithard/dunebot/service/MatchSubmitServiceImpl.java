package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.MatchNotExistsException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.dto.MatchSubmitDto;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchSubmitServiceImpl implements MatchSubmitService {
    private final MatchRepository matchRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public MatchSubmitDto getMatchSubmit(long telegramUserId, long telegramChatId, String matchIdString) {
        try {
            long matchId = Long.parseLong(matchIdString);
            Match match = matchRepository.findById(matchId).orElseThrow(MatchNotExistsException::new);
            validate(telegramChatId, match);

            List<Player> matchActivePlayers = new ArrayList<>();
            boolean isSubmitAllowed = false;
            for (MatchPlayer matchPlayer : match.getMatchPlayers()) {
                if (matchPlayer.getPlayer().getExternalId() == telegramUserId) {
                    isSubmitAllowed = true;
                }
                matchActivePlayers.add(matchPlayer.getPlayer());
            }
            if (!isSubmitAllowed) {
                throw new AnswerableDuneBotException("Вы не можете инициировать публикацию этого матча", telegramChatId);
            }
            return new MatchSubmitDto(match.getId(), match.getModType(), matchActivePlayers);
        } catch (NumberFormatException | MatchNotExistsException exception) {
            throw new AnswerableDuneBotException("Матча с таким ID не существует!", telegramChatId);
        }
    }

    private static void validate(long telegramChatId, Match match) {
        if (match.isFinished()) {
            throw new AnswerableDuneBotException("Запрещено регистрировать результаты завершенных матчей", telegramChatId);
        }
        if (match.getPositiveAnswersCount() < match.getModType().getPlayersCount()) {
            throw new AnswerableDuneBotException("В опросе участвует меньше игроков чем нужно для матча. Все игроки должны войти в опрос", telegramChatId);
        }
        if (match.isOnSubmit()) {
            throw new AnswerableDuneBotException("Запрос на публикацию этого матча уже сделан", telegramChatId);
        }

    }
}
