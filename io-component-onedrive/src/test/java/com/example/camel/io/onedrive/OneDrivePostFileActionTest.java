package com.example.camel.io.onedrive;

import com.example.camel.io.spi.AuthTokens;
import com.example.camel.io.spi.Job;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OneDrivePostFileActionTest extends com.example.camel.io.spi.BaseActionTest<OneDrivePostFileAction> {

    OneDrivePostFileActionTest() {
        super("onedrive", "_postFile");
    }

    @Test
    void postFile_returnsUrl() throws Exception {
        Job job = new Job();
        job.addParameter("fileName", "testFile-Testcase");
        job.addParameter("parentid", "/Shared/Scan2Egnyte");

        Map<String, String> token = new HashMap<>();
        token.put("access_token", "dummy-token");
        Map<String, Object> attachedParams = new HashMap<>();
        attachedParams.put("url", "https://onedrive.example.com/files/");
        AuthTokens tokens = new AuthTokens();
        tokens.addTokens("egnyte", token, attachedParams);
        job.addHeader(AuthTokens.getHeaderName(), tokens);

        File input = new File("src/test/resources/1.pdf");
        job.addFile("pdftest1.pdf", input);

        Object result = send(job);

        assertTrue(result instanceof Map);
        Map<?, ?> resp = (Map<?, ?>) result;
        assertEquals("onedrive", resp.get("providerId"));
        assertEquals("testFile-Testcase", resp.get("fileName"));
        assertTrue(resp.get("url").toString().contains("testFile-Testcase"));
    }
}
