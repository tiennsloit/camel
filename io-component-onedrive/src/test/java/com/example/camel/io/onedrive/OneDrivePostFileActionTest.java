package com.example.camel.io.onedrive;

import com.example.camel.io.spi.AuthTokens;
import com.example.camel.io.spi.Job;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
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

        File input = loadResourceFile("1.pdf");
        job.addFile("1.pdf", input);

        Object result = send(job);

        assertTrue(result instanceof Map);
        Map<?, ?> resp = (Map<?, ?>) result;
        assertEquals("onedrive", resp.get("providerId"));
        assertEquals("1.pdf", resp.get("fileName"));
        assertTrue(resp.get("url").toString().contains("1.pdf"));
        assertTrue(((Number) resp.get("fileSize")).longValue() > 0);
    }

    @Test
    void resourceFile_exists_and_hasSize() throws Exception {
        File input = loadResourceFile("1.pdf");
        assertTrue(input.exists(), "Test fixture 1.pdf not found");
        assertTrue(input.length() > 0, "Test fixture 1.pdf is empty");
        assertTrue(Files.size(input.toPath()) > 0, "Test fixture 1.pdf size check failed");
    }

    private File loadResourceFile(String name) throws URISyntaxException {
        Path path = Path.of(getClass().getClassLoader().getResource(name).toURI());
        return path.toFile();
    }
}
