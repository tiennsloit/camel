package com.example.camel.io.spi;

import java.util.Objects;

/**
 * Simple folder metadata representation shared by all IO components.
 */
public final class FolderInfoItem {
    private final String id;
    private final String name;
    private final String path;
    private final boolean hasChildren;

    public FolderInfoItem(String id, String name, String path, boolean hasChildren) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.path = Objects.requireNonNull(path, "path");
        this.hasChildren = hasChildren;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String path() {
        return path;
    }

    public boolean hasChildren() {
        return hasChildren;
    }
}
