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
    private static final String BOT_START_DATE = "01.05.24";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy");

    private final MetaDataRepository metaDataRepository;

    public LocalDate findRatingDate(MetaDataKey metaDataKey) {
        MetaData playerRatingDateMetaData = metaDataRepository.findByType(metaDataKey).orElse(new MetaData(metaDataKey, BOT_START_DATE));
        TemporalAccessor parsedDate = DATE_TIME_FORMATTER.parse(playerRatingDateMetaData.getValue());
        return LocalDate.from(parsedDate);
    }

    public void saveOnlyLatestRatingDate(MetaDataKey metaDataKey, LocalDate date) {
        String dateString = date.format(DATE_TIME_FORMATTER);
        MetaData curentMetaData = metaDataRepository.findByType(metaDataKey).orElse(new MetaData(metaDataKey, dateString));
        LocalDate currentSavedDate = LocalDate.from(DATE_TIME_FORMATTER.parse(curentMetaData.getValue()));
        if (currentSavedDate.isBefore(date)) {
            curentMetaData.setValue(date.format(DATE_TIME_FORMATTER));
        }
        metaDataRepository.save(curentMetaData);
    }
}
