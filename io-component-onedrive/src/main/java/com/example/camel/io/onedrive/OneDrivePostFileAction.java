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
    public Object execute(ExecuteInput input) {
        Map<String, Object> params = mergedWithAttachedParams(input.parameters());
        String fileName = params.getOrDefault("fileName", "unknown").toString();
        String baseUrl = resolveStringParam(params, "url", "not valid url");
        String accessToken = OneDriveApi.resolveAccessToken(params);
        // pick first file if provided
        java.io.File file = input.files().values().stream().findFirst().orElse(null);
        String fileNameFromFile = file != null ? file.getName() : fileName;
        boolean graphUpload = Boolean.parseBoolean(String.valueOf(params.getOrDefault("graphUpload", "false")));
        String parentPath = params.getOrDefault("parentPath", "").toString();

        if (graphUpload) {
            if (accessToken == null) {
                throw new IllegalStateException("Missing access_token for graphUpload");
            }
            if (file == null) {
                throw new IllegalStateException("Missing file for graphUpload");
            }
            Map<String, Object> graphResp = OneDriveApi.uploadSmallFile(accessToken, parentPath, file);
            graphResp.put("providerId", providerId());
            graphResp.put("action", actionName());
            return graphResp;
        }

        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        String url = baseUrl + fileNameFromFile;
        Map<String, Object> resp = new HashMap<>();
        resp.put("providerId", providerId());
        resp.put("action", "postFile");
        resp.put("fileName", fileNameFromFile);
        resp.put("url", url);
        if (accessToken != null) {
            resp.put("access_token", accessToken);
        }
        if (file != null) {
            resp.put("fileSize", file.length());
        }

        return resp;
    }
}
