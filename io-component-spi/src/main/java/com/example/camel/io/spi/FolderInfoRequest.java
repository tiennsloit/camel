package com.example.camel.io.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Request for folder info. The {@code folderId} can be null to indicate root listing.
 */
public final class FolderInfoRequest {
    private final String folderId;
    private final Map<String, String> options;

    public FolderInfoRequest(String folderId, Map<String, String> options) {
        this.folderId = folderId;
        this.options = options == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(options));
    }

    public String folderId() {
        return folderId;
    }

    public Map<String, String> options() {
        return options;
    }

    public FolderInfoRequest withOption(String key, String value) {
        Objects.requireNonNull(key, "key");
        Map<String, String> copy = new HashMap<>(options);
        copy.put(key, value);
        return new FolderInfoRequest(folderId, copy);
    }
}
