package ru.trainithard.dunebot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bot configuration setting
 */
@Getter
@Setter
@Entity
@Table(name = "SETTINGS")
@NoArgsConstructor
public class Setting extends BaseEntity {
    /**
     * Setting name
     */
    @Enumerated(EnumType.STRING)
    private SettingKey key;

    /**
     * Setting value. Should be deserialized based on setting type.
     */
    private String value;

    public Setting(SettingKey key, String value) {
        this.key = key;
        this.value = value;
    }
}
