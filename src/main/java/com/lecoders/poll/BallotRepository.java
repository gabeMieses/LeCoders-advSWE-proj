package com.lecoders.poll;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

/**
 * In-memory store for ballots.
 */
@Repository
public class BallotRepository {

    private final Map<Long, Ballot> ballots = new ConcurrentHashMap<>();
    private final Set<String> voted = ConcurrentHashMap.newKeySet();
    private final AtomicLong nextId = new AtomicLong(1);

    /**
     * Saves a new ballot unless its voter has already voted on the same poll.
     *
     * @param ballot the ballot to save; its id is ignored
     * @return the saved ballot with its id set, or empty if the voter already voted
     */
    public synchronized Optional<Ballot> saveIfFirstVote(Ballot ballot) {
        if (!voted.add(ballot.pollId() + ":" + ballot.voterId())) {
            return Optional.empty();
        }
        Ballot saved = new Ballot(nextId.getAndIncrement(), ballot.pollId(), ballot.voterId(),
                ballot.rankedOptionIds());
        ballots.put(saved.id(), saved);
        return Optional.of(saved);
    }

    /** Atomically snapshots the same ballots accepted by POST. */
    public synchronized List<Ballot> findByPollId(long pollId) {
        return ballots.values().stream().filter(ballot -> ballot.pollId() == pollId)
                .sorted(Comparator.comparing(Ballot::id)).toList();
    }
}
