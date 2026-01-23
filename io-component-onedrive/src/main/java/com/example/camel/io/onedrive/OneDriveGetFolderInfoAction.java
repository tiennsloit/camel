package com.example.camel.io.onedrive;

import com.example.camel.io.spi.AbstractActionExecutor;
import com.example.camel.io.spi.ExecuteInput;
import com.example.camel.io.spi.FolderInfoItem;
import com.example.camel.io.spi.FolderInfoResponse;

import java.util.List;
import java.util.Map;

/**
 * ActionExecutor that produces folder info for OneDrive.
 */
public class OneDriveGetFolderInfoAction extends AbstractActionExecutor {
    public OneDriveGetFolderInfoAction() {
        super("onedrive", "getFolderInfo");
    }

    @Override
    public Object execute(ExecuteInput input) {
        Map<String, Object> params = mergedWithAttachedParams(input.parameters());
        String parentId = params.containsKey("folderId") ? String.valueOf(params.get("folderId")) : null;
        return buildResponse(parentId);
    }

    static FolderInfoResponse buildResponse(String parentId) {
        List<FolderInfoItem> items = parentId == null
                ? List.of(
                        new FolderInfoItem("o-root-1", "OneDrive", "/OneDrive", true),
                        new FolderInfoItem("o-root-2", "Shared", "/Shared", true))
                : List.of(
                        new FolderInfoItem(parentId + "-child-a", "Projects", "/Projects", true),
                        new FolderInfoItem(parentId + "-child-b", "Archive", "/Archive", false));
        return new FolderInfoResponse("onedrive", parentId, items);
    }
}
