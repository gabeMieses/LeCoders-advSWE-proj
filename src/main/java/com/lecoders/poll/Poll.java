package com.lecoders.poll;

import java.util.List;

/**
 * A poll created by a client program.
 *
 * @param id unique poll id, or null before the poll is saved
 * @param question the question the poll asks
 * @param options the answer options voters can rank
 * @param apiKey API key of the client program that created the poll
 * @param countingMethod the counting method requested for the poll
 */
public record Poll(
        Long id, String question, List<String> options, String apiKey, String countingMethod) {
}
