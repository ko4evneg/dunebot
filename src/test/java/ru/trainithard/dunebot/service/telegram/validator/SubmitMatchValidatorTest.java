package ru.trainithard.dunebot.service.telegram.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.*;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmitMatchValidatorTest {
    private final SubmitMatchValidator validator = new SubmitMatchValidator();
    private final Match match = new Match();
    private final Message message = new Message();
    private CommandMessage commandMessage;

    @BeforeEach
    void beforeEach() {
        User user = new User();
        user.setId(10000L);
        Chat chat = new Chat();
        chat.setId(10000L);
        chat.setType(ChatType.PRIVATE.getValue());
        message.setFrom(user);
        message.setChat(chat);
        message.setMessageId(10000);
        commandMessage = CommandMessage.getMessageInstance(message);

        Player player1 = new Player();
        player1.setId(10000L);
        player1.setExternalId(10000L);
        MatchPlayer matchPlayer1 = new MatchPlayer();
        matchPlayer1.setPlayer(player1);
        Player player2 = new Player();
        player1.setId(10001L);
        MatchPlayer matchPlayer2 = new MatchPlayer();
        matchPlayer2.setPlayer(player2);
        Player player3 = new Player();
        player3.setId(10002L);
        MatchPlayer matchPlayer3 = new MatchPlayer();
        matchPlayer3.setPlayer(player3);
        Player player4 = new Player();
        player4.setId(10003L);
        MatchPlayer matchPlayer4 = new MatchPlayer();
        matchPlayer4.setPlayer(player4);
        match.setMatchPlayers(List.of(matchPlayer1, matchPlayer2, matchPlayer3, matchPlayer4));
        match.setPositiveAnswersCount(4);
        match.setId(15000L);
        match.setModType(ModType.CLASSIC);
        match.setState(MatchState.NEW);
    }

    @ParameterizedTest
    @MethodSource("submitWrongStates")
    void shouldThrowWhenMatchIsNotEligibleForSubmit(MatchState matchState, String expectedException) {
        match.setState(matchState);

        assertThatThrownBy(() -> validator.validateSubmitMatch(commandMessage, match))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(expectedException);
    }

    private static Stream<Arguments> submitWrongStates() {
        Stream<Arguments> finishedStateTests = MatchState.getEndedMatchStates().stream()
                .map(matchState -> Arguments.of(matchState, "Запрещено регистрировать результаты завершенных матчей"));
        return Stream.concat(finishedStateTests,
                Stream.of(
                        Arguments.of(MatchState.ON_SUBMIT, "Запрос на публикацию этого матча уже сделан"),
                        Arguments.of(MatchState.SUBMITTED, "Результаты матча уже зарегистрированы. " +
                                                           "При ошибке в результатах, используйте команду '/resubmit 15000'")
                ));
    }

    @ParameterizedTest
    @MethodSource("resubmitWrongStates")
    void shouldThrowWhenMatchIsNotEligibleForResubmit(MatchState matchState, String expectedException) {
        match.setState(matchState);

        assertThatThrownBy(() -> validator.validateReSubmitMatch(commandMessage, match))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage(expectedException);
    }

    private static Stream<Arguments> resubmitWrongStates() {
        Stream<Arguments> finishedStateTests = MatchState.getEndedMatchStates().stream()
                .map(matchState -> Arguments.of(matchState, "Запрещено регистрировать результаты завершенных матчей"));
        return Stream.concat(finishedStateTests,
                Stream.of(
                        Arguments.of(MatchState.NEW, "Команда '/resubmit' разрешена только для матчей, уже прошедших регистрацию результатов. " +
                                                     "Для регистрации результатов используйте команду '/submit'"),
                        Arguments.of(MatchState.ON_SUBMIT, "Команда '/resubmit' разрешена только для матчей, уже прошедших регистрацию результатов. " +
                                                           "Для регистрации результатов используйте команду '/submit'")
                ));
    }

    @Test
    void shouldThrowWhenNotEnoughPlayersForSubmit() {
        match.setPositiveAnswersCount(3);

        assertThatThrownBy(() -> validator.validateSubmitMatch(commandMessage, match))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("В опросе участвует меньше игроков чем нужно для матча. Все игроки должны войти в опрос");
    }

    @Test
    void shouldThrowWhenNotEnoughPlayersForReSubmit() {
        match.setPositiveAnswersCount(3);

        assertThatThrownBy(() -> validator.validateReSubmitMatch(commandMessage, match))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("В опросе участвует меньше игроков чем нужно для матча. Все игроки должны войти в опрос");
    }

    @Test
    void shouldThrowWhenAlienMatchSubmitted() {
        message.getFrom().setId(200L);
        message.getChat().setId(200L);
        commandMessage = CommandMessage.getMessageInstance(message);

        assertThatThrownBy(() -> validator.validateSubmitMatch(commandMessage, match))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Вы не можете инициировать публикацию этого матча");
    }

    @Test
    void shouldThrowWhenAlienMatchReSubmitted() {
        message.getFrom().setId(200L);
        message.getChat().setId(200L);
        commandMessage = CommandMessage.getMessageInstance(message);

        assertThatThrownBy(() -> validator.validateReSubmitMatch(commandMessage, match))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Вы не можете инициировать публикацию этого матча");
    }

    @Test
    void shouldNotThrowWhenMatchIsReadyForSubmit() {
        assertThatCode(() -> validator.validateSubmitMatch(commandMessage, match)).doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowWhenMatchIsReadyForReSubmit() {
        match.setState(MatchState.SUBMITTED);

        assertThatCode(() -> validator.validateReSubmitMatch(commandMessage, match)).doesNotThrowAnyException();
    }
}
