package com.example.camel.io.onedrive;

import com.example.camel.io.spi.AbstractActionExecutor;
import com.example.camel.io.spi.AuthTokens;
import com.example.camel.io.spi.ExecuteInput;

import java.util.HashMap;
import java.util.Map;

/**
 * Stub action executor for posting a file to OneDrive.
 * Replace with real upload logic; currently returns a dummy URL.
 */
public class OneDrivePostFileAction extends AbstractActionExecutor {
    public OneDrivePostFileAction() {
        super();
    }

    @Override
    public Object execute(ExecuteInput input) throws Exception {
        Map<String, Object> params = mergedWithAttachedParams(input.parameters());
        String fileName = params.getOrDefault("fileName", "unknown").toString();
        String baseUrl = resolveStringParam(params, "url", "not valid url");
        String accessToken = OneDriveApi.resolveAccessToken(params);
        // pick first file if provided
        java.io.File file = input.files().values().stream().findFirst().orElse(null);
        String fileNameFromFile = file != null ? file.getName() : fileName;
        String parentPath = params.getOrDefault("parentPath", "").toString();

        if (accessToken == null) {
            throw new IllegalStateException("Missing access_token for graphUpload");
        }
        if (file == null) {
            throw new IllegalStateException("Missing file for graphUpload");
        }
        Map<String, Object> graphResp = OneDriveApi.uploadSmallFile(accessToken, parentPath, file);
        graphResp.put("providerId", providerId());
        graphResp.put("action", actionName());
        graphResp.put("access_token", accessToken);
        return graphResp;
    }
}
