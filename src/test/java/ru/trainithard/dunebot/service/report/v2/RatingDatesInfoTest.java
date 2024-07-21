package ru.trainithard.dunebot.service.report.v2;

import org.junit.jupiter.api.Test;
import ru.trainithard.dunebot.model.RatingDate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RatingDatesInfoTest {
    @Test
    void shouldSetEarliestRatingDateWhenOneEntityPresented() {
        RatingDate ratingDate1 = new RatingDate(10000L, LocalDate.of(2010, 3, 12));
        RatingDate ratingDate2 = new RatingDate(10000L, LocalDate.of(2010, 3, 1));
        RatingDate ratingDate3 = new RatingDate(10000L, LocalDate.of(2010, 2, 10));
        LocalDate earliestDate = LocalDate.of(2010, 1, 30);
        RatingDate ratingDate4 = new RatingDate(10000L, earliestDate);
        List<RatingDate> ratingDates = List.of(ratingDate1, ratingDate2, ratingDate3, ratingDate4);

        RatingDatesInfo ratingDatesInfo = new RatingDatesInfo(ratingDates);

        assertThat(ratingDatesInfo.getEarliestRatingDate()).isEqualTo(earliestDate);
    }

    @Test
    void shouldSetEarliestRatingDateWhenMultipleEntitiesPresented() {
        RatingDate ratingDate1 = new RatingDate(10000L, LocalDate.of(2010, 3, 12));
        RatingDate ratingDate2 = new RatingDate(10000L, LocalDate.of(2010, 3, 1));
        RatingDate ratingDate3 = new RatingDate(10001L, LocalDate.of(2010, 2, 10));
        LocalDate earliestDate = LocalDate.of(2010, 1, 30);
        RatingDate ratingDate4 = new RatingDate(10001L, earliestDate);
        RatingDate ratingDate5 = new RatingDate(10002L, LocalDate.of(2010, 4, 10));
        List<RatingDate> ratingDates = List.of(ratingDate1, ratingDate2, ratingDate3, ratingDate4, ratingDate5);

        RatingDatesInfo ratingDatesInfo = new RatingDatesInfo(ratingDates);

        assertThat(ratingDatesInfo.getEarliestRatingDate()).isEqualTo(earliestDate);
    }

    @Test
    void shouldSetEarliestRatingDateWhenSameDatesPresented() {
        LocalDate earliestDate = LocalDate.of(2010, 1, 30);
        RatingDate ratingDate1 = new RatingDate(10000L, earliestDate);
        RatingDate ratingDate2 = new RatingDate(10001L, earliestDate);
        RatingDate ratingDate3 = new RatingDate(10002L, earliestDate);
        List<RatingDate> ratingDates = List.of(ratingDate1, ratingDate2, ratingDate3);

        RatingDatesInfo ratingDatesInfo = new RatingDatesInfo(ratingDates);

        assertThat(ratingDatesInfo.getEarliestRatingDate()).isEqualTo(earliestDate);
    }

    @Test
    void shouldSetNullEarliestDateWhenEmptyRatingsCollectionProvided() {
        RatingDatesInfo ratingDatesInfo = new RatingDatesInfo(Collections.emptyList());

        assertThat(ratingDatesInfo.getEarliestRatingDate()).isNull();
    }

    @Test
    void shouldReturnLatestRatingDateWhenOneEntityPresented() {
        LocalDate latestDate = LocalDate.of(2010, 3, 12);
        RatingDate ratingDate1 = new RatingDate(10000L, latestDate);
        RatingDate ratingDate2 = new RatingDate(10000L, LocalDate.of(2010, 3, 1));
        RatingDate ratingDate3 = new RatingDate(10000L, LocalDate.of(2010, 2, 10));
        RatingDate ratingDate4 = new RatingDate(10000L, LocalDate.of(2010, 1, 30));
        List<RatingDate> ratingDates = List.of(ratingDate1, ratingDate2, ratingDate3, ratingDate4);

        RatingDatesInfo ratingDatesInfo = new RatingDatesInfo(ratingDates);

        assertThat(ratingDatesInfo.getLatestRatingDate(10000L)).isEqualTo(latestDate);
    }

    @Test
    void shouldReturnLatestRatingDateWhenMultipleEntitiesPresented() {
        LocalDate latestDate10000 = LocalDate.of(2010, 3, 12);
        RatingDate ratingDate1 = new RatingDate(10000L, latestDate10000);
        RatingDate ratingDate2 = new RatingDate(10000L, LocalDate.of(2010, 3, 1));
        LocalDate latestDate10001 = LocalDate.of(2010, 2, 10);
        RatingDate ratingDate3 = new RatingDate(10001L, latestDate10001);
        RatingDate ratingDate4 = new RatingDate(10001L, LocalDate.of(2010, 1, 30));
        LocalDate latestDate10002 = LocalDate.of(2010, 4, 10);
        RatingDate ratingDate5 = new RatingDate(10002L, latestDate10002);
        List<RatingDate> ratingDates = List.of(ratingDate1, ratingDate2, ratingDate3, ratingDate4, ratingDate5);

        RatingDatesInfo ratingDatesInfo = new RatingDatesInfo(ratingDates);

        assertThat(ratingDatesInfo.getLatestRatingDate(10000L)).isEqualTo(latestDate10000);
        assertThat(ratingDatesInfo.getLatestRatingDate(10001L)).isEqualTo(latestDate10001);
        assertThat(ratingDatesInfo.getLatestRatingDate(10002L)).isEqualTo(latestDate10002);
    }

    @Test
    void shouldReturnLatestRatingDateWhenSameDatesPresented() {
        LocalDate latestDate = LocalDate.of(2010, 1, 30);
        RatingDate ratingDate1 = new RatingDate(10000L, latestDate);
        RatingDate ratingDate2 = new RatingDate(10001L, latestDate);
        RatingDate ratingDate3 = new RatingDate(10002L, latestDate);
        RatingDate ratingDate4 = new RatingDate(10002L, latestDate);
        List<RatingDate> ratingDates = List.of(ratingDate1, ratingDate2, ratingDate3, ratingDate4);

        RatingDatesInfo ratingDatesInfo = new RatingDatesInfo(ratingDates);

        assertThat(ratingDatesInfo.getLatestRatingDate(10000L)).isEqualTo(latestDate);
        assertThat(ratingDatesInfo.getLatestRatingDate(10001L)).isEqualTo(latestDate);
        assertThat(ratingDatesInfo.getLatestRatingDate(10002L)).isEqualTo(latestDate);
    }

    @Test
    void shouldReturnNullLatestDateWhenEmptyRatingsCollectionProvided() {
        RatingDatesInfo ratingDatesInfo = new RatingDatesInfo(Collections.emptyList());

        assertThat(ratingDatesInfo.getLatestRatingDate(10000L)).isNull();
    }
}
