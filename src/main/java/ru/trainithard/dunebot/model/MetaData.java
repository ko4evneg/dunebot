package ru.trainithard.dunebot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "META_DATA")
@NoArgsConstructor
@AllArgsConstructor
public class MetaData extends BaseEntity{
    @Enumerated(EnumType.STRING)
    private MetaDataKey type;
    private String value;
}
