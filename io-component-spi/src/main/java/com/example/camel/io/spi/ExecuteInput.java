package com.example.camel.io.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Input passed to dynamic action executors.
 */
public final class ExecuteInput {
    private final String providerId;
    private final String actionName;
    private final Map<String, Object> parameters;

    public ExecuteInput(String providerId, String actionName, Map<String, Object> parameters) {
        this.providerId = providerId;
        this.actionName = actionName;
        this.parameters = parameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(parameters));
    }

    public String providerId() {
        return providerId;
    }

    public String actionName() {
        return actionName;
    }

    public Map<String, Object> parameters() {
        return parameters;
    }
}
