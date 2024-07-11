package ru.trainithard.dunebot.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MatchTest {
    private final Match match = new Match();

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void shouldReturnResubmitAllowedTrueWhenSubmitsAttemptsNotExceedsLimits(int resubmitsDone) {
        match.setSubmitsRetryCount(resubmitsDone);

        boolean actualIsResubmitAllowed = match.isResubmitAllowed(5);

        assertThat(actualIsResubmitAllowed).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 6, 7})
    void shouldReturnResubmitAllowedFalseWhenSubmitsAttemptsNotExceedsLimits(int resubmitsDone) {
        match.setSubmitsRetryCount(resubmitsDone);

        boolean actualIsResubmitAllowed = match.isResubmitAllowed(5);

        assertThat(actualIsResubmitAllowed).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"CLASSIC, 4", "UPRISING_4, 4", "UPRISING_6, 6"})
    void shouldReturnIsReadyToStartTrueWhenPositiveVotesEqualsModTypeSizeExactly(ModType modType, int positiveVotes) {
        match.setModType(modType);
        match.setPositiveAnswersCount(positiveVotes);

        boolean actualIsReadyToStart = match.isReadyToStart();

        assertThat(actualIsReadyToStart).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"CLASSIC, 3", "CLASSIC, 5", "UPRISING_4, 3", "UPRISING_4, 5", "UPRISING_6, 5", "UPRISING_6, 7"})
    void shouldReturnIsReadyToStartFalseWhenPositiveVotesNotEqualsModTypeSizeExactly(ModType modType, int positiveVotes) {
        match.setModType(modType);
        match.setPositiveAnswersCount(positiveVotes);

        boolean actualIsReadyToStart = match.isReadyToStart();

        assertThat(actualIsReadyToStart).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"CLASSIC, 3", "UPRISING_4, 3", "UPRISING_6, 5"})
    void shouldReturnHasMissingPlayersTrueWhenPositiveVotesLessThanModTypeSize(ModType modType, int positiveVotes) {
        match.setModType(modType);
        match.setPositiveAnswersCount(positiveVotes);

        boolean actualIsReadyToStart = match.hasMissingPlayers();

        assertThat(actualIsReadyToStart).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"CLASSIC, 4", "CLASSIC, 5", "UPRISING_4, 4", "UPRISING_4, 5", "UPRISING_6, 6", "UPRISING_6, 7"})
    void shouldReturnHasMissingPlayersFalseWhenPositiveVotesMoreThanOrEqualsModTypeSize(ModType modType, int positiveVotes) {
        match.setModType(modType);
        match.setPositiveAnswersCount(positiveVotes);

        boolean actualIsReadyToStart = match.hasMissingPlayers();

        assertThat(actualIsReadyToStart).isFalse();
    }
}
