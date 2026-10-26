package com.lecoders.poll;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared key validation and returned zero-based option IDs.
 * REST endpoints for creating polls, casting ballots, counting results and deleting polls.
 */
@RestController
public class PollController {

    record CreatePollRequest(
            String question,
            List<String> options,
            @JsonProperty("counting_method") String countingMethod) {
    }

    record CastBallotRequest(
            @JsonProperty("voter_id") String voterId,
            @JsonProperty("ranked_option_ids") List<Integer> rankedOptionIds) {
    }

    private static final Set<String> COUNTING_METHODS =
            Set.of("plurality", "ranked_choice", "borda");

    private final PollRepository polls;
    private final BallotRepository ballots;
    private final ApiKeyValidator apiKeys;

    /**
     * Creates the controller.
     *
     * @param polls repository used to store and look up polls
     * @param ballots repository used to store ballots
     */
    public PollController(
            PollRepository polls, BallotRepository ballots, ApiKeyValidator apiKeys) {
        this.polls = polls;
        this.ballots = ballots;
        this.apiKeys = apiKeys;
    }

    /**
     * Creates a new poll owned by the calling client program.
     *
     * @param apiKey API key of the client program
     * @param request the poll's question, options and counting method
     * @return the new poll's id and zero-based option ids
     */
    @PostMapping("/polls")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createPoll(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody CreatePollRequest request) {
        apiKeys.requireValid(apiKey);
        if (isBlank(request.question())
                || request.options() == null || request.options().size() < 2
                || isBlank(request.countingMethod())
                || !COUNTING_METHODS.contains(request.countingMethod())
                || new HashSet<>(request.options()).size() != request.options().size()
                || request.options().stream().anyMatch(PollController::isBlank)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "question, at least 2 distinct non-blank options, and a counting_method of "
                            + COUNTING_METHODS + " are required");
        }
        Poll poll = polls.save(new Poll(
                null, request.question(), request.options(), apiKey, request.countingMethod()));
        return Map.of("poll_id", poll.id(), "option_ids", poll.optionIds());
    }

    /**
     * Casts a ranked ballot on a poll. Option ids are the zero-based positions of the
     * options in the list the poll was created with.
     *
     * @param apiKey API key of the client program
     * @param pollId id of the poll being voted on
     * @param request the voter's id and ranked list of option ids
     * @return the new ballot's id
     */
    @PostMapping("/polls/{pollId}/ballots")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> castBallot(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @PathVariable long pollId,
            @RequestBody CastBallotRequest request) {
        apiKeys.requireValid(apiKey);
        Poll poll = polls.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "unknown poll_id " + pollId));
        // TODO: reject ballots once the poll is closed, after polls store a closing time.
        if (isBlank(request.voterId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "voter_id is required");
        }
        validateRanking(request.rankedOptionIds(), poll.options().size());
        Ballot ballot = ballots.saveIfFirstVote(
                        new Ballot(null, pollId, request.voterId(), request.rankedOptionIds()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "voter " + request.voterId() + " has already voted on this poll"));
        return Map.of("ballot_id", ballot.id());
    }

    private static void validateRanking(List<Integer> rankedOptionIds, int optionCount) {
        if (rankedOptionIds == null || rankedOptionIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ranked_option_ids must contain at least one option");
        }
        Set<Integer> seen = new HashSet<>();
        for (Integer optionId : rankedOptionIds) {
            if (optionId == null || optionId < 0 || optionId >= optionCount) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "unknown option id " + optionId);
            }
            if (!seen.add(optionId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "option id " + optionId + " is ranked more than once");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
