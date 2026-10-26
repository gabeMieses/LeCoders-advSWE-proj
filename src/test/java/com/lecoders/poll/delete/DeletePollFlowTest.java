package com.lecoders.poll.delete;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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

/**
 * Tests DELETE /polls/{pollId} together with the other endpoints and the real in-memory
 * repositories, to check that a deleted poll is really gone for every later call.
 */
@WebMvcTest(PollController.class)
@Import({PollRepository.class, BallotRepository.class})
class DeletePollFlowTest {

    @Autowired
    private MockMvc mvc;

    private long pollId;

    /** Creates a fresh poll owned by client A, with two ballots already cast on it. */
    @BeforeEach
    void createPollWithTwoBallots() throws Exception {
        String body = mvc.perform(post("/polls")
                        .header(MockApiKeys.HEADER, MockApiKeys.CLIENT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "Next map?",
                                 "options": ["Dust", "Inferno", "Nuke"],
                                 "counting_method": "plurality"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        pollId = ((Number) JsonPath.read(body, "$.poll_id")).longValue();

        castBallot("v1").andExpect(status().isCreated());
        castBallot("v2").andExpect(status().isCreated());
    }

    private ResultActions castBallot(String voterId) throws Exception {
        return mvc.perform(post("/polls/{pollId}/ballots", pollId)
                .header(MockApiKeys.HEADER, MockApiKeys.CLIENT_A)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"voter_id\": \"" + voterId + "\", \"ranked_option_ids\": [0]}"));
    }

    private ResultActions deletePoll(String apiKey) throws Exception {
        return mvc.perform(delete("/polls/{pollId}", pollId)
                .header(MockApiKeys.HEADER, apiKey));
    }

    @Test
    void deletePoll_removesPollAndReportsItsBallots() throws Exception {
        deletePoll(MockApiKeys.CLIENT_A)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poll_id").value(pollId))
                .andExpect(jsonPath("$.ballots_deleted").value(2));
    }

    @Test
    void deletePoll_thenCastBallot_returns404() throws Exception {
        deletePoll(MockApiKeys.CLIENT_A).andExpect(status().isOk());

        castBallot("v3").andExpect(status().isNotFound());
    }

    @Test
    void deletePoll_twice_secondCallReturns404() throws Exception {
        deletePoll(MockApiKeys.CLIENT_A).andExpect(status().isOk());

        deletePoll(MockApiKeys.CLIENT_A).andExpect(status().isNotFound());
    }

    @Test
    void deletePoll_byOtherClient_leavesPollUsable() throws Exception {
        deletePoll(MockApiKeys.CLIENT_B).andExpect(status().isUnauthorized());

        castBallot("v3").andExpect(status().isCreated());
        deletePoll(MockApiKeys.CLIENT_A)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ballots_deleted").value(3));
    }
}
