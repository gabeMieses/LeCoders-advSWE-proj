package com.lecoders.poll.results;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lecoders.poll.ApiKeyValidator;
import com.lecoders.poll.Poll;
import com.lecoders.poll.PollRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Tests snapshot isolation, stable IDs and fail-closed authentication. AI-generated. */
class StorageAndAuthTest {
    @Test
    void ballotsAreImmutableSnapshotsAndDeletionDoesNotAffectOtherPolls() {
        InMemoryRankedBallotStore store = new InMemoryRankedBallotStore();
        List<Long> input = new ArrayList<>(List.of(1L, 2L));
        assertThat(store.findRankings(1)).isEmpty();
        store.append(1, input);
        final List<List<Long>> snapshot = store.findRankings(1);
        input.clear();
        store.append(1, List.of(2L));
        store.append(2, List.of(1L));
        assertThat(snapshot).containsExactly(List.of(1L, 2L));
        assertThat(store.findRankings(1)).hasSize(2);
        assertThatThrownBy(() -> snapshot.add(List.of(1L)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.get(0).add(3L))
                .isInstanceOf(UnsupportedOperationException.class);
        store.deleteByPollId(1);
        assertThat(store.findRankings(1)).isEmpty();
        assertThat(store.findRankings(2)).hasSize(1);
    }

    @Test
    void pollOptionOrderCannotBeChangedByCaller() {
        List<String> labels = new ArrayList<>(List.of("A", "B"));
        PollRepository repository = new PollRepository();
        Poll poll = repository.save(new Poll(null, "Question", labels, "key", "borda"));
        labels.clear();
        assertThat(poll.optionIds()).containsExactly(1L, 2L);
        assertThat(poll.options()).containsExactly("A", "B");
        assertThat(repository.save(poll).id()).isEqualTo(poll.id());
        assertThat(repository.findById(poll.id())).contains(poll);
    }

    @Test
    void noConfiguredKeysMeansNoAccess() {
        ApiKeyValidator empty = new ApiKeyValidator("");
        assertThatThrownBy(() -> empty.requireValid("anything"))
                .isInstanceOf(ResponseStatusException.class);
        ApiKeyValidator configured = new ApiKeyValidator(" first, second, ,first ");
        configured.requireValid("first");
        configured.requireValid("second");
        for (String key : new String[] {null, "", " first", "wrong"}) {
            assertThatThrownBy(() -> configured.requireValid(key))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }
}
