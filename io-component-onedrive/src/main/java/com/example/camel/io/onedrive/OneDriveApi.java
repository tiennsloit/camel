package com.example.camel.io.onedrive;

import com.example.camel.io.spi.AuthTokens;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;

/**
 * Minimal helper for OneDrive-specific API needs (e.g., token extraction).
 * Replace/extend with real Graph client usage as needed.
 */
public final class OneDriveApi {
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private OneDriveApi() {}

    /**
     * Resolve an access token for OneDrive from either:
     * - direct parameter "access_token"
     * - AuthTokens header entry for provider "onedrive"
     *
     * @return token string or null if not present
     */
    public static String resolveAccessToken(Map<String, Object> params) {
        Object direct = params.get("access_token");
        if (direct != null) {
            return direct.toString();
        }
        Object auth = params.get(AuthTokens.getHeaderName());
        if (auth instanceof AuthTokens tokens) {
            AuthTokens.TokenEntry entry = tokens.tokens().get("onedrive");
            if (entry != null) {
                String token = entry.tokens().get("access_token");
                if (token != null) {
                    return token;
                }
            }
        }
        return null;
    }

    /**
     * Upload a small file (<= ~4 MB) to OneDrive using the simple upload API.
     * Returns a map with status, responseBody, targetPath, and fileSize.
     */
    public static Map<String, Object> uploadSmallFile(String accessToken, String parentPath, File file) throws IOException, InterruptedException {
        String fileName = file.getName();
        String pathSegment = (parentPath == null || parentPath.isBlank())
                ? fileName
                : parentPath + "/" + fileName;
        String url = "https://graph.microsoft.com/v1.0/me/drive/root:/" + pathSegment + ":/content";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(Files.readAllBytes(file.toPath())))
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

        Map<String, Object> out = new HashMap<>();
        out.put("status", resp.statusCode());
        out.put("responseBody", resp.body());
        out.put("targetPath", pathSegment);
        out.put("fileSize", file.length());
        out.put("url", url);
        return out;
    }
}
