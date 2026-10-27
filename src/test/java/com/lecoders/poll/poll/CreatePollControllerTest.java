package com.lecoders.poll.poll;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lecoders.poll.ApiKeyValidator;
import com.lecoders.poll.BallotRepository;
import com.lecoders.poll.PollController;
import com.lecoders.poll.PollRepository;
import com.lecoders.poll.support.MockApiKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// AI-generated with Claude Code (Opus 5.5); reviewed and tested by Gabriel Mieses.
@WebMvcTest(PollController.class)
// AI-assisted integration: import the shared key validator used by all endpoints.
@Import({PollRepository.class, BallotRepository.class, ApiKeyValidator.class})
class CreatePollControllerTest {

    private static final String VALID_POLL = """
            {"question": "Next map?",
             "options": ["Dust", "Inferno", "Nuke"],
             "counting_method": "plurality"}
            """;

    @Autowired
    private MockMvc mvc;

    private ResultActions createPoll(String apiKey, String body) throws Exception {
        var request = post("/polls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (apiKey != null) {
            request.header(MockApiKeys.HEADER, apiKey);
        }
        return mvc.perform(request);
    }

    private ResultActions createPoll(String body) throws Exception {
        return createPoll(MockApiKeys.CLIENT_A, body);
    }

    @Test
    void createPoll_valid_returns201WithPollIdAndOptionIds() throws Exception {
        createPoll(VALID_POLL)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.poll_id").isNumber())
                .andExpect(jsonPath("$.option_ids.length()").value(3))
                .andExpect(jsonPath("$.option_ids[0]").value(0))
                .andExpect(jsonPath("$.option_ids[2]").value(2));
    }

    @Test
    void createPoll_eachSupportedCountingMethod_returns201() throws Exception {
        for (String method : new String[] {"plurality", "ranked_choice", "borda"}) {
            createPoll("""
                    {"question": "Q?", "options": ["A", "B"], "counting_method": "%s"}
                    """.formatted(method))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void createPoll_secondClientKey_returns201() throws Exception {
        createPoll(MockApiKeys.CLIENT_B, VALID_POLL).andExpect(status().isCreated());
    }

    @Test
    void createPoll_missingApiKey_returns401() throws Exception {
        createPoll(null, VALID_POLL).andExpect(status().isUnauthorized());
    }

    @Test
    void createPoll_blankApiKey_returns401() throws Exception {
        createPoll(MockApiKeys.BLANK, VALID_POLL).andExpect(status().isUnauthorized());
    }

    @Test
    void createPoll_unknownApiKey_returns401() throws Exception {
        createPoll("not-a-real-key", VALID_POLL).andExpect(status().isUnauthorized());
    }

    @Test
    void createPoll_missingQuestion_returns400() throws Exception {
        createPoll("""
                {"options": ["A", "B"], "counting_method": "borda"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPoll_blankQuestion_returns400() throws Exception {
        createPoll("""
                {"question": " ", "options": ["A", "B"], "counting_method": "borda"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPoll_missingOptions_returns400() throws Exception {
        createPoll("""
                {"question": "Q?", "counting_method": "borda"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPoll_oneOption_returns400() throws Exception {
        createPoll("""
                {"question": "Q?", "options": ["A"], "counting_method": "borda"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPoll_blankOption_returns400() throws Exception {
        createPoll("""
                {"question": "Q?", "options": ["A", " "], "counting_method": "borda"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPoll_nullOption_returns400() throws Exception {
        createPoll("""
                {"question": "Q?", "options": ["A", null], "counting_method": "borda"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPoll_duplicateOptions_returns400() throws Exception {
        createPoll("""
                {"question": "Q?", "options": ["A", "A"], "counting_method": "borda"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPoll_missingCountingMethod_returns400() throws Exception {
        createPoll("""
                {"question": "Q?", "options": ["A", "B"]}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPoll_blankCountingMethod_returns400() throws Exception {
        createPoll("""
                {"question": "Q?", "options": ["A", "B"], "counting_method": " "}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPoll_unsupportedCountingMethod_returns400() throws Exception {
        createPoll("""
                {"question": "Q?", "options": ["A", "B"], "counting_method": "ranked_choiced"}
                """)
                .andExpect(status().isBadRequest());
    }
}
