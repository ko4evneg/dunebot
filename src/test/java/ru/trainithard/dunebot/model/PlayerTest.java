package ru.trainithard.dunebot.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerTest {
    private final Player player = new Player();

    {
        player.setExternalId(123000L);
        player.setSteamName("st_name");
        player.setFirstName("f_name");
        player.setLastName("l_name");
        player.setExternalName("ex_name");
    }

    @Test
    void shouldReturnMinimalRequiredName() {
        assertThat(player.getFriendlyName()).isEqualTo("f_name (st_name) l_name");
    }

    @Test
    void shouldReturnExternalMentionNameWhenExternalFirstNameMissing() {
        assertThat(player.getMentionTag()).isEqualTo("ex_name");
    }

    @Test
    void shouldReturnExternalMentionNameWhenExternalFirstNamePresented() {
        player.setExternalFirstName("ex_f_name");

        assertThat(player.getMentionTag()).isEqualTo("ex_name");
    }

    @Test
    void shouldReturnExternalFirstNameWhenExternalNameMissing() {
        player.setExternalFirstName("ex_f_name");
        player.setExternalName(null);

        assertThat(player.getMentionTag()).isEqualTo("ex_f_name");
    }
}
