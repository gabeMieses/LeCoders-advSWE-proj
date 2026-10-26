package com.lecoders.poll.results;

/**
 * Supported, case-sensitive wire values.
 *
 */
public enum CountingMethod {
    PLURALITY("plurality"),
    RANKED_CHOICE("ranked_choice"),
    BORDA("borda");

    private final String wireName;

    CountingMethod(String wireName) {
        this.wireName = wireName;
    }

    /** Returns the API spelling. */
    public String wireName() {
        return wireName;
    }

    /** Rejects missing or unsupported methods instead of choosing an implicit default. */
    public static CountingMethod parse(String value) {
        for (CountingMethod method : values()) {
            if (method.wireName.equals(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unsupported counting_method");
    }
}
