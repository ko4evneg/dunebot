package ru.trainithard.dunebot.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerTest {
    private final Player player = new Player();

    {
        player.setSteamName("st_name");
        player.setFirstName("f_name");
        player.setLastName("l_name");
        player.setExternalName("ex_name");
    }

    @Test
    void shouldReturnMinimalRequiredName() {
        assertEquals("f_name (st_name) l_name", player.getFriendlyName());
    }

    @Test
    void shouldReturnMentionWithoutWhenExternalFirstNameMissing() {
        assertEquals("@ex_name", player.getMention());
    }

    @Test
    void shouldReturnMentionWithExternalNameWhenExternalFirstNamePresented() {
        player.setExternalFirstName("ex_f_name");

        assertEquals("@ex_name", player.getMention());
    }

    @Test
    void shouldReturnMentionWithExternalFirstNameWhenExternalNameMissing() {
        player.setExternalFirstName("ex_f_name");
        player.setExternalName(null);

        assertEquals("@ex_f_name", player.getMention());
    }
}
