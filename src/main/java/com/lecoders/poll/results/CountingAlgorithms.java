package com.lecoders.poll.results;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Pure counting algorithms with deterministic numeric-ID ties.
 *
 */
public final class CountingAlgorithms {
    private CountingAlgorithms() {}

    /**
     * Counts a validated snapshot without modifying its options or ballots.
     * Partial rankings are allowed; empty, duplicate and unknown rankings are rejected.
     */
    public static CountResult count(
            List<Long> options, List<List<Long>> ballots, CountingMethod method) {
        validate(options, ballots);
        if (method == null) {
            throw new IllegalArgumentException("counting_method is required");
        }
        if (ballots.isEmpty()) {
            return new CountResult(null, zeroCounts(options), 0, List.of());
        }
        if (method == CountingMethod.RANKED_CHOICE) {
            return rankedChoice(options, ballots);
        }
        Map<Long, Long> counts = zeroCounts(options);
        for (List<Long> ballot : ballots) {
            if (method == CountingMethod.PLURALITY) {
                counts.merge(ballot.get(0), 1L, Long::sum);
            } else {
                for (int rank = 0; rank < ballot.size(); rank++) {
                    counts.merge(ballot.get(rank), (long) options.size() - rank - 1, Long::sum);
                }
            }
        }
        return new CountResult(winner(counts), counts, ballots.size(), List.of());
    }

    private static void validate(List<Long> options, List<List<Long>> ballots) {
        if (options == null || options.size() < 2
                || options.stream().anyMatch(java.util.Objects::isNull)
                || new HashSet<>(options).size() != options.size()) {
            throw new IllegalArgumentException(
                    "At least two distinct non-null option IDs required");
        }
        if (ballots == null) {
            throw new IllegalArgumentException("Ballots are required");
        }
        for (List<Long> ballot : ballots) {
            if (ballot == null || ballot.isEmpty()
                    || ballot.stream().anyMatch(java.util.Objects::isNull)
                    || !options.containsAll(ballot)
                    || new HashSet<>(ballot).size() != ballot.size()) {
                throw new IllegalArgumentException("Invalid stored ranking");
            }
        }
    }

    private static Map<Long, Long> zeroCounts(Iterable<Long> options) {
        Map<Long, Long> counts = new TreeMap<>();
        for (Long option : options) {
            counts.put(option, 0L);
        }
        return counts;
    }

    // The lowest numeric ID wins an equal highest score, independent of input order.
    private static Long winner(Map<Long, Long> counts) {
        return counts.keySet().stream()
                .min(Comparator.<Long>comparingLong(counts::get).reversed()
                        .thenComparingLong(Long::longValue)).orElseThrow();
    }

    private static CountResult rankedChoice(List<Long> options, List<List<Long>> ballots) {
        TreeSet<Long> active = new TreeSet<>(options);
        List<CountResult.Round> rounds = new ArrayList<>();
        while (true) {
            Map<Long, Long> counts = zeroCounts(active);
            long exhausted = 0;
            for (List<Long> ballot : ballots) {
                Long choice = ballot.stream().filter(active::contains).findFirst().orElse(null);
                if (choice == null) {
                    exhausted++;
                } else {
                    counts.merge(choice, 1L, Long::sum);
                }
            }
            long activeBallots = ballots.size() - exhausted;
            Long leader = winner(counts);
            if (activeBallots == 0 || counts.get(leader) > activeBallots / 2) {
                rounds.add(new CountResult.Round(
                        rounds.size() + 1, counts, activeBallots, exhausted, null));
                return new CountResult(
                        activeBallots == 0 ? null : leader, counts, ballots.size(), rounds);
            }
            // On equal lowest counts, eliminate the highest ID, preserving lower-ID preference.
            Long eliminated = active.stream()
                    .min(Comparator.<Long>comparingLong(counts::get)
                            .thenComparing(Comparator.reverseOrder())).orElseThrow();
            rounds.add(new CountResult.Round(
                    rounds.size() + 1, counts, activeBallots, exhausted, eliminated));
            active.remove(eliminated);
        }
    }
}
