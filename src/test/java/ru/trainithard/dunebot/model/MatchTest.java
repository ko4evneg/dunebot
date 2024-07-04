package ru.trainithard.dunebot.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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

    @ParameterizedTest
    @MethodSource("submitsMatchPlayerSource")
    void shouldReturnHasAllPlacesSubmitTrueWhenAllPlacesPresented(ModType modType, List<MatchPlayer> matchPlayers) {
        match.setModType(modType);
        match.setMatchPlayers(matchPlayers);

        boolean actualHasAllPlacesSubmitted = match.hasAllPlacesSubmitted();

        assertThat(actualHasAllPlacesSubmitted).isTrue();
    }

    private static Stream<Arguments> submitsMatchPlayerSource() {
        return Stream.of(
                Arguments.of(ModType.CLASSIC, createMatchPlayers(1, 2, 3, 4)),
                Arguments.of(ModType.CLASSIC, createMatchPlayers(1, 2, 3, 4, 0, 0)),
                Arguments.of(ModType.UPRISING_4, createMatchPlayers(1, 2, 3, 4)),
                Arguments.of(ModType.UPRISING_4, createMatchPlayers(1, 2, 3, 4, 0, 0)),
                Arguments.of(ModType.UPRISING_6, createMatchPlayers(1, 2, 3, 4, 5, 6)),
                Arguments.of(ModType.UPRISING_6, createMatchPlayers(1, 2, 3, 4, 5, 6, 0, 0))
        );
    }

    @ParameterizedTest
    @MethodSource("missingSubmitsMatchPlayerSource")
    void shouldReturnHasAllPlacesSubmitFalseWhenPlacesAreMissing(ModType modType, List<MatchPlayer> matchPlayers) {
        match.setModType(modType);
        match.setMatchPlayers(matchPlayers);

        boolean actualHasAllPlacesSubmitted = match.hasAllPlacesSubmitted();

        assertThat(actualHasAllPlacesSubmitted).isFalse();
    }

    private static Stream<Arguments> missingSubmitsMatchPlayerSource() {
        return Stream.of(
                Arguments.of(ModType.CLASSIC, createMatchPlayers(1, 2, 3)),
                Arguments.of(ModType.CLASSIC, createMatchPlayers(1, 2, 3, 0)),
                Arguments.of(ModType.CLASSIC, createMatchPlayers(1, 2, 3, 0, 0)),
                Arguments.of(ModType.CLASSIC, createMatchPlayers(0, 0, 0, 0, 0)),
                Arguments.of(ModType.UPRISING_4, createMatchPlayers(1, 2, 3)),
                Arguments.of(ModType.UPRISING_4, createMatchPlayers(1, 2, 3, 0)),
                Arguments.of(ModType.UPRISING_4, createMatchPlayers(1, 2, 3, 0, 0)),
                Arguments.of(ModType.UPRISING_4, createMatchPlayers(0, 0, 0, 0, 0)),
                Arguments.of(ModType.UPRISING_6, createMatchPlayers(1, 2, 3, 4, 5)),
                Arguments.of(ModType.UPRISING_6, createMatchPlayers(1, 2, 3, 4, 5, 0)),
                Arguments.of(ModType.UPRISING_6, createMatchPlayers(1, 2, 3, 4, 5, 0, 0)),
                Arguments.of(ModType.UPRISING_6, createMatchPlayers(0, 0, 0, 0, 0, 0, 0))
        );
    }

    @ParameterizedTest
    @MethodSource("missingCandidatePlacesSource")
    void shouldReturnMissingCandidatePlaces(ModType modType, List<MatchPlayer> matchPlayers, Set<Integer> expectedResult) {
        match.setModType(modType);
        match.setMatchPlayers(matchPlayers);

        Set<Integer> actualMissingCandidatePlaces = match.getMissingCandidatePlaces();

        assertThat(actualMissingCandidatePlaces).containsExactlyInAnyOrderElementsOf(expectedResult);
    }

    private static Stream<Arguments> missingCandidatePlacesSource() {
        return Stream.of(
                Arguments.of(ModType.CLASSIC, createMatchPlayers(1, 2, 3), Set.of(4)),
                Arguments.of(ModType.CLASSIC, createMatchPlayers(1), Set.of(2, 3, 4)),
                Arguments.of(ModType.CLASSIC, createMatchPlayers(2, 3), Set.of(1, 4)),
                Arguments.of(ModType.CLASSIC, createMatchPlayers(1, 2, 3, 0, 0), Set.of(4)),
                Arguments.of(ModType.CLASSIC, createMatchPlayers(0, 0, 0, 0, 0), Set.of(1, 2, 3, 4)),
                Arguments.of(ModType.UPRISING_4, createMatchPlayers(1, 2, 3), Set.of(4)),
                Arguments.of(ModType.UPRISING_4, createMatchPlayers(1), Set.of(2, 3, 4)),
                Arguments.of(ModType.UPRISING_4, createMatchPlayers(2, 3), Set.of(1, 4)),
                Arguments.of(ModType.UPRISING_4, createMatchPlayers(1, 2, 3, 0, 0), Set.of(4)),
                Arguments.of(ModType.UPRISING_4, createMatchPlayers(0, 0, 0, 0, 0), Set.of(1, 2, 3, 4)),
                Arguments.of(ModType.UPRISING_6, createMatchPlayers(1, 2, 3), Set.of(4, 5, 6)),
                Arguments.of(ModType.UPRISING_6, createMatchPlayers(1), Set.of(2, 3, 4, 5, 6)),
                Arguments.of(ModType.UPRISING_6, createMatchPlayers(2, 3, 5), Set.of(1, 4, 6)),
                Arguments.of(ModType.UPRISING_6, createMatchPlayers(1, 2, 3, 0, 0), Set.of(4, 5, 6)),
                Arguments.of(ModType.UPRISING_6, createMatchPlayers(0, 0, 0, 0, 0), Set.of(1, 2, 3, 4, 5, 6))
        );
    }

    private static List<MatchPlayer> createMatchPlayers(int... candidatePlaces) {
        List<MatchPlayer> matchPlayers = new ArrayList<>();
        for (int candidatePlace : candidatePlaces) {
            MatchPlayer matchPlayer = new MatchPlayer();
            matchPlayer.setCandidatePlace(candidatePlace);
            matchPlayers.add(matchPlayer);
        }
        return matchPlayers;
    }
}
