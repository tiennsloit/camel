package com.example.camel.io.gdrive;

import com.example.camel.io.spi.FolderInfoRequest;
import com.example.camel.io.spi.FolderInfoResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoogleDriveFolderInfoProviderTest {

    private final GoogleDriveFolderInfoProvider provider = new GoogleDriveFolderInfoProvider();

    @Test
    void idShouldBeGdrive() {
        assertEquals("gdrive", provider.id());
    }

    @Test
    void rootListingShouldContainStubRoots() throws Exception {
        FolderInfoResponse resp = provider.getFolderInfo(new FolderInfoRequest(null, null));
        assertNotNull(resp);
        assertEquals("gdrive", resp.providerId());
        assertNull(resp.parentFolderId());
        assertEquals(2, resp.folders().size());
        assertEquals("g-root-1", resp.folders().get(0).id());
        assertEquals("g-root-2", resp.folders().get(1).id());
    }

    @Test
    void childListingShouldIncludeParentInIds() throws Exception {
        String parentId = "parent-123";
        FolderInfoResponse resp = provider.getFolderInfo(new FolderInfoRequest(parentId, null));
        assertNotNull(resp);
        assertEquals("gdrive", resp.providerId());
        assertEquals(parentId, resp.parentFolderId());
        assertEquals(2, resp.folders().size());
        assertTrue(resp.folders().get(0).id().startsWith(parentId));
        assertTrue(resp.folders().get(1).id().startsWith(parentId));
    }
}
