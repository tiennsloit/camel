package com.example.camel.io.onedrive;

import com.example.camel.io.spi.AbstractActionExecutor;
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
        // pick first file if provided
        java.io.File file = input.files().values().stream().findFirst().orElse(null);
        String fileNameFromFile = file != null ? file.getName() : fileName;

        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        String url = baseUrl + fileNameFromFile;
        Map<String, Object> resp = new HashMap<>();
        resp.put("providerId", providerId());
        resp.put("action", "postFile");
        resp.put("fileName", fileNameFromFile);
        resp.put("url", url);
        if (file != null) {
            resp.put("fileSize", file.length());
        }

        return resp;
    }
}
