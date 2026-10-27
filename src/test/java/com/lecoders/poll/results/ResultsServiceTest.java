package com.lecoders.poll.results;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lecoders.poll.Poll;
import com.lecoders.poll.PollRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/**
 * Mocked repository tests distinguish bad requests from corrupt storage.
 *
 * <p>AI-generated with OpenAI Codex; integration tested by Haoran (@lek1ma).
 */
@ExtendWith(MockitoExtension.class)
class ResultsServiceTest {
    @Mock
    private PollRepository polls;
    @Mock
    private RankedBallotStore ballots;
    private ResultsService service;

    @BeforeEach
    void setUp() {
        service = new ResultsService(polls, ballots);
    }

    @Test
    void rejectsMethodBeforeQueryingStorage() {
        assertThatThrownBy(() -> service.results(77, "bad"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(polls, ballots);
    }

    @Test
    void unknownPollIsNotAnEmptyElection() {
        when(polls.findById(77)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.results(77, "plurality"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(404));
        verifyNoInteractions(ballots);
    }

    @Test
    void readsActualStoredRankingsAndHonorsRequestedMethod() {
        when(polls.findById(77)).thenReturn(Optional.of(
                new Poll(77L, "Question", List.of("A", "B"), "secret", "plurality")));
        when(ballots.findRankings(77)).thenReturn(List.of(List.of(1L, 0L)));
        ResultsService.ResultResponse result = service.results(77, "borda");
        assertThat(result.countingMethod()).isEqualTo("borda");
        assertThat(result.winningOptionId()).isEqualTo(1L);
        assertThat(result.totalBallots()).isEqualTo(1);
    }

    @Test
    void corruptStorageReturnsServerError() {
        when(polls.findById(77)).thenReturn(Optional.of(
                new Poll(77L, "Question", List.of("A", "B"), "secret", "plurality")));
        when(ballots.findRankings(77)).thenReturn(List.of(List.of(999L)));
        assertThatThrownBy(() -> service.results(77, "borda"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(500));
    }
}
