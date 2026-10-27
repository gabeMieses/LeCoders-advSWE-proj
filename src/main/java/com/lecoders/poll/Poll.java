package com.lecoders.poll;

import java.util.List;
import java.util.stream.LongStream;

/**
 * A poll created by a client program.
 *
 * <p>AI-generated with Claude Code (Opus 5.5); reviewed and tested by Gabriel Mieses.
 *
 * @param id unique poll id, or null before the poll is saved
 * @param question the question the poll asks
 * @param options the answer options voters can rank
 * @param apiKey API key of the client program that created the poll
 * @param countingMethod the counting method requested for the poll
 */
public record Poll(
        Long id, String question, List<String> options, String apiKey, String countingMethod) {

    /**
     * Protects the zero-based option positions used by ballot POST.
     *
     */
    public Poll {
        options = List.copyOf(options);
    }

    /**
     * Matches the team's zero-based option IDs, 0 through N-1.
     *
     */
    public List<Long> optionIds() {
        return LongStream.range(0, options.size()).boxed().toList();
    }
}
