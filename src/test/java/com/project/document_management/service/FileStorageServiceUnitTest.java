package com.project.document_management.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceUnitTest {

    @TempDir
    Path tempDir;

    @Test
    void storeFile_ValidFile_ReturnsPath() throws IOException {
        FileStorageService service = new FileStorageService();
        MockMultipartFile file = new MockMultipartFile(
                "test", "test.txt", "text/plain", "content".getBytes());

        String path = service.storeFile(file);
        assertTrue(path.contains("test.txt"));
    }

    @Test
    void sanitizeFilename_InvalidChars_ReplacesUnderscores() {
        FileStorageService service = new FileStorageService();
        String result = service.sanitizeFilename("bad@file#name.jpg");
        assertEquals("bad_file_name.jpg", result);
    }
}