package com.example.camel.io.spi;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Response envelope used by all IO components.
 */
public final class FolderInfoResponse {
    private final String providerId;
    private final String parentFolderId;
    private final List<FolderInfoItem> folders;

    public FolderInfoResponse(String providerId, String parentFolderId, List<FolderInfoItem> folders) {
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.parentFolderId = parentFolderId;
        this.folders = Collections.unmodifiableList(folders == null ? List.of() : List.copyOf(folders));
    }

    public String providerId() {
        return providerId;
    }

    public String parentFolderId() {
        return parentFolderId;
    }

    public List<FolderInfoItem> folders() {
        return folders;
    }
}
