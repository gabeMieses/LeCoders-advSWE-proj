package com.lecoders.poll.results;

import com.lecoders.poll.BallotRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Reads the same repository used by ballot POST. */
@Component
public class RepositoryRankedBallotStore implements RankedBallotStore {
    private final BallotRepository ballots;

    /** Adapts the team's Integer option IDs to the counting module's numeric IDs. */
    public RepositoryRankedBallotStore(BallotRepository ballots) {
        this.ballots = ballots;
    }

    @Override
    public List<List<Long>> findRankings(long pollId) {
        return ballots.findByPollId(pollId).stream()
                .map(ballot -> ballot.rankedOptionIds().stream().map(Integer::longValue).toList())
                .toList();
    }
}
