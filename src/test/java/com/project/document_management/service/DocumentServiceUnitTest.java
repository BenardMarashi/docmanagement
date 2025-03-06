package com.project.document_management.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Optional;

import com.project.document_management.model.Document;
import com.project.document_management.repository.DocumentRepository;
import com.project.document_management.elastic.ElasticDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

class DocumentServiceUnitTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ElasticDocumentRepository elasticDocumentRepository;

    @InjectMocks
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Ensure the ElasticDocumentRepository is injected into documentService
        ReflectionTestUtils.setField(documentService, "elasticDocumentRepository", elasticDocumentRepository);
    }

    @Test
    void uploadDocument_ValidFile_ReturnsDocument() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(fileStorageService.storeFile(any())).thenReturn("test_path");

        // Create a document with an ID to avoid NPEs
        Document savedDocument = new Document();
        savedDocument.setId(1L);
        when(documentRepository.save(any())).thenReturn(savedDocument);

        Document result = documentService.uploadDocument(file, "test");
        assertNotNull(result);
        assertEquals(1L, result.getId());
        // Verify that RabbitTemplate is called with the expected parameters.
        verify(rabbitTemplate).convertAndSend(eq("documentQueue"), eq("1"));
    }

    @Test
    void uploadDocument_EmptyFile_ThrowsException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                documentService.uploadDocument(file, "test"));
    }

    @Test
    void getOCRText_ExistingDocument_ReturnsText() {
        Document doc = new Document();
        doc.setOcrText("test text");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        assertEquals("test text", documentService.getOCRText(1L));
    }

    @Test
    void deleteDocument_ValidId_DeletesResources() {
        Document doc = new Document();
        doc.setId(1L); // Set the ID to avoid NPE
        doc.setStoragePath("test_path");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        documentService.deleteDocument(1L);
        verify(documentRepository).delete(any());
        // Verify that the ElasticDocumentRepository's deleteById method is called with the document's ID as a String.
        verify(elasticDocumentRepository).deleteById(eq("1"));
    }

    @Test
    void syncDocumentToElasticsearch_ValidDocument_SavesToES() {
        Document doc = new Document();
        doc.setId(1L);
        doc.setTitle("test");

        documentService.syncDocumentToElasticsearch(doc);
        // Verify that the ElasticDocumentRepository's save method is called.
        verify(elasticDocumentRepository).save(any());
    }
}
