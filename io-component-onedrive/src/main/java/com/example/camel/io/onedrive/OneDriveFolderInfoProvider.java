package com.example.camel.io.onedrive;

import com.example.camel.io.spi.FolderInfoProvider;
import com.example.camel.io.spi.FolderInfoRequest;
import com.example.camel.io.spi.FolderInfoResponse;

/**
 * Placeholder OneDrive implementation. Replace stubbed data with real Graph API calls.
 */
public class OneDriveFolderInfoProvider implements FolderInfoProvider {
    @Override
    public FolderInfoResponse getFolderInfo(FolderInfoRequest request) {
        return OneDriveGetFolderInfoAction.buildResponse(request.folderId());
    }
}
