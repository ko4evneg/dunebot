package ru.trainithard.dunebot.model;

import org.junit.jupiter.params.ParameterizedTest;
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
}
