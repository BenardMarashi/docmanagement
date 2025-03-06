package com.project.document_management.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.document_management.elastic.ElasticDocument;
import com.project.document_management.elastic.ElasticDocumentRepository;
import com.project.document_management.model.Document;
import com.project.document_management.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // This will load application-test.properties
public class DocumentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // We mock out the ElasticDocumentRepository to avoid external ES dependency.
    @Mock
    private ElasticDocumentRepository elasticDocumentRepository;

    @BeforeEach
    void setUp() {
        // Clean up the in-memory database between tests.
        documentRepository.deleteAll();
    }

    @Test
    void testUploadDocument() throws Exception {
        // Create a dummy file (plain text file in this example).
        byte[] content = "This is a test file content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content);

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("title", "Integration Test Document"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Integration Test Document"));
    }

    @Test
    void testGetOCRText() throws Exception {
        // Pre-populate the in-memory database with a document having OCR text.
        Document doc = new Document();
        doc.setTitle("Doc with OCR");
        doc.setOcrText("Extracted OCR Content");
        doc.setStoragePath("dummy/path"); // Not used by this endpoint.
        doc.setUploadedAt(LocalDateTime.now());
        Document savedDoc = documentRepository.save(doc);

        mockMvc.perform(get("/api/documents/" + savedDoc.getId() + "/ocr"))
                .andExpect(status().isOk())
                .andExpect(content().string("Extracted OCR Content"));
    }

    @Test
    void testSearchDocuments() throws Exception {
        // Simulate a search result from Elasticsearch by stubbing the repository.
        ElasticDocument elasticDoc = new ElasticDocument();
        elasticDoc.setId("1");
        elasticDoc.setTitle("Elastic Test Document");
        elasticDoc.setOcrText("Sample OCR");
        elasticDoc.setUploadedAt(LocalDateTime.now());
        when(elasticDocumentRepository.findByOcrTextContaining(anyString()))
                .thenReturn(List.of(elasticDoc));

        mockMvc.perform(get("/api/documents/search").param("query", "Elastic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Elastic Test Document"));
    }
}
