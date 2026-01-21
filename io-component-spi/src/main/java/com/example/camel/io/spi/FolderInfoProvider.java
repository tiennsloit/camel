package com.example.camel.io.spi;

/**
 * Contract implemented by each IO component to expose folder listing.
 * Implementations must be registered via ServiceLoader
 * (META-INF/services/com.example.camel.io.spi.FolderInfoProvider).
 */
public interface FolderInfoProvider {
    /**
        * Unique provider identifier, e.g. "onedrive", "gdrive", "dropbox".
        */
    String id();

    /**
     * Return folder metadata for the requested folder.
     * Passing a null or empty folderId should return root/top-level folders.
     */
    FolderInfoResponse getFolderInfo(FolderInfoRequest request) throws Exception;
}
