package com.project.document_management.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.project.document_management.elastic.ElasticDocument;
import com.project.document_management.elastic.ElasticDocumentRepository;
import com.project.document_management.model.Document;
import com.project.document_management.repository.DocumentRepository;
import com.project.document_management.ocr.OCRWorker;
import com.project.document_management.service.DocumentService;
import com.project.document_management.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public class DocumentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.1")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false"); // Disable security

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Get actual Testcontainers host:port
        String elasticsearchUrl = "http://" + elasticsearch.getHost() + ":" + elasticsearch.getFirstMappedPort();

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.elasticsearch.uris", () -> elasticsearchUrl); // Override ES URL
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("file.storage.path", () -> "target/test-files");
    }

    // 2. Mock OCR Processing with proper dependency injection
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        OCRWorker mockOCRWorker(DocumentRepository documentRepository) {
            return new OCRWorker(documentRepository, "dummy/tessdata/path") {
                @Override
                public void processDocument(Long documentId) {
                    Document doc = documentRepository.findById(documentId).orElseThrow();
                    doc.setOcrText("MOCKED OCR TEXT");
                    documentRepository.save(doc);
                }
            };
        }
    }

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ElasticDocumentRepository elasticRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private DocumentRepository documentRepository;

    // 3. Cleanup After Each Test
    @AfterEach
    void cleanup() throws IOException {
        documentRepository.deleteAll();
        elasticRepository.deleteAll();
        Files.deleteIfExists(Path.of("target/test-files"));
    }

    // 4. Test Scenarios
    @Test
    void testFullDocumentLifecycle() throws Exception {
        // Upload Document
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes());
        Document doc = documentService.uploadDocument(mockFile, "Integration Test");

        // Verify OCR Processing
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> documentService.getOCRText(doc.getId()) != null);

        // Verify Elasticsearch Sync
        ElasticDocument elasticDoc = elasticRepository.findById(doc.getId().toString()).orElseThrow();
        assertEquals("Integration Test", elasticDoc.getTitle());
        assertEquals("MOCKED OCR TEXT", elasticDoc.getOcrText());

        // Verify File Storage
        assertTrue(Files.exists(Path.of(doc.getStoragePath())));

        // Delete Document
        documentService.deleteDocument(doc.getId());
        assertFalse(Files.exists(Path.of(doc.getStoragePath())));
        assertFalse(elasticRepository.existsById(doc.getId().toString()));
    }

    @Test
    void testSearchFunctionality() {
        // Seed Elasticsearch
        ElasticDocument doc = new ElasticDocument();
        doc.setOcrText("search test");
        elasticRepository.save(doc);

        List<ElasticDocument> results = elasticRepository.findByOcrTextContaining("search");
        assertEquals(1, results.size());
    }
}