package com.example.camel.io.spi;

import java.util.Map;

/**
 * Base ActionExecutor for folder listing actions. Subclasses implement
 * {@link #getFolderInfo(FolderInfoRequest)}; this base wires it to the
 * dynamic action execution contract.
 */
public abstract class AbstractGetFolderInfoExecutor extends AbstractActionExecutor {
    protected AbstractGetFolderInfoExecutor(String providerId, String actionName) {
        super(providerId, actionName);
    }

    @Override
    public Object execute(ExecuteInput input) throws Exception {
        Map<String, Object> params = mergedWithAttachedParams(input.parameters());
        String folderId = params.containsKey("folderId") ? String.valueOf(params.get("folderId")) : null;
        return getFolderInfo(new FolderInfoRequest(folderId, null));
    }

    public abstract FolderInfoResponse getFolderInfo(FolderInfoRequest request) throws Exception;
}
