package com.lecoders.poll.ballot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lecoders.poll.ApiKeyValidator;
import com.lecoders.poll.BallotRepository;
import com.lecoders.poll.PollController;
import com.lecoders.poll.PollRepository;
import com.lecoders.poll.support.MockApiKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = PollController.class,
        properties = "poll.api-keys=" + MockApiKeys.CLIENT_A + "," + MockApiKeys.CLIENT_B)
@Import({PollRepository.class, BallotRepository.class, ApiKeyValidator.class})
class CastBallotControllerTest {

    @Autowired
    private MockMvc mvc;

    private long pollId;

    /** Creates a fresh three-option poll (option ids 0, 1, 2) so tests do not share voters. */
    @BeforeEach
    void createPoll() throws Exception {
        String body = mvc.perform(post("/polls")
                        .header(MockApiKeys.HEADER, MockApiKeys.CLIENT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "Next map?",
                                 "options": ["Dust", "Inferno", "Nuke"],
                                 "counting_method": "borda"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        pollId = ((Number) JsonPath.read(body, "$.poll_id")).longValue();
    }

    private ResultActions castBallot(long poll, String apiKey, String body) throws Exception {
        var request = post("/polls/{pollId}/ballots", poll)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (apiKey != null) {
            request.header(MockApiKeys.HEADER, apiKey);
        }
        return mvc.perform(request);
    }

    private ResultActions castBallot(String body) throws Exception {
        return castBallot(pollId, MockApiKeys.CLIENT_A, body);
    }

    @Test
    void castBallot_validFullRanking_returns201WithBallotId() throws Exception {
        castBallot("""
                {"voter_id": "v1", "ranked_option_ids": [2, 0, 1]}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ballot_id").isNumber());
    }

    @Test
    void castBallot_partialRanking_returns201() throws Exception {
        castBallot("""
                {"voter_id": "v1", "ranked_option_ids": [1]}
                """)
                .andExpect(status().isCreated());
    }

    @Test
    void castBallot_differentClientKey_returns201() throws Exception {
        castBallot(pollId, MockApiKeys.CLIENT_B, """
                {"voter_id": "v1", "ranked_option_ids": [0]}
                """)
                .andExpect(status().isCreated());
    }

    @Test
    void castBallot_missingApiKey_returns401() throws Exception {
        castBallot(pollId, null, """
                {"voter_id": "v1", "ranked_option_ids": [0]}
                """)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void castBallot_blankApiKey_returns401() throws Exception {
        castBallot(pollId, MockApiKeys.BLANK, """
                {"voter_id": "v1", "ranked_option_ids": [0]}
                """)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void castBallot_unknownPoll_returns404() throws Exception {
        castBallot(pollId + 1000, MockApiKeys.CLIENT_A, """
                {"voter_id": "v1", "ranked_option_ids": [0]}
                """)
                .andExpect(status().isNotFound());
    }

    @Test
    void castBallot_missingVoterId_returns400() throws Exception {
        castBallot("""
                {"ranked_option_ids": [0]}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void castBallot_blankVoterId_returns400() throws Exception {
        castBallot("""
                {"voter_id": " ", "ranked_option_ids": [0]}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void castBallot_missingRanking_returns400() throws Exception {
        castBallot("""
                {"voter_id": "v1"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void castBallot_emptyRanking_returns400() throws Exception {
        castBallot("""
                {"voter_id": "v1", "ranked_option_ids": []}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void castBallot_optionIdTooLarge_returns400() throws Exception {
        castBallot("""
                {"voter_id": "v1", "ranked_option_ids": [3]}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void castBallot_negativeOptionId_returns400() throws Exception {
        castBallot("""
                {"voter_id": "v1", "ranked_option_ids": [-1]}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void castBallot_nullOptionId_returns400() throws Exception {
        castBallot("""
                {"voter_id": "v1", "ranked_option_ids": [0, null]}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void castBallot_duplicateOptionId_returns400() throws Exception {
        castBallot("""
                {"voter_id": "v1", "ranked_option_ids": [1, 0, 1]}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void castBallot_voterAlreadyVoted_returns400() throws Exception {
        castBallot("""
                {"voter_id": "v1", "ranked_option_ids": [0]}
                """)
                .andExpect(status().isCreated());

        castBallot("""
                {"voter_id": "v1", "ranked_option_ids": [1]}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void castBallot_rejectedBallot_doesNotBlockLaterValidVote() throws Exception {
        castBallot("""
                {"voter_id": "v1", "ranked_option_ids": [9]}
                """)
                .andExpect(status().isBadRequest());

        castBallot("""
                {"voter_id": "v1", "ranked_option_ids": [0]}
                """)
                .andExpect(status().isCreated());
    }
}
