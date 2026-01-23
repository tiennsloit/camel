package com.example.camel.io.onedrive;

import com.example.camel.io.spi.AbstractGetFolderInfoExecutor;
import com.example.camel.io.spi.FolderInfoItem;
import com.example.camel.io.spi.FolderInfoRequest;
import com.example.camel.io.spi.FolderInfoResponse;

import java.util.List;

/**
 * ActionExecutor that produces folder info for OneDrive.
 */
public class OneDriveGetFolderInfoAction extends AbstractGetFolderInfoExecutor {
    public OneDriveGetFolderInfoAction() {
        super("onedrive", "getFolderInfo");
    }

    @Override
    public FolderInfoResponse getFolderInfo(FolderInfoRequest request) {
        String parentId = request.folderId();
        List<FolderInfoItem> items = parentId == null
                ? List.of(
                        new FolderInfoItem("o-root-1", "OneDrive", "/OneDrive", true),
                        new FolderInfoItem("o-root-2", "Shared", "/Shared", true))
                : List.of(
                        new FolderInfoItem(parentId + "-child-a", "Projects", "/Projects", true),
                        new FolderInfoItem(parentId + "-child-b", "Archive", "/Archive", false));
        return new FolderInfoResponse(providerId(), parentId, items);
    }
}
