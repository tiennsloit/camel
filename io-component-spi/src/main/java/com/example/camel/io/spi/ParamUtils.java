package com.example.camel.io.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility helpers for parameter handling across IO components.
 */
public final class ParamUtils {
    private ParamUtils() {}

    /**
    * Merge attachedParams from AuthTokens header (if present) into a new map.
    * This does not mutate the source map.
    */
    public static Map<String, Object> mergedWithAttachedParams(Map<String, Object> params) {
        Map<String, Object> merged = new HashMap<>(params);
        Object auth = params.get(AuthTokens.getHeaderName());
        if (auth instanceof AuthTokens tokens) {
            tokens.tokens().values().forEach(entry -> {
                // expose tokens (e.g., access_token) if not already present
                entry.tokens().forEach(merged::putIfAbsent);
                entry.attachedParams().forEach(merged::putIfAbsent);
            });
        }
        return merged;
    }

    /**
    * Resolve a string parameter by key, optionally looking into attachedParams from AuthTokens.
    */
    public static String resolveStringParam(Map<String, Object> params, String key, String defaultValue) {
        Object direct = params.get(key);
        if (direct != null) {
            return direct.toString();
        }
        Object auth = params.get(AuthTokens.getHeaderName());
        if (auth instanceof AuthTokens tokens) {
            return tokens.tokens().values().stream()
                    .map(entry -> entry.attachedParams().get(key))
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .findFirst()
                    .orElse(defaultValue);
        }
        return defaultValue;
    }
}
