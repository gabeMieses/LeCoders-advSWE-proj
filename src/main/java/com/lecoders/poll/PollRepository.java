package com.lecoders.poll;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

/** Existing in-memory poll storage. */
@Repository
public class PollRepository {
    private final Map<Long, Poll> polls = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    /** Assigns an ID to new polls and stores the immutable value. */
    public Poll save(Poll poll) {
        Poll saved = poll.id() == null
                ? new Poll(nextId.getAndIncrement(), poll.question(), poll.options(),
                        poll.apiKey(), poll.countingMethod())
                : poll;
        polls.put(saved.id(), saved);
        return saved;
    }

    /** Reads a poll without creating an empty poll for an unknown ID. */
    public Optional<Poll> findById(long pollId) {
        return Optional.ofNullable(polls.get(pollId));
    }
}
