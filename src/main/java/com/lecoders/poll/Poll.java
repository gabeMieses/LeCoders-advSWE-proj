package com.lecoders.poll;

import java.util.List;
import java.util.stream.LongStream;

/** Existing poll model with immutable option order. */
public record Poll(
        Long id, String question, List<String> options, String apiKey, String countingMethod) {

    /** Freezes option order because option IDs are one-based positions within this poll. */
    public Poll {
        options = List.copyOf(options);
    }

    /** Returns poll-local IDs 1..N until Issue #2 introduces persistent option entities. */
    public List<Long> optionIds() {
        return LongStream.rangeClosed(1, options.size()).boxed().toList();
    }
}
