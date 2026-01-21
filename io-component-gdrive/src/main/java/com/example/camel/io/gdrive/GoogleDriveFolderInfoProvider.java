package com.example.camel.io.gdrive;

import com.example.camel.io.spi.FolderInfoItem;
import com.example.camel.io.spi.FolderInfoProvider;
import com.example.camel.io.spi.FolderInfoRequest;
import com.example.camel.io.spi.FolderInfoResponse;

import java.util.List;

/**
 * Placeholder Google Drive implementation. Replace stubbed data with real API calls.
 */
public class GoogleDriveFolderInfoProvider implements FolderInfoProvider {
    @Override
    public String id() {
        return "gdrive";
    }

    @Override
    public FolderInfoResponse getFolderInfo(FolderInfoRequest request) {
        String parentId = request.folderId();
        List<FolderInfoItem> items = parentId == null
                ? List.of(
                        new FolderInfoItem("g-root-1", "My Drive", "/My Drive", true),
                        new FolderInfoItem("g-root-2", "Shared drives", "/Shared drives", true))
                : List.of(
                        new FolderInfoItem(parentId + "-child-1", "Docs", "/Docs", false),
                        new FolderInfoItem(parentId + "-child-2", "Images", "/Images", true));
        return new FolderInfoResponse(id(), parentId, items);
    }
}
