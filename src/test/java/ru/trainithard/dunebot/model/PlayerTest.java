package ru.trainithard.dunebot.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerTest {
    private final Player player = new Player();

    {
        player.setSteamName("st_name");
        player.setFirstName("f_name");
    }

    @Test
    void shouldReturnMinimalRequiredName() {
        assertEquals("st_name (f_name)", player.getFriendlyName());
    }

    @Test
    void shouldReturnNameWithLastName() {
        player.setLastName("l_name");

        assertEquals("st_name (f_name l_name)", player.getFriendlyName());
    }
}
