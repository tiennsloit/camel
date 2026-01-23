package com.example.camel.io.onedrive;

import com.example.camel.io.spi.AuthTokens;
import com.example.camel.io.spi.Job;
import com.example.camel.io.spi.BaseActionTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OneDrivePostFileActionTest extends BaseActionTest {

    @BeforeEach
    void setup() {
        init("onedrive", "_postFile");
    }

    @Test
    void postFile_returnsUrl() throws Exception {
        Job job = new Job();
        job.addParameter("fileName", "testFile-Testcase");
        job.addParameter("parentid", "/Shared/Scan2Egnyte");

        Map<String, String> token = new HashMap<>();
        token.put("access_token", Global.token);
        Map<String, Object> attachedParams = new HashMap<>();
        attachedParams.put("url", "https://onedrive.example.com/files/");
        AuthTokens tokens = new AuthTokens();
        tokens.addTokens("onedrive", token, attachedParams);
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

class Global {
    final static String token = "EwBIBMl6BAAUu4TQbLz/EdYigQnDPtIo76ZZUKsAAY8+Iv2QvhdijM8kqEHN+d59GYS43nZC6o01w5jwgsVXcrhiSGTVTXCuZDIDmU4quQXlHAojChSb4YgqOxrvm2W9APTSpUyIYfYHZI/+PW6UnoG4TqBK4atYg1KtIozsH85vdQJRzm//Mthltl1nwhHa/AwQAdrBVgtig1FWai1sUIlJ5RpMmzGbOk1CZkJqn9p4IIuPOao+v+Ww8+WsDlupiTMbw45l6cAyHFx+A965uPUvG4h1i2GyxN+FZa24uWeX8WJBygFCUZJrCSTsEzQza5THK7wwPqxkWCAe8MBODBJBvAI5uvyIElANHv5W2K8o8lfwVOBXI5ReXJfBqoYQZgAAEKcpPx718LYGCUxS2C2axbUQA29vZKQXmbiLoz9orwCeIxet9x8luPMlkwGPn36RKP/p0G+GjS6fpkkMtmd06CcMxMmTQTezW5OOxaRieKgdd/jsNCM/5tTel8sRxxF3SgTKJqpfLmH9M/FqxuGy4+FpU38alyoDpphcikX4/TIbs9/0twbh9QGP4jeOZ0rM5jy4OSlfr4O0Y2IGdqCGD3cKCuT8cUJagC0+wWXLOfVrqirskNkUprWkfA3uYIgfCirlQKHt1X2QaLNv4g7nUgj+BAXHLHeHXjZSZMBwjhcsgQCV86Azmj4esWRzb5dt7pKWwSIppYIAIw5tWYdaQtz9mQBhs6ufdVeDV/AWRBRh2WQPFvytYIdWW5VhqQwgp/ps3XU96hydTNQWNmJxf/ZZcA3MKqwe7O8Lg9EOkcwd6n7wFVpAcBDpzVhPYvOIu7onLKOsGAy4OrhGJWzoMTdJ617ZOn8PFisfLZ2eALVKXDCmvQTYCnx3N0LG3BxSoIRLEgyDr418yX6rKmfifg8kOTH3nNHHWQoseFeFQAj0fl8v4AL9h7sXtLAocWad+Mqf+I+heJ4KWO7JQras93wGZ/9qFbiIJ9EgY3Lvb+cxNLmP60ZXY8MFSwUbL4RVyzRo3uhaxNeo6k+ajVImKBF4z6ADKW0LIwv3OVVQFr5y0cNxkklHxyTWA+J+MInpHiD93a2cC3iu/0JbfnYHLCMhO2HYiNumVKrNWPD12nlB2IRSUTDAyVxJ90LnC5jRw06nnomypYkpEy5vR+gI8Ut0L/x5PtGGGT/84ptz4Nzt4JMNRSCyMl74DYcF7aNtg3191dubAYILEzWTiMV5NaywPGelZ8wcEMtc3p22ptaCFHPGthgiEgP8Dh7vpj3udfFmScBa8/vIXsNSDZ86B6IY9mwUNQZ3VwSyS41k4rNAvezt6mYY1BbykH4Onx3+wBNKxIF81B1mTJqUP06jBp/EnnlMHpplFX3tBvfMJzI0xUrnURmewe6fd8NoRnavnfQ1Vwc+xmYa6PJnINeRIy22Rg4ph1/neVvW3iJRe12AZ6pNAw==";
}