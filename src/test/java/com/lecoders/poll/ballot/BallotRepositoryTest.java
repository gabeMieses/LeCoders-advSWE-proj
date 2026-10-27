package com.lecoders.poll.ballot;

import static org.assertj.core.api.Assertions.assertThat;

import com.lecoders.poll.Ballot;
import com.lecoders.poll.BallotRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// AI-generated with Claude Code (Opus 5.5); reviewed and tested by JB Diei.
class BallotRepositoryTest {

    private BallotRepository ballots;

    @BeforeEach
    void setUp() {
        ballots = new BallotRepository();
    }

    @Test
    void saveIfFirstVote_firstVote_assignsIdAndKeepsFields() {
        Optional<Ballot> saved = ballots.saveIfFirstVote(new Ballot(null, 1, "v1", List.of(2, 0)));

        assertThat(saved).isPresent();
        assertThat(saved.get().id()).isEqualTo(1L);
        assertThat(saved.get().pollId()).isEqualTo(1L);
        assertThat(saved.get().voterId()).isEqualTo("v1");
        assertThat(saved.get().rankedOptionIds()).containsExactly(2, 0);
    }

    @Test
    void saveIfFirstVote_assignsIncreasingIds() {
        Ballot first = ballots.saveIfFirstVote(new Ballot(null, 1, "v1", List.of(0)))
                .orElseThrow();
        Ballot second = ballots.saveIfFirstVote(new Ballot(null, 1, "v2", List.of(1)))
                .orElseThrow();

        assertThat(second.id()).isGreaterThan(first.id());
    }

    @Test
    void saveIfFirstVote_sameVoterSamePoll_returnsEmpty() {
        ballots.saveIfFirstVote(new Ballot(null, 1, "v1", List.of(0)));

        assertThat(ballots.saveIfFirstVote(new Ballot(null, 1, "v1", List.of(1)))).isEmpty();
    }

    @Test
    void saveIfFirstVote_sameVoterDifferentPoll_isSaved() {
        ballots.saveIfFirstVote(new Ballot(null, 1, "v1", List.of(0)));

        assertThat(ballots.saveIfFirstVote(new Ballot(null, 2, "v1", List.of(0)))).isPresent();
    }

    @Test
    void saveIfFirstVote_ignoresIdOnInput() {
        Ballot saved = ballots.saveIfFirstVote(new Ballot(42L, 1, "v1", List.of(0))).orElseThrow();

        assertThat(saved.id()).isEqualTo(1L);
    }

    // AI-generated with Claude Code (Opus 5.5); reviewed and tested by Jason Dixon.
    @Test
    void deleteByPollId_removesOnlyThatPollsBallots() {
        ballots.saveIfFirstVote(new Ballot(null, 1, "v1", List.of(0)));
        ballots.saveIfFirstVote(new Ballot(null, 1, "v2", List.of(1)));
        ballots.saveIfFirstVote(new Ballot(null, 2, "v1", List.of(0)));

        assertThat(ballots.deleteByPollId(1)).isEqualTo(2);
        // Poll 2's ballot is still recorded, so v1 still cannot vote on it again.
        assertThat(ballots.saveIfFirstVote(new Ballot(null, 2, "v1", List.of(1)))).isEmpty();
    }

    @Test
    void deleteByPollId_forgetsVotersOfThatPoll() {
        ballots.saveIfFirstVote(new Ballot(null, 1, "v1", List.of(0)));

        ballots.deleteByPollId(1);

        assertThat(ballots.saveIfFirstVote(new Ballot(null, 1, "v1", List.of(0)))).isPresent();
    }

    @Test
    void deleteByPollId_pollWithNoBallots_returnsZero() {
        assertThat(ballots.deleteByPollId(7)).isZero();
    }
}
