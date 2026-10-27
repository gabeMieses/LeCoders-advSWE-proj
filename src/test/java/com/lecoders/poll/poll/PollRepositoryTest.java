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

    // AI-generated with Claude Code (Opus 5.5); reviewed and tested by Gabriel Mieses.
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

    // AI-generated with Claude Code (Opus 5.5); reviewed and tested by Jason Dixon.
    @Test
    void deleteById_existingPoll_removesItAndReturnsTrue() {
        Poll saved = polls.save(newPoll("Q?"));

        assertThat(polls.deleteById(saved.id())).isTrue();
        assertThat(polls.findById(saved.id())).isEmpty();
    }

    @Test
    void deleteById_unknownPoll_returnsFalse() {
        assertThat(polls.deleteById(99L)).isFalse();
    }

    @Test
    void deleteById_leavesOtherPollsInPlace() {
        Poll kept = polls.save(newPoll("Kept?"));
        Poll deleted = polls.save(newPoll("Deleted?"));

        polls.deleteById(deleted.id());

        assertThat(polls.findById(kept.id())).isPresent();
    }
}
