package com.lecoders.poll.results;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Immutable calculation result; counts mean Borda points or votes.
 *
 */
public record CountResult(
        @JsonProperty("winning_option_id") Long winningOptionId,
        Map<Long, Long> counts,
        @JsonProperty("total_ballots") long totalBallots,
        List<Round> rounds) {

    /** Takes defensive copies so callers cannot alter an already computed result. */
    public CountResult {
        counts = Map.copyOf(counts);
        rounds = List.copyOf(rounds);
    }

    /** Counts include only candidates active at the start of this round. */
    public record Round(
            int round,
            Map<Long, Long> counts,
            @JsonProperty("active_ballots") long activeBallots,
            @JsonProperty("exhausted_ballots") long exhaustedBallots,
            @JsonProperty("eliminated_option_id") Long eliminatedOptionId) {

        /** Preserves the round independently of later rounds. */
        public Round {
            counts = Map.copyOf(counts);
        }
    }
}
