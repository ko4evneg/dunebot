package ru.trainithard.dunebot.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.Leader;
import ru.trainithard.dunebot.model.ModType;

import java.util.List;

public interface LeaderRepository extends JpaRepository<Leader, Long> {
    List<Leader> findAllByModType(ModType modType, Sort sort);
}
