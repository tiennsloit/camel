package com.example.camel.io.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal auth token holder for tests/local execution.
 */
public final class AuthTokens {
    private static final String HEADER_NAME = "X-Auth-Tokens";
    private final Map<String, TokenEntry> tokens = new HashMap<>();

    public void addTokens(String provider, Map<String, String> tokenMap, Map<String, Object> attachedParams) {
        tokens.put(provider, new TokenEntry(new HashMap<>(tokenMap), attachedParams == null ? Map.of() : new HashMap<>(attachedParams)));
    }

    public Map<String, TokenEntry> tokens() {
        return Collections.unmodifiableMap(tokens);
    }

    public static String getHeaderName() {
        return HEADER_NAME;
    }

    public record TokenEntry(Map<String, String> tokens, Map<String, Object> attachedParams) {}
}
