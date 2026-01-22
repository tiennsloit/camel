package com.example.camel.io.spi;

import java.util.Map;

/**
 * Generic action executor for dynamic plugin actions.
 * Implementations must be registered via META-INF/services/com.example.camel.io.spi.ActionExecutor
 * inside the plugin JAR.
 */
public interface ActionExecutor {
    /**
     * The provider id this action belongs to (e.g., "onedrive").
     */
    String providerId();

    /**
     * The logical action name (may start with "_" if meant to override Execute).
     */
    String actionName();

    /**
     * Execute the action with the given input.
     */
    Object execute(ExecuteInput input) throws Exception;

    /**
     * Convenience: merge attached params from AuthTokens into a new Map (does not mutate the input).
     */
    default Map<String, Object> mergedWithAttachedParams(Map<String, Object> params) {
        return ParamUtils.mergedWithAttachedParams(params);
    }

    /**
     * Convenience: resolve a string param, optionally from attached params in AuthTokens.
     */
    default String resolveStringParam(Map<String, Object> params, String key, String defaultValue) {
        return ParamUtils.resolveStringParam(params, key, defaultValue);
    }
}
