package com.lecoders.poll.poll;

import static org.assertj.core.api.Assertions.assertThat;

import com.lecoders.poll.Poll;
import com.lecoders.poll.PollRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PollRepositoryTest {

    private PollRepository polls;

    @BeforeEach
    void setUp() {
        polls = new PollRepository();
    }

    private static Poll newPoll(String question) {
        return new Poll(null, question, List.of("A", "B"), "key", "borda");
    }

    @Test
    void save_newPoll_assignsIdAndKeepsFields() {
        Poll saved = polls.save(newPoll("Q?"));

        assertThat(saved.id()).isEqualTo(1L);
        assertThat(saved.question()).isEqualTo("Q?");
        assertThat(saved.options()).containsExactly("A", "B");
        assertThat(saved.apiKey()).isEqualTo("key");
        assertThat(saved.countingMethod()).isEqualTo("borda");
    }

    @Test
    void save_assignsIncreasingIds() {
        Poll first = polls.save(newPoll("First?"));
        Poll second = polls.save(newPoll("Second?"));

        assertThat(second.id()).isGreaterThan(first.id());
    }

    @Test
    void save_pollWithId_keepsThatId() {
        Poll saved = polls.save(new Poll(42L, "Q?", List.of("A", "B"), "key", "borda"));

        assertThat(saved.id()).isEqualTo(42L);
    }
}
