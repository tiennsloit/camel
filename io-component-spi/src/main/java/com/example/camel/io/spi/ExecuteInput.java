package com.example.camel.io.spi;

import java.io.File;
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
    private final Map<String, File> files;

    public ExecuteInput(String providerId, String actionName, Map<String, Object> parameters) {
        this(providerId, actionName, parameters, Collections.emptyMap());
    }

    public ExecuteInput(String providerId, String actionName, Map<String, Object> parameters, Map<String, File> files) {
        this.providerId = providerId;
        this.actionName = actionName;
        this.parameters = parameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(parameters));
        this.files = files == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(files));
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

    public Map<String, File> files() {
        return files;
    }
}
