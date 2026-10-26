package com.lecoders.poll;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class PollRepository {

  private final Map<Long, Poll> polls = new ConcurrentHashMap<>();
  private final AtomicLong nextId = new AtomicLong(1);

  public Poll save(Poll poll) {
    Poll saved = poll.id() == null
        ? new Poll(nextId.getAndIncrement(), poll.question(), poll.options(),
            poll.apiKey(), poll.countingMethod())
        : poll;
    polls.put(saved.id(), saved);
    return saved;
  }
}
