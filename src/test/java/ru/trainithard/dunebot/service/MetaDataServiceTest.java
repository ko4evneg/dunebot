package ru.trainithard.dunebot.service;

import org.junit.jupiter.api.Test;
import ru.trainithard.dunebot.model.MetaData;
import ru.trainithard.dunebot.model.MetaDataKey;
import ru.trainithard.dunebot.repository.MetaDataRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.*;

class MetaDataServiceTest {
    private final MetaDataRepository metaDataRepository = mock(MetaDataRepository.class);
    private final MetaDataService metaDataService = new MetaDataService(metaDataRepository);

    @Test
    void shouldUpdateEarlierMetaData() {
        doReturn(Optional.of(new MetaData(MetaDataKey.PLAYER_RATING_DATE, "01-10-10")))
                .when(metaDataRepository).findByType(MetaDataKey.PLAYER_RATING_DATE);

        metaDataService.saveRatingDate(MetaDataKey.PLAYER_RATING_DATE, LocalDate.of(2010, 9, 9));

        verify(metaDataRepository).save(argThat(metaData -> metaData.getValue().equals("09-09-10")));
    }

    @Test
    void shouldUpdateLaterMetaData() {
        doReturn(Optional.of(new MetaData(MetaDataKey.PLAYER_RATING_DATE, "01-10-10")))
                .when(metaDataRepository).findByType(MetaDataKey.PLAYER_RATING_DATE);

        metaDataService.saveRatingDate(MetaDataKey.PLAYER_RATING_DATE, LocalDate.of(2010, 11, 9));

        verify(metaDataRepository).save(argThat(metaData -> metaData.getValue().equals("09-11-10")));
    }

    @Test
    void shouldSaveMetaData() {
        doReturn(Optional.empty()).when(metaDataRepository).findByType(MetaDataKey.PLAYER_RATING_DATE);

        metaDataService.saveRatingDate(MetaDataKey.PLAYER_RATING_DATE, LocalDate.of(2010, 11, 9));

        verify(metaDataRepository).save(argThat(metaData -> metaData.getValue().equals("09-11-10")));
    }
}
