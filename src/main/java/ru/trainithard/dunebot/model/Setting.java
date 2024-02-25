package ru.trainithard.dunebot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "SETTINGS")
@NoArgsConstructor
public class Setting extends BaseEntity {
    private String key;
    private String value;

    public Setting(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
