package com.lecoders.poll;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Configurable shared key check; no hard-coded production keys.
 *
 * <p>AI-generated with OpenAI Codex; integration tested by Haoran (@lek1ma).
 */
@Component
public class ApiKeyValidator {
    private final Set<String> allowed;

    /** Configure poll.api-keys (environment POLL_API_KEYS) as comma-separated client keys. */
    public ApiKeyValidator(@Value("${poll.api-keys:}") String keys) {
        allowed = Arrays.stream(keys.split(",")).map(String::trim)
                .filter(key -> !key.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    /** Fails closed when no keys are configured; any registered client can read results. */
    public void requireValid(String key) {
        if (key == null || !allowed.contains(key)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid or missing API key");
        }
    }
}
