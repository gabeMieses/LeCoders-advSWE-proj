package com.lecoders.poll.results;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.lecoders.poll.Poll;
import com.lecoders.poll.PollRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/** Real HTTP tests using application stores; ballot POST remains separate. AI-generated. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "poll.api-keys=test-key,other-client")
class ResultsHttpTest {
    @Autowired
    private TestRestTemplate http;
    @Autowired
    private PollRepository polls;
    @Autowired
    private InMemoryRankedBallotStore ballots;

    private ResponseEntity<JsonNode> get(String path, String key) {
        HttpHeaders headers = new HttpHeaders();
        if (key != null) {
            headers.set("X-API-Key", key);
        }
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }

    @ParameterizedTest
    @EnumSource(CountingMethod.class)
    void returnsPopulatedResultsOverHttp(CountingMethod method) {
        Poll poll = polls.save(new Poll(null, "Map?", List.of("A", "B", "C"),
                "test-key", "plurality"));
        ballots.append(poll.id(), List.of(2L, 1L, 3L));
        ballots.append(poll.id(), List.of(2L));
        ballots.append(poll.id(), List.of(1L, 2L));
        String path = "/polls/" + poll.id() + "/results?counting_method=" + method.wireName();
        ResponseEntity<JsonNode> response = get(path, "other-client");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode body = response.getBody();
        assertThat(body.get("poll_id").asLong()).isEqualTo(poll.id());
        assertThat(body.get("winning_option_id").asLong()).isEqualTo(2);
        assertThat(body.get("total_ballots").asInt()).isEqualTo(3);
        assertThat(body.get("counting_method").asText()).isEqualTo(method.wireName());
        assertThat(body.has("counts")).isTrue();
        assertThat(body.has("rounds")).isTrue();
        assertThat(get(path, "test-key").getBody()).isEqualTo(body);
        assertThat(ballots.findRankings(poll.id())).hasSize(3);
    }

    @Test
    void createThenQueryEmptyPollUsingReturnedIds() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "test-key");
        ResponseEntity<JsonNode> created = http.postForEntity("/polls", new HttpEntity<>(
                Map.of("question", "Map?", "options", List.of("A", "B"),
                        "counting_method", "plurality"), headers), JsonNode.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getBody().get("option_ids").toString()).isEqualTo("[1,2]");
        long pollId = created.getBody().get("poll_id").asLong();
        JsonNode result = get("/polls/" + pollId + "/results?counting_method=ranked_choice",
                "test-key").getBody();
        assertThat(result.get("winning_option_id").isNull()).isTrue();
        assertThat(result.get("total_ballots").asInt()).isZero();
        assertThat(result.get("counts").get("1").asInt()).isZero();
        assertThat(result.get("rounds").isEmpty()).isTrue();
    }

    @Test
    void rejectsUnauthorizedMalformedAndUnknownRequests() {
        String path = "/polls/999999/results?counting_method=plurality";
        assertThat(get(path, null).getStatusCode().value()).isEqualTo(401);
        assertThat(get(path, "wrong").getStatusCode().value()).isEqualTo(401);
        assertThat(get(path, "test-key").getStatusCode().value()).isEqualTo(404);
        assertThat(get("/polls/999999/results?counting_method=approval", "test-key")
                .getStatusCode().value()).isEqualTo(400);
        assertThat(get("/polls/999999/results", "test-key")
                .getStatusCode().value()).isEqualTo(400);
        assertThat(get("/polls/not-a-number/results?counting_method=borda", "test-key")
                .getStatusCode().value()).isEqualTo(400);
    }
}
