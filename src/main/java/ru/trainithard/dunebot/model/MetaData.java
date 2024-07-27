package ru.trainithard.dunebot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "META_DATA")
@NoArgsConstructor
public class MetaData extends BaseEntity{
    private String type;
    private String value;
}
