package ru.trainithard.dunebot.service.report.v2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.PlayerRating;
import ru.trainithard.dunebot.service.AppSettingsService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class RatingStatsComparatorTest {
    private static final LocalDate RATING_DATE = LocalDate.of(2010, 10, 10);
    private final AppSettingsService appSettingsService = mock(AppSettingsService.class);
    private final RatingStatsComparator ratingStatsComparator = new RatingStatsComparator(appSettingsService);
    private final Player left = new Player();
    private final Player right = new Player();
    private final PlayerRating leftRating = new PlayerRating(left, RATING_DATE);
    private final PlayerRating rightRating = new PlayerRating(right, RATING_DATE);

    @BeforeEach
    void beforeEach() {
        doReturn(20).when(appSettingsService).getIntSetting(AppSettingKey.MONTHLY_MATCHES_THRESHOLD);
    }

    //TODO REVERSE NAMES LEFT -> RIGHT, TESTS ARE CORRECT
    @Test
    void shouldReturnPositiveWhenLeftExceededThresholdAndRightNotExceeded() {
        leftRating.setMatchesCount(20);
        leftRating.setEfficiency(1.0);
        rightRating.setMatchesCount(19);
        rightRating.setEfficiency(100.0);

        int actualComparison = ratingStatsComparator.compare(rightRating, leftRating);

        assertThat(actualComparison).isPositive();
    }

    @Test
    void shouldReturnPositiveWhenRightExceededThresholdAndLeftNotExceeded() {
        rightRating.setMatchesCount(20);
        rightRating.setEfficiency(1.0);
        leftRating.setMatchesCount(19);
        leftRating.setEfficiency(100.0);

        int actualComparison = ratingStatsComparator.compare(rightRating, leftRating);

        assertThat(actualComparison).isNegative();
    }

    @Test
    void shouldReturnPositiveWhenLeftEfficiencyMore() {
        leftRating.setMatchesCount(20);
        leftRating.setEfficiency(100.0);
        rightRating.setMatchesCount(20);
        rightRating.setEfficiency(1.0);

        int actualComparison = ratingStatsComparator.compare(rightRating, leftRating);

        assertThat(actualComparison).isPositive();
    }

    @Test
    void shouldReturnPositiveWhenRightEfficiencyMore() {
        leftRating.setMatchesCount(20);
        leftRating.setEfficiency(1.0);
        rightRating.setMatchesCount(20);
        rightRating.setEfficiency(100.0);

        int actualComparison = ratingStatsComparator.compare(rightRating, leftRating);

        assertThat(actualComparison).isNegative();
    }

    @Test
    void shouldReturnZeroWhenEfficiencyIsEqual() {
        leftRating.setMatchesCount(20);
        leftRating.setEfficiency(5.0);
        rightRating.setMatchesCount(20);
        rightRating.setEfficiency(5.0);

        int actualComparison = ratingStatsComparator.compare(rightRating, leftRating);

        assertThat(actualComparison).isZero();
    }
}
