package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.MetaData;
import ru.trainithard.dunebot.model.MetaDataKey;

import java.util.Optional;

public interface MetaDataRepository extends JpaRepository<MetaData, Long> {
    Optional<MetaData> findByType(MetaDataKey type);
}
