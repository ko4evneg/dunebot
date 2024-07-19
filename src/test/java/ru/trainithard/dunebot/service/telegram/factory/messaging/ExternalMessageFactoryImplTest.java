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
    void getFinishedPlayersSubmitMessage() {
        Match match = new Match();
        match.setId(12345L);
        List<MatchPlayer> matchPlayers = List.of(
                getMatchPlayer(1, match, "l1", "f1", "s1", "l1"),
                getMatchPlayer(2, match, "l2", "f2", "s2", "l2"),
                getMatchPlayer(3, match, "l3", "f3", "s3", "l3"),
                getMatchPlayer(4, match, "l4", "f4", "s4", "l4")
        );

        ExternalMessage actualMessage = messageFactory.getFinishedPlayersSubmitMessage(matchPlayers);

        assertThat(actualMessage.getText()).isEqualTo("""
                Следующие результаты зарегистрированы для *матча 12345*:
                1: f1 \\(s1\\) l1
                2: f2 \\(s2\\) l2
                3: f3 \\(s3\\) l3
                4: f4 \\(s4\\) l4""");
    }

    @Test
    void getFinishedPlayersSubmitMessage_shouldIgnoreNotParticipatedPlayers() {
        Match match = new Match();
        match.setId(12345L);
        List<MatchPlayer> matchPlayers = List.of(
                getMatchPlayer(1, match, "l1", "f1", "s1", "l1"),
                getMatchPlayer(2, match, "l2", "f2", "s2", "l2"),
                getMatchPlayer(3, match, "l3", "f3", "s3", "l3"),
                getMatchPlayer(4, match, "l4", "f4", "s4", "l4"),
                getMatchPlayer(null, match, "l5", "f5", "s5", "l5")
        );

        ExternalMessage actualMessage = messageFactory.getFinishedPlayersSubmitMessage(matchPlayers);

        assertThat(actualMessage.getText()).isEqualTo("""
                Следующие результаты зарегистрированы для *матча 12345*:
                1: f1 \\(s1\\) l1
                2: f2 \\(s2\\) l2
                3: f3 \\(s3\\) l3
                4: f4 \\(s4\\) l4""");
    }

    @Test
    void getFinishedLeadersSubmitMessage_shouldIgnoreNotParticipatedPlayers() {
        Match match = new Match();
        match.setId(12345L);
        List<MatchPlayer> matchPlayers = List.of(
                getMatchPlayer(1, match, "l1", "f1", "s1", "l1"),
                getMatchPlayer(2, match, "l2", "f2", "s2", "l2"),
                getMatchPlayer(3, match, "l3", "f3", "s3", "l3"),
                getMatchPlayer(4, match, "l4", "f4", "s4", "l4"),
                getMatchPlayer(null, match, "l5", "f5", "s5", "l5")
        );

        ExternalMessage actualMessage = messageFactory.getFinishedLeadersSubmitMessage(matchPlayers);

        assertThat(actualMessage.getText()).isEqualTo("""
                Следующие результаты зарегистрированы для *матча 12345*:
                1: f1 \\(s1\\) l1 \\- l1
                2: f2 \\(s2\\) l2 \\- l2
                3: f3 \\(s3\\) l3 \\- l3
                4: f4 \\(s4\\) l4 \\- l4
                                
                В случае ошибки используйте команду '/resubmit 15000'""");
    }

    @Test
    void matchSuccessfulFinishMessage() {
        Match match = new Match();
        match.setId(12345L);
        match.setMatchPlayers(List.of(
                getMatchPlayer(1, match, "l1", "f1", "s1", "l1"),
                getMatchPlayer(2, match, "l2", "f2", "s2", "l2"),
                getMatchPlayer(3, match, "l3", "f3", "s3", "l3"),
                getMatchPlayer(4, match, "l4", "f4", "s4", "l4")
        ));

        ExternalMessage actualMessage = messageFactory.getMatchSuccessfulFinishMessage(match);

        assertThat(actualMessage.getText())
                .startsWith("*Матч 12345* завершился:\n\n")
                .endsWith("""
                        ⭐️ l1
                                        
                        2️⃣ f2 \\(s2\\) l2
                        ⭐️ l2

                        3️⃣ f3 \\(s3\\) l3
                        ⭐️ l3
                                        
                        4️⃣ f4 \\(s4\\) l4
                        ⭐️ l4""");
        assertThat(actualMessage.getText().split("\n")[2])
                .startsWith("1️⃣ ").contains("f1 \\(s1\\) l1");
    }

    @Test
    void matchSuccessfulFinishMessage_shouldIgnoreNotParticipatedPlayers() {
        Match match = new Match();
        match.setId(12345L);
        match.setMatchPlayers(List.of(
                getMatchPlayer(1, match, "l1", "f1", "s1", "l1"),
                getMatchPlayer(2, match, "l2", "f2", "s2", "l2"),
                getMatchPlayer(3, match, "l3", "f3", "s3", "l3"),
                getMatchPlayer(4, match, "l4", "f4", "s4", "l4"),
                getMatchPlayer(null, match, "l5", "f5", "s5", "l5")
        ));

        ExternalMessage actualMessage = messageFactory.getMatchSuccessfulFinishMessage(match);

        assertThat(actualMessage.getText())
                .startsWith("*Матч 12345* завершился:\n\n")
                .endsWith("""
                        ⭐️ l1
                                        
                        2️⃣ f2 \\(s2\\) l2
                        ⭐️ l2

                        3️⃣ f3 \\(s3\\) l3
                        ⭐️ l3
                                        
                        4️⃣ f4 \\(s4\\) l4
                        ⭐️ l4""");
        assertThat(actualMessage.getText().split("\n")[2])
                .startsWith("1️⃣ ").contains("f1 \\(s1\\) l1");
    }

    @Test
    void getFinishedSubmitParticipantMessage_partipiant() {
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setPlace(2);
        Leader leader = new Leader();
        leader.setName("leader_name");
        matchPlayer.setLeader(leader);
        Match match = new Match();
        match.setId(10000L);
        matchPlayer.setMatch(match);

        ExternalMessage actualMessage = messageFactory
                .getFinishedSubmitParticipantMessage(matchPlayer, "submitter", 11);

        assertThat(actualMessage.getText()).isEqualTo("""
                Игрок *submitter* завершил регистрацию результатов *матча 10000*
                Ознакомьтесь с результатами \\- у вас есть 11 минута чтобы проверить их\\. В случае ошибки, используйте команду '/resubmit 10000'\\.
                                
                За вами зарегистрированы *2 место* и лидер *leader\\_name*\\.""");
    }

    @Test
    void getFinishedSubmitParticipantMessage_nonPartipiant() {
        MatchPlayer matchPlayer = new MatchPlayer();
        Match match = new Match();
        match.setId(10000L);
        matchPlayer.setMatch(match);

        ExternalMessage actualMessage = messageFactory
                .getFinishedSubmitParticipantMessage(matchPlayer, "submitter", 11);

        assertThat(actualMessage.getText()).isEqualTo("""
                Игрок *submitter* завершил регистрацию результатов *матча 10000*
                Ознакомьтесь с результатами \\- у вас есть 11 минута чтобы проверить их\\. В случае ошибки, используйте команду '/resubmit 10000'\\.
                                
                За вами зарегистрировано *неучастие* в матче\\.""");
    }

    private MatchPlayer getMatchPlayer(Integer place, Match match, String leaderName, String... names) {
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
        matchPlayer.setMatch(match);
        return matchPlayer;
    }
}
