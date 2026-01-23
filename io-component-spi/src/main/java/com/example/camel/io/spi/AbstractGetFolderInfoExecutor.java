package com.example.camel.io.spi;

import java.util.Map;

/**
 * Base ActionExecutor for folder listing actions. It wraps a FolderInfoProvider
 * and exposes it through the dynamic action mechanism.
 */
public abstract class AbstractGetFolderInfoExecutor extends AbstractActionExecutor {
    private final FolderInfoProvider provider;

    protected AbstractGetFolderInfoExecutor(String providerId, String actionName, FolderInfoProvider provider) {
        super(providerId, actionName);
        this.provider = provider;
    }

    @Override
    public Object execute(ExecuteInput input) throws Exception {
        Map<String, Object> params = mergedWithAttachedParams(input.parameters());
        String folderId = params.containsKey("folderId") ? String.valueOf(params.get("folderId")) : null;
        return provider.getFolderInfo(new FolderInfoRequest(folderId, null));
    }
}
