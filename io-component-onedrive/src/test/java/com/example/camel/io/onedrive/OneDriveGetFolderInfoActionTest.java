package com.example.camel.io.onedrive;

import com.example.camel.io.spi.BaseActionTest;
import com.example.camel.io.spi.FolderInfoResponse;
import com.example.camel.io.spi.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OneDriveGetFolderInfoActionTest extends BaseActionTest {

    @BeforeEach
    void setup() {
        init("onedrive", "getFolderInfo");
    }

    @Test
    void rootListing_viaAction() throws Exception {
        Object result = send(new Job());
        assertTrue(result instanceof FolderInfoResponse);
        FolderInfoResponse resp = (FolderInfoResponse) result;
        assertEquals("onedrive", resp.providerId());
        assertNull(resp.parentFolderId());
        assertEquals(2, resp.folders().size());
    }

    @Test
    void childListing_viaAction() throws Exception {
        Job job = new Job();
        job.addParameter("folderId", "parent-123");

        Object result = send(job);
        assertTrue(result instanceof FolderInfoResponse);
        FolderInfoResponse resp = (FolderInfoResponse) result;
        assertEquals("onedrive", resp.providerId());
        assertEquals("parent-123", resp.parentFolderId());
        assertEquals(2, resp.folders().size());
        assertTrue(resp.folders().stream().anyMatch(f -> f.id().startsWith("parent-123-child-")));
    }
}
