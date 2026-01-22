package com.example.camel.io.onedrive;

import com.example.camel.io.spi.ActionExecutor;
import com.example.camel.io.spi.ExecuteInput;

import java.util.HashMap;
import java.util.Map;

/**
 * Stub action executor for posting a file to OneDrive.
 * Replace with real upload logic; currently returns a dummy URL.
 */
public class OneDrivePostFileAction implements ActionExecutor {
    @Override
    public String providerId() {
        return "onedrive";
    }

    @Override
    public String actionName() {
        // matches the JSON descriptor "_postFile"
        return "_postFile";
    }

    @Override
    public Object execute(ExecuteInput input) {
        Map<String, Object> params = new HashMap<>(input.parameters());
        String fileName = params.getOrDefault("fileName", "unknown").toString();
        String baseUrl = params.getOrDefault("url", "not valid url").toString();
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        String url = baseUrl + fileName;
        Map<String, Object> resp = new HashMap<>();
        resp.put("providerId", providerId());
        resp.put("action", "postFile");
        resp.put("fileName", fileName);
        resp.put("url", url);

        return resp;
    }
}
