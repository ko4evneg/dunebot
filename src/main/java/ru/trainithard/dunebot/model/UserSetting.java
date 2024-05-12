package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bot configuration setting
 */
@Getter
@Setter
@Entity
@Table(name = "USER_SETTINGS")
@NoArgsConstructor
public class UserSetting extends BaseEntity {
    /**
     * Setting's owner.
     */
    @OneToOne
    @JoinColumn(name = "PLAYER_ID")
    Player player;
    /**
     * Setting name.
     */
    @Enumerated(EnumType.STRING)
    private UserSettingKey key;

    /**
     * Setting value. Should be deserialized based on setting type.
     */
    private String value;

    public UserSetting(Player owner, UserSettingKey key, String value) {
        this.player = owner;
        this.key = key;
        this.value = value;
    }
}
