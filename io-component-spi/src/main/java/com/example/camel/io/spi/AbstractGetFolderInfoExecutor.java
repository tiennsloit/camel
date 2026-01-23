package com.example.camel.io.spi;

import java.util.Map;

/**
 * Base ActionExecutor for folder listing actions. Subclasses implement
 * {@link #getFolderInfo(FolderInfoRequest)}; this base wires it to the
 * dynamic action execution contract.
 */
public abstract class AbstractGetFolderInfoExecutor extends AbstractActionExecutor {
    protected AbstractGetFolderInfoExecutor() {
        super(null, null);
    }

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

    @Override
    public String actionName() {
        String explicit = explicitActionName();
        if (explicit != null) {
            return explicit;
        }
        return deriveActionName(getClass(), providerId());
    }

    private static String deriveActionName(Class<?> clazz, String providerId) {
        String simple = clazz.getSimpleName();
        if (simple.endsWith("Action") && simple.length() > "Action".length()) {
            simple = simple.substring(0, simple.length() - "Action".length());
        }
        if (providerId != null && simple.toLowerCase().startsWith(providerId.toLowerCase())) {
            simple = simple.substring(providerId.length());
        }
        if (simple.isEmpty()) {
            simple = clazz.getSimpleName();
        }
        // lowerCamelCase first character
        if (!simple.isEmpty()) {
            simple = Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
        }
        return simple;
    }
}
