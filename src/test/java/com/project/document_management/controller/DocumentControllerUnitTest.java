package com.project.document_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import com.project.document_management.model.Document;
import com.project.document_management.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DocumentControllerUnitTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentController documentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(documentController).build();
    }

    @Test
    void uploadDocument_ValidRequest_ReturnsOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "content".getBytes());

        when(documentService.uploadDocument(any(), any()))
                .thenReturn(new Document());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("title", "test"))
                .andExpect(status().isOk());
    }

    @Test
    void getDocuments_EmptySearch_ReturnsAll() throws Exception {
        when(documentService.getDocuments(any(), any(), any()))
                .thenReturn(Collections.singletonList(new Document()));

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk());
    }
}