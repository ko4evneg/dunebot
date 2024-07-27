package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.MetaData;
import ru.trainithard.dunebot.model.MetaDataKey;
import ru.trainithard.dunebot.repository.MetaDataRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

@Service
@RequiredArgsConstructor
public class MetaDataService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy");

    private final MetaDataRepository metaDataRepository;

    public LocalDate findRatingDate(MetaDataKey metaDataKey) {
        MetaData playerRatingDateMetaData = metaDataRepository.findByType(metaDataKey).orElseThrow();
        TemporalAccessor parsedDate = DATE_TIME_FORMATTER.parse(playerRatingDateMetaData.getValue());
        return LocalDate.from(parsedDate);
    }

    public void saveRatingDate(MetaDataKey metaDataKey, LocalDate date) {
        String dateString = date.format(DATE_TIME_FORMATTER);
        MetaData metaData = metaDataRepository.findByType(metaDataKey).orElse(new MetaData(metaDataKey, dateString));
        metaDataRepository.save(metaData);
    }
}
