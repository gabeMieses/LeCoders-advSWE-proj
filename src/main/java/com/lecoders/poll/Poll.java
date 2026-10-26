package com.lecoders.poll;

import java.util.List;

public record Poll(
    Long id, String question, List<String> options, String apiKey, String countingMethod) {}
