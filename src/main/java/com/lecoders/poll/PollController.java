package com.lecoders.poll;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Existing creation endpoint. */
@RestController
public class PollController {
    record CreatePollRequest(
            String question,
            List<String> options,
            @JsonProperty("counting_method") String countingMethod) {}

    private final PollRepository polls;
    private final ApiKeyValidator apiKeys;

    /** Shares repositories and key validation with the results endpoint. */
    public PollController(PollRepository polls, ApiKeyValidator apiKeys) {
        this.polls = polls;
        this.apiKeys = apiKeys;
    }

    /** Creates a poll and exposes IDs in the same order as the supplied option labels. */
    @PostMapping("/polls")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createPoll(
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            @RequestBody CreatePollRequest request) {
        apiKeys.requireValid(apiKey);
        if (isBlank(request.question())
                || request.options() == null || request.options().size() < 2
                || isBlank(request.countingMethod())
                || request.options().stream().anyMatch(PollController::isBlank)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "question, at least 2 non-blank options, and counting_method are required");
        }
        Poll poll = polls.save(new Poll(
                null, request.question(), request.options(), apiKey, request.countingMethod()));
        return Map.of("poll_id", poll.id(), "option_ids", poll.optionIds());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
