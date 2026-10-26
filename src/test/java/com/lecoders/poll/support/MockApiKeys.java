package com.lecoders.poll.support;

/**
 * Fake API keys and the API key header name, shared by tests that call the poll endpoints.
 */
public final class MockApiKeys {

    /** Name of the request header that carries a client program's API key. */
    public static final String HEADER = "X-API-Key";

    /** Key for the client program that creates and votes on polls in most tests. */
    public static final String CLIENT_A = "test-key-client-a";

    /** Key for a second client program, for tests that need two different callers. */
    public static final String CLIENT_B = "test-key-client-b";

    /** A key made only of whitespace, which the service must treat as missing. */
    public static final String BLANK = "   ";

    private MockApiKeys() {
    }
}
