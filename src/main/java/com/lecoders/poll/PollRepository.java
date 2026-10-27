package com.lecoders.poll;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

/**
 * In-memory store for polls.
 */
@Repository
public class PollRepository {

    private final Map<Long, Poll> polls = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    /**
     * Saves a poll, assigning a new id if it does not have one yet.
     *
     * <p>AI-generated with Claude Code (Opus 5.5); reviewed and tested by Gabriel Mieses.
     *
     * @param poll the poll to save
     * @return the saved poll with its id set
     */
    public Poll save(Poll poll) {
        Poll saved = poll.id() == null
                ? new Poll(nextId.getAndIncrement(), poll.question(), poll.options(),
                        poll.apiKey(), poll.countingMethod())
                : poll;
        polls.put(saved.id(), saved);
        return saved;
    }

    /**
     * Looks up a poll by id.
     *
     * @param id the poll id
     * @return the poll, or empty if no poll has that id
     */
    public Optional<Poll> findById(long id) {
        return Optional.ofNullable(polls.get(id));
    }

    /**
     * Removes a poll.
     *
     * <p>AI-generated with Claude Code (Opus 5.5); reviewed and tested by Jason Dixon.
     *
     * @param id the poll id
     * @return true if a poll with that id existed and was removed
     */
    public boolean deleteById(long id) {
        return polls.remove(id) != null;
    }
}
