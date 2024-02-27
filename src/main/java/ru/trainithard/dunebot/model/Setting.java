package ru.trainithard.dunebot.model;

import jakarta.persistence.Entity;
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
    private String key;

    /**
     * Setting value. Should be deserialized based on setting type.
     */
    private String value;

    public Setting(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
