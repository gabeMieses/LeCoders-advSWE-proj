package com.lecoders.poll.results;

import com.lecoders.poll.ApiKeyValidator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET results adapter; calculations have no write side effects. */
@RestController
public class ResultsController {
    private final ResultsService results;
    private final ApiKeyValidator apiKeys;

    /** Shares the API-key policy with poll creation. */
    public ResultsController(ResultsService results, ApiKeyValidator apiKeys) {
        this.results = results;
        this.apiKeys = apiKeys;
    }

    /** Returns current results, including while voting is still open. */
    @GetMapping("/polls/{pollId}/results")
    public ResultsService.ResultResponse getResults(
            @PathVariable long pollId,
            @RequestParam(name = "counting_method", required = false) String method,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey) {
        apiKeys.requireValid(apiKey);
        return results.results(pollId, method);
    }
}
