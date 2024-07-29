package ru.trainithard.dunebot.util;

import org.junit.jupiter.api.Test;
import ru.trainithard.dunebot.exception.DuneBotException;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.PlayerRating;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingCloseEntitiesUtilTest {
    private static final LocalDate DATE = LocalDate.of(2010, 10, 10);
    private final Player player1 = getPlayer(1);
    private final Player player2 = getPlayer(2);
    private final Player player3 = getPlayer(3);
    private final Player player4 = getPlayer(4);
    private final Player player5 = getPlayer(5);
    private final Player player6 = getPlayer(6);
    private final Player player7 = getPlayer(7);
    private final Player player8 = getPlayer(8);
    private final Player player9 = getPlayer(9);
    private final Player player10 = getPlayer(10);
    private final PlayerRating rating1 = new PlayerRating(player1, DATE);
    private final PlayerRating rating2 = new PlayerRating(player2, DATE);
    private final PlayerRating rating3 = new PlayerRating(player3, DATE);
    private final PlayerRating rating4 = new PlayerRating(player4, DATE);
    private final PlayerRating rating5 = new PlayerRating(player5, DATE);
    private final PlayerRating rating6 = new PlayerRating(player6, DATE);
    private final PlayerRating rating7 = new PlayerRating(player7, DATE);
    private final PlayerRating rating8 = new PlayerRating(player8, DATE);
    private final PlayerRating rating9 = new PlayerRating(player9, DATE);
    private final PlayerRating rating10 = new PlayerRating(player10, DATE);
    private final List<PlayerRating> ratings =
            List.of(rating1, rating2, rating3, rating4, rating5, rating6, rating7, rating8, rating9, rating10);

    @Test
    void shouldReturnAllPlayersWhenPlayerInTheMidstOfList() {
        List<PlayerRating> actualResult = RatingCloseEntitiesUtil.getClosestEntitiesList(ratings, 4, 5);

        assertThat(actualResult)
                .containsExactly(rating3, rating4, rating5, rating6, rating7);
    }

    @Test
    void shouldReturnRequiredSizePlayersWhenPlayerInTheMidstOfList() {
        List<PlayerRating> actualResult = RatingCloseEntitiesUtil.getClosestEntitiesList(ratings, 4, 7);

        assertThat(actualResult)
                .containsExactly(rating2, rating3, rating4, rating5, rating6, rating7, rating8);
    }

    @Test
    void shouldReturnAllPlayersFromStartWhenNotEnoughPlayersBeforeIndex() {
        List<PlayerRating> actualResult = RatingCloseEntitiesUtil.getClosestEntitiesList(ratings, 0, 5);

        assertThat(actualResult)
                .containsExactly(rating1, rating2, rating3, rating4, rating5);
    }

    @Test
    void shouldReturnAllPlayersFromEndWhenNotEnoughPlayersAfterIndex() {
        List<PlayerRating> actualResult = RatingCloseEntitiesUtil.getClosestEntitiesList(ratings, 9, 5);

        assertThat(actualResult)
                .containsExactly(rating6, rating7, rating8, rating9, rating10);
    }

    @Test
    void shouldTrowOnEvenSelectionSize() {
        assertThatThrownBy(() -> RatingCloseEntitiesUtil.getClosestEntitiesList(ratings, 5, 6))
                .isInstanceOf(DuneBotException.class)
                .hasMessage("Even selection size prohibited");
    }

    private Player getPlayer(int id) {
        Player player = new Player();
        player.setId((long) id);
        return player;
    }
}
