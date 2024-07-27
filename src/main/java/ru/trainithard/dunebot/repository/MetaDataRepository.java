package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.MetaData;

public interface MetaDataRepository extends JpaRepository<MetaData, Long> {
}
