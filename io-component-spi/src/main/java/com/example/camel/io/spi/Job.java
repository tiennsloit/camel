package com.example.camel.io.spi;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple job envelope for tests and local execution.
 */
public final class Job {
    private final Map<String, Object> parameters = new HashMap<>();
    private final Map<String, Object> headers = new HashMap<>();
    private final Map<String, File> files = new HashMap<>();

    public void addParameter(String name, Object value) {
        parameters.put(name, value);
    }

    public void addHeader(String name, Object value) {
        headers.put(name, value);
    }

    public void addFile(String name, File file) {
        files.put(name, file);
    }

    public Map<String, Object> parameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public Map<String, Object> headers() {
        return Collections.unmodifiableMap(headers);
    }

    public Map<String, File> files() {
        return Collections.unmodifiableMap(files);
    }
}
