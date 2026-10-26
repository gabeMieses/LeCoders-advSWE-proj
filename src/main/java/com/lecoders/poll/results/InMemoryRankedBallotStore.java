package com.lecoders.poll.results;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Temporary storage matching the current in-memory PollRepository.
 * Issue #4 must validate voters and options before calling append; this is not a ballot endpoint.
 */
@Repository
public class InMemoryRankedBallotStore implements RankedBallotStore {
    private final Map<Long, List<List<Long>>> rankings = new ConcurrentHashMap<>();

    /** Atomically appends a defensive copy of an already validated ballot. */
    public void append(long pollId, List<Long> rankedOptionIds) {
        List<Long> ballot = List.copyOf(rankedOptionIds);
        rankings.compute(pollId, (key, existing) -> {
            List<List<Long>> updated = new ArrayList<>();
            if (existing != null) {
                updated.addAll(existing);
            }
            updated.add(ballot);
            return List.copyOf(updated);
        });
    }

    @Override
    public List<List<Long>> findRankings(long pollId) {
        return rankings.getOrDefault(pollId, List.of());
    }

    /** Removes a poll's ballots when Issue #5 deletes the poll. */
    public void deleteByPollId(long pollId) {
        rankings.remove(pollId);
    }
}
