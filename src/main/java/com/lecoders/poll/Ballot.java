package com.lecoders.poll;

import java.util.List;

/**
 * A ranked ballot cast on a poll.
 *
 * <p>AI-generated with Claude Code (Opus 5.5); reviewed and tested by JB Diei.
 *
 * @param id unique ballot id, or null before the ballot is saved
 * @param pollId id of the poll the ballot was cast on
 * @param voterId client-supplied id of the voter
 * @param rankedOptionIds option ids ordered from most to least preferred
 */
public record Ballot(Long id, long pollId, String voterId, List<Integer> rankedOptionIds) {
    /**
     * Freezes the submitted ranking for consistent result snapshots.
     *
     * <p>AI-generated with OpenAI Codex; integration tested by Haoran (@lek1ma).
     */
    public Ballot {
        rankedOptionIds = List.copyOf(rankedOptionIds);
    }
}
