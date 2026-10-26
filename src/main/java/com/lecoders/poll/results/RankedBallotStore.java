package com.lecoders.poll.results;

import java.util.List;

/**
 * Read boundary for Issue #4 or a future database adapter.
 *
 * <p>AI-generated with OpenAI Codex; integration tested by Haoran (@lek1ma).
 */
public interface RankedBallotStore {
    /** Returns an immutable, consistent snapshot of the poll's stored rankings. */
    List<List<Long>> findRankings(long pollId);
}
