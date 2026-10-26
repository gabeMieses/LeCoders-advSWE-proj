package com.lecoders.poll.results;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.lecoders.poll.support.MockApiKeys;
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

/** Creates, casts and counts using real HTTP only. AI-assisted integration for Issue #6. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "poll.api-keys=" + MockApiKeys.CLIENT_A + "," + MockApiKeys.CLIENT_B)
class ResultsHttpTest {
    @Autowired
    private TestRestTemplate http;

    private HttpHeaders headers(String key) {
        HttpHeaders headers = new HttpHeaders();
        if (key != null) {
            headers.set(MockApiKeys.HEADER, key);
        }
        return headers;
    }

    private ResponseEntity<JsonNode> get(String path, String key) {
        return http.exchange(path, HttpMethod.GET,
                new HttpEntity<>(headers(key)), JsonNode.class);
    }

    private ResponseEntity<JsonNode> post(String path, Object body, String key) {
        return http.postForEntity(path, new HttpEntity<>(body, headers(key)), JsonNode.class);
    }

    private long createPoll() {
        ResponseEntity<JsonNode> created = post("/polls",
                Map.of("question", "Map?", "options", List.of("A", "B", "C"),
                        "counting_method", "plurality"), MockApiKeys.CLIENT_A);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getBody().get("option_ids").toString()).isEqualTo("[0,1,2]");
        return created.getBody().get("poll_id").asLong();
    }

    private void cast(long pollId, String voter, List<Integer> ranking) {
        assertThat(post("/polls/" + pollId + "/ballots",
                Map.of("voter_id", voter, "ranked_option_ids", ranking), MockApiKeys.CLIENT_B)
                .getStatusCode().value()).isEqualTo(201);
    }

    @ParameterizedTest
    @EnumSource(CountingMethod.class)
    void returnsPopulatedResultsOverHttp(CountingMethod method) {
        long pollId = createPoll();
        cast(pollId, "v1", List.of(1, 0, 2));
        cast(pollId, "v2", List.of(1));
        cast(pollId, "v3", List.of(0, 1));
        String path = "/polls/" + pollId + "/results?counting_method=" + method.wireName();
        ResponseEntity<JsonNode> response = get(path, MockApiKeys.CLIENT_B);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode body = response.getBody();
        assertThat(body.get("poll_id").asLong()).isEqualTo(pollId);
        assertThat(body.get("winning_option_id").asLong()).isEqualTo(1);
        assertThat(body.get("total_ballots").asInt()).isEqualTo(3);
        assertThat(body.get("counting_method").asText()).isEqualTo(method.wireName());
        long expected = method == CountingMethod.BORDA ? 5 : 2;
        assertThat(body.get("counts").get("1").asLong()).isEqualTo(expected);
        assertThat(get(path, MockApiKeys.CLIENT_A).getBody()).isEqualTo(body);
    }

    @Test
    void rankedChoiceTransfersPostedBallotsAndRecordsRounds() {
        long pollId = createPoll();
        cast(pollId, "v1", List.of(0, 1));
        cast(pollId, "v2", List.of(0));
        cast(pollId, "v3", List.of(1));
        cast(pollId, "v4", List.of(1));
        cast(pollId, "v5", List.of(2, 1));
        JsonNode result = get("/polls/" + pollId
                + "/results?counting_method=ranked_choice", MockApiKeys.CLIENT_A).getBody();
        assertThat(result.get("winning_option_id").asInt()).isEqualTo(1);
        assertThat(result.get("rounds").size()).isEqualTo(2);
        assertThat(result.get("rounds").get(0).get("eliminated_option_id").asInt()).isEqualTo(2);
        assertThat(result.get("counts").get("1").asInt()).isEqualTo(3);
        long otherPoll = createPoll();
        assertThat(get("/polls/" + otherPoll + "/results?counting_method=plurality",
                MockApiKeys.CLIENT_A).getBody().get("total_ballots").asInt()).isZero();
    }

    @Test
    void createThenQueryEmptyPollUsingReturnedIds() {
        long pollId = createPoll();
        JsonNode result = get("/polls/" + pollId + "/results?counting_method=ranked_choice",
                MockApiKeys.CLIENT_A).getBody();
        assertThat(result.get("winning_option_id").isNull()).isTrue();
        assertThat(result.get("total_ballots").asInt()).isZero();
        assertThat(result.get("counts").get("0").asInt()).isZero();
        assertThat(result.get("rounds").isEmpty()).isTrue();
    }

    @Test
    void rejectsUnauthorizedMalformedAndUnknownRequests() {
        String path = "/polls/999999/results?counting_method=plurality";
        assertThat(get(path, null).getStatusCode().value()).isEqualTo(401);
        assertThat(get(path, "wrong").getStatusCode().value()).isEqualTo(401);
        assertThat(get(path, MockApiKeys.CLIENT_A).getStatusCode().value()).isEqualTo(404);
        assertThat(get("/polls/999999/results?counting_method=approval", MockApiKeys.CLIENT_A)
                .getStatusCode().value()).isEqualTo(400);
        assertThat(get("/polls/999999/results", MockApiKeys.CLIENT_A)
                .getStatusCode().value()).isEqualTo(400);
        assertThat(get("/polls/not-a-number/results?counting_method=borda", MockApiKeys.CLIENT_A)
                .getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void rejectedBallotsDoNotChangeResultsAndKeyPolicyIsShared() {
        long pollId = createPoll();
        Map<String, Object> ballot = Map.of("voter_id", "v1", "ranked_option_ids", List.of(0));
        String path = "/polls/" + pollId + "/ballots";
        assertThat(post("/polls", Map.of(), "wrong").getStatusCode().value()).isEqualTo(401);
        assertThat(post(path, ballot, "wrong").getStatusCode().value()).isEqualTo(401);
        assertThat(post(path, ballot, null).getStatusCode().value()).isEqualTo(401);
        cast(pollId, "v1", List.of(0));
        assertThat(post(path, ballot, MockApiKeys.CLIENT_A).getStatusCode().value()).isEqualTo(400);
        assertThat(post(path, Map.of("voter_id", "v2", "ranked_option_ids", List.of(3)),
                MockApiKeys.CLIENT_A).getStatusCode().value()).isEqualTo(400);
        assertThat(get("/polls/" + pollId + "/results?counting_method=plurality",
                MockApiKeys.CLIENT_A).getBody().get("total_ballots").asInt()).isEqualTo(1);
    }
}
