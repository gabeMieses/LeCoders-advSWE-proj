package com.lecoders.poll.delete;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lecoders.poll.BallotRepository;
import com.lecoders.poll.Poll;
import com.lecoders.poll.PollController;
import com.lecoders.poll.PollRepository;
import com.lecoders.poll.support.MockApiKeys;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Unit tests for DELETE /polls/{pollId} with both repositories replaced by Mockito mocks,
 * so each test controls exactly which poll exists and checks which deletes were called.
 *
 * <p>AI-generated with Claude Code (Opus 5.5); reviewed and tested by Jason Dixon.
 */
@WebMvcTest(PollController.class)
class DeletePollControllerTest {

    private static final long POLL_ID = 5L;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PollRepository polls;

    @MockitoBean
    private BallotRepository ballots;

    /** Every test starts with poll 5, owned by client A, holding 3 ballots. */
    @BeforeEach
    void stubPollOwnedByClientA() {
        Poll poll = new Poll(POLL_ID, "Next map?", List.of("Dust", "Inferno"),
                MockApiKeys.CLIENT_A, "plurality");
        when(polls.findById(POLL_ID)).thenReturn(Optional.of(poll));
        when(polls.deleteById(POLL_ID)).thenReturn(true);
        when(ballots.deleteByPollId(POLL_ID)).thenReturn(3);
    }

    private ResultActions deletePoll(long pollId, String apiKey) throws Exception {
        var request = delete("/polls/{pollId}", pollId);
        if (apiKey != null) {
            request.header(MockApiKeys.HEADER, apiKey);
        }
        return mvc.perform(request);
    }

    private void verifyNothingDeleted() {
        verify(polls, never()).deleteById(anyLong());
        verify(ballots, never()).deleteByPollId(anyLong());
    }

    @Test
    void deletePoll_creatorKey_returns200WithPollIdAndBallotCount() throws Exception {
        deletePoll(POLL_ID, MockApiKeys.CLIENT_A)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poll_id").value(POLL_ID))
                .andExpect(jsonPath("$.ballots_deleted").value(3));
    }

    @Test
    void deletePoll_creatorKey_deletesPollBeforeItsBallots() throws Exception {
        deletePoll(POLL_ID, MockApiKeys.CLIENT_A);

        InOrder order = inOrder(polls, ballots);
        order.verify(polls).deleteById(POLL_ID);
        order.verify(ballots).deleteByPollId(POLL_ID);
    }

    @Test
    void deletePoll_otherClientKey_returns401AndDeletesNothing() throws Exception {
        deletePoll(POLL_ID, MockApiKeys.CLIENT_B)
                .andExpect(status().isUnauthorized());

        verifyNothingDeleted();
    }

    @Test
    void deletePoll_missingApiKey_returns401AndDeletesNothing() throws Exception {
        deletePoll(POLL_ID, null)
                .andExpect(status().isUnauthorized());

        verifyNothingDeleted();
    }

    @Test
    void deletePoll_blankApiKey_returns401() throws Exception {
        deletePoll(POLL_ID, MockApiKeys.BLANK)
                .andExpect(status().isUnauthorized());

        verifyNothingDeleted();
    }

    @Test
    void deletePoll_unknownApiKey_returns401WithoutLookingUpPoll() throws Exception {
        deletePoll(POLL_ID, "not-a-real-key")
                .andExpect(status().isUnauthorized());

        verify(polls, never()).findById(anyLong());
        verifyNothingDeleted();
    }

    @Test
    void deletePoll_unknownPoll_returns404() throws Exception {
        when(polls.findById(99L)).thenReturn(Optional.empty());

        deletePoll(99L, MockApiKeys.CLIENT_A)
                .andExpect(status().isNotFound());

        verifyNothingDeleted();
    }

    @Test
    void deletePoll_nonNumericPollId_returns400() throws Exception {
        mvc.perform(delete("/polls/abc").header(MockApiKeys.HEADER, MockApiKeys.CLIENT_A))
                .andExpect(status().isBadRequest());

        verifyNothingDeleted();
    }
}
