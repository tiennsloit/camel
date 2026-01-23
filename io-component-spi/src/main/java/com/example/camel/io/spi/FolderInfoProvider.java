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
    default String id() {
        return deriveProviderId(getClass());
    }

    /**
     * Return folder metadata for the requested folder.
     * Passing a null or empty folderId should return root/top-level folders.
     */
    FolderInfoResponse getFolderInfo(FolderInfoRequest request) throws Exception;

    private static String deriveProviderId(Class<?> clazz) {
        String pkg = clazz.getPackageName();
        int lastDot = pkg.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < pkg.length()) {
            return pkg.substring(lastDot + 1);
        }
        return pkg;
    }
}
