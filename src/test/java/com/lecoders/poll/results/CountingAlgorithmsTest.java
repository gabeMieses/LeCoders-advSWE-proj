package com.lecoders.poll.results;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Hand-calculated election examples and invalid-data tests for Issue #6.
 *
 * <p>AI-generated with OpenAI Codex; integration tested by Haoran (@lek1ma).
 */
class CountingAlgorithmsTest {
    private static final List<Long> OPTIONS = List.of(1L, 2L, 3L);

    @ParameterizedTest
    @EnumSource(CountingMethod.class)
    void emptyPollHasNoWinner(CountingMethod method) {
        CountResult result = CountingAlgorithms.count(OPTIONS, List.of(), method);
        assertThat(result.winningOptionId()).isNull();
        assertThat(result.counts()).containsEntry(1L, 0L).containsEntry(2L, 0L)
                .containsEntry(3L, 0L);
        assertThat(result.totalBallots()).isZero();
        assertThat(result.rounds()).isEmpty();
    }

    @Test
    void pluralityIgnoresLowerRanksAndRetainsZeroVoteOptions() {
        CountResult result = CountingAlgorithms.count(OPTIONS,
                List.of(List.of(2L, 1L), List.of(2L, 3L), List.of(1L)),
                CountingMethod.PLURALITY);
        assertThat(result.winningOptionId()).isEqualTo(2L);
        assertThat(result.counts()).containsEntry(1L, 1L).containsEntry(2L, 2L)
                .containsEntry(3L, 0L);
        assertThat(result.totalBallots()).isEqualTo(3);
        assertThat(result.rounds()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(CountingMethod.class)
    void tiesUseNumericIdsRegardlessOfOptionOrder(CountingMethod method) {
        CountResult result = CountingAlgorithms.count(List.of(10L, 2L),
                List.of(List.of(10L, 2L), List.of(2L, 10L)), method);
        assertThat(result.winningOptionId()).isEqualTo(2L);
        if (method == CountingMethod.RANKED_CHOICE) {
            assertThat(result.rounds().get(0).eliminatedOptionId()).isEqualTo(10L);
        }
    }

    @Test
    void bordaUsesPollOptionCountAndZeroForUnrankedOptions() {
        CountResult result = CountingAlgorithms.count(OPTIONS,
                List.of(List.of(1L, 2L, 3L), List.of(2L, 3L, 1L), List.of(3L)),
                CountingMethod.BORDA);
        assertThat(result.counts()).containsEntry(1L, 2L).containsEntry(2L, 3L)
                .containsEntry(3L, 3L);
        assertThat(result.winningOptionId()).isEqualTo(2L);
    }

    @Test
    void rankedChoiceTransfersVotesAcrossMultipleRounds() {
        List<List<Long>> ballots = List.of(
                List.of(1L, 2L, 3L), List.of(1L, 2L, 3L),
                List.of(2L, 3L, 1L), List.of(2L, 3L, 1L), List.of(3L, 2L, 1L));
        CountResult result = CountingAlgorithms.count(
                OPTIONS, ballots, CountingMethod.RANKED_CHOICE);
        assertThat(result.winningOptionId()).isEqualTo(2L);
        assertThat(result.rounds()).hasSize(2);
        assertThat(result.rounds().get(0).counts())
                .containsEntry(1L, 2L).containsEntry(2L, 2L).containsEntry(3L, 1L);
        assertThat(result.rounds().get(0).eliminatedOptionId()).isEqualTo(3L);
        assertThat(result.rounds().get(1).eliminatedOptionId()).isNull();
        assertThat(result.counts()).containsEntry(1L, 2L).containsEntry(2L, 3L)
                .doesNotContainKey(3L);
        assertThat(ballots.get(4)).containsExactly(3L, 2L, 1L);
        assertThatThrownBy(() -> result.rounds().get(0).counts().put(1L, 9L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void exhaustedVotesLeaveMajorityDenominatorAndTotalStaysFixed() {
        CountResult result = CountingAlgorithms.count(OPTIONS,
                List.of(List.of(1L), List.of(1L), List.of(2L), List.of(2L), List.of(3L)),
                CountingMethod.RANKED_CHOICE);
        assertThat(result.winningOptionId()).isEqualTo(1L);
        assertThat(result.rounds()).hasSize(3);
        assertThat(result.rounds().get(1).exhaustedBallots()).isEqualTo(1);
        assertThat(result.rounds().get(1).activeBallots()).isEqualTo(4);
        assertThat(result.rounds().get(2).exhaustedBallots()).isEqualTo(3);
        assertThat(result.totalBallots()).isEqualTo(5);
    }

    @Test
    void immediateMajorityNeedsNoElimination() {
        CountResult result = CountingAlgorithms.count(OPTIONS,
                List.of(List.of(3L), List.of(3L), List.of(2L)), CountingMethod.RANKED_CHOICE);
        assertThat(result.winningOptionId()).isEqualTo(3L);
        assertThat(result.rounds()).hasSize(1);
        assertThat(result.rounds().get(0).eliminatedOptionId()).isNull();
    }

    @Test
    void invalidStoredBallotsFailInsteadOfSilentlyChangingElection() {
        List<List<List<Long>>> badSets = new ArrayList<>();
        badSets.add(List.of(List.of()));
        badSets.add(List.of(List.of(1L, 1L)));
        badSets.add(List.of(List.of(99L)));
        badSets.add(Arrays.asList((List<Long>) null));
        badSets.add(List.of(Arrays.asList(1L, null)));
        for (List<List<Long>> ballots : badSets) {
            assertThatIllegalArgumentException().isThrownBy(() ->
                    CountingAlgorithms.count(OPTIONS, ballots, CountingMethod.PLURALITY));
        }
        assertThatIllegalArgumentException().isThrownBy(() ->
                CountingAlgorithms.count(OPTIONS, null, CountingMethod.PLURALITY));
    }

    @Test
    void invalidOptionsAndMethodsAreRejected() {
        List<List<Long>> invalid = Arrays.asList(
                null, List.of(), List.of(1L), List.of(1L, 1L), Arrays.asList(1L, null));
        for (List<Long> options : invalid) {
            assertThatIllegalArgumentException().isThrownBy(() ->
                    CountingAlgorithms.count(options, List.of(), CountingMethod.BORDA));
        }
        assertThatIllegalArgumentException().isThrownBy(() ->
                CountingAlgorithms.count(OPTIONS, List.of(), null));
        for (CountingMethod method : CountingMethod.values()) {
            assertThat(CountingMethod.parse(method.wireName())).isEqualTo(method);
        }
        for (String value : Arrays.asList(null, "", "PLURALITY", "approval", " borda")) {
            assertThatIllegalArgumentException().isThrownBy(() -> CountingMethod.parse(value));
        }
    }
}
