package com.lecoders.poll.results;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lecoders.poll.Poll;
import com.lecoders.poll.PollRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Loads existing poll data and counts a ballot snapshot. */
@Service
public class ResultsService {
    private final PollRepository polls;
    private final RankedBallotStore ballots;

    /** Uses the shared poll repository and replaceable ballot read boundary. */
    public ResultsService(PollRepository polls, RankedBallotStore ballots) {
        this.polls = polls;
        this.ballots = ballots;
    }

    /** Validates the explicit method first, then returns 404 for an unknown poll. */
    public ResultResponse results(long pollId, String methodName) {
        CountingMethod method;
        try {
            method = CountingMethod.parse(methodName);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        Poll poll = polls.findById(pollId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown poll_id"));
        CountResult count;
        try {
            count = CountingAlgorithms.count(
                    poll.optionIds(), ballots.findRankings(pollId), method);
        } catch (IllegalArgumentException exception) {
            // Corrupt persisted data is a server problem, not an invalid GET request.
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Invalid stored ballot data", exception);
        }
        return new ResultResponse(pollId, method.wireName(), count.winningOptionId(),
                count.counts(), count.totalBallots(), count.rounds());
    }

    /** JSON response fields are shared across all three counting methods. */
    public record ResultResponse(
            @JsonProperty("poll_id") long pollId,
            @JsonProperty("counting_method") String countingMethod,
            @JsonProperty("winning_option_id") Long winningOptionId,
            Map<Long, Long> counts,
            @JsonProperty("total_ballots") long totalBallots,
            List<CountResult.Round> rounds) {}
}
