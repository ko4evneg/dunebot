package ru.trainithard.dunebot.service.telegram.factory.messaging;

import org.junit.jupiter.api.Test;
import ru.trainithard.dunebot.model.Leader;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalMessageFactoryImplTest {
    private final ExternalMessageFactory messageFactory = new ExternalMessageFactoryImpl();

    @Test
    void matchSuccessfulFinishMessage() {
        Match match = new Match();
        match.setId(12345L);
        match.setMatchPlayers(List.of(
                getMatchPlayer(1, "l1", "f1", "s1", "l1"),
                getMatchPlayer(2, "l2", "f2", "s2", "l2"),
                getMatchPlayer(3, "l3", "f3", "s3", "l3"),
                getMatchPlayer(4, "l4", "f4", "s4", "l4")
        ));

        ExternalMessage actualMessage = messageFactory.getMatchSuccessfulFinishMessage(match);

        assertThat(actualMessage.getText()).isEqualTo("""
                *Матч 12345* завершился:
                                
                1️⃣ 🥳🍾🎉 f1 \\(s1\\) l1 🎉🍾🥳
                ⭐️ l1
                                
                2️⃣ f2 \\(s2\\) l2
                ⭐️ l2

                3️⃣ f3 \\(s3\\) l3
                ⭐️ l3
                                
                4️⃣ f4 \\(s4\\) l4
                ⭐️ l4""");
    }

    private MatchPlayer getMatchPlayer(int place, String leaderName, String... names) {
        Player player = new Player();
        player.setFirstName(names[0]);
        player.setSteamName(names[1]);
        player.setLastName(names[2]);
        Leader leader = new Leader();
        leader.setName(leaderName);
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setPlayer(player);
        matchPlayer.setLeader(leader);
        matchPlayer.setPlace(place);
        return matchPlayer;
    }
}
