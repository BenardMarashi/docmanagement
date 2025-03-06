package com.project.document_management.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.document_management.config.RabbitMQConfig;
import com.project.document_management.elastic.ElasticDocument;
import com.project.document_management.elastic.ElasticDocumentRepository;
import com.project.document_management.model.Document;
import com.project.document_management.repository.DocumentRepository;


@Service
public class DocumentService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Autowired(required = false)
    private ElasticSearchService elasticSearchService;

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher eventPublisher;
    @Autowired
    private ElasticDocumentRepository elasticDocumentRepository;
    @Autowired
    public DocumentService(FileStorageService fileStorageService, DocumentRepository documentRepository,
                           RabbitTemplate rabbitTemplate, ApplicationEventPublisher eventPublisher) {
        this.fileStorageService = fileStorageService;
        this.documentRepository = documentRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.eventPublisher = eventPublisher;
    }

    public Document uploadDocument(MultipartFile file, String title) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = fileStorageService.storeFile(file);

        Document document = new Document();
        document.setTitle(title);
        document.setStoragePath(filename);
        document.setFileSize(file.getSize());
        document.setContentType(file.getContentType());
        document.setUploadedAt(LocalDateTime.now());
        Document savedDocument = documentRepository.save(document);
        eventPublisher.publishEvent(savedDocument); // Trigger sync to Elasticsearch


        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.DOCUMENT_QUEUE, savedDocument.getId().toString());
        } catch (Exception e) {
            throw new RuntimeException("Message queuing failed", e);
        }

        return savedDocument;
    }

    public String getOCRText(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return document.getOcrText();
    }

    
    public void syncDocumentToElasticsearch(Document document) {
        try {
            if (document.getOcrText() == null || document.getOcrText().isEmpty()) {
                logger.info("Document ID {} has no OCR text yet, skipping Elasticsearch sync", document.getId());
                return;
            }
            
            ElasticDocument elasticDocument = new ElasticDocument();
            elasticDocument.setId(document.getId().toString());
            elasticDocument.setTitle(document.getTitle());
            elasticDocument.setOcrText(document.getOcrText());
            elasticDocument.setUploadedAt(document.getUploadedAt());
            elasticDocument.setFileSize(document.getFileSize());
            elasticDocument.setContentType(document.getContentType());
            
            // Use the elasticSearchService if available, otherwise use direct repository
            if (elasticSearchService != null) {
                elasticSearchService.indexDocument(elasticDocument);
            } else {
                elasticDocumentRepository.save(elasticDocument);
                logger.info("Document ID {} synced successfully to Elasticsearch", document.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to sync document ID {} to Elasticsearch: {}", 
                      document.getId(), e.getMessage(), e);
        }
    }

    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    public Resource getDocumentFile(Long id) throws IOException {
        Document document = getDocumentById(id);
        Path filePath = Paths.get(document.getStoragePath());
        return new UrlResource(filePath.toUri());
    }

    public void deleteDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        try {
            Files.deleteIfExists(Paths.get(document.getStoragePath()));
            documentRepository.delete(document);
            elasticDocumentRepository.deleteById(document.getId().toString());
        } catch (IOException e) {
            throw new RuntimeException("Error deleting file", e);
        }
    }

    public List<Document> getDocuments(String search, String sortField, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        if (!search.isEmpty()) {
            return documentRepository.findByTitleContainingIgnoreCaseOrOcrTextContaining(search, sort);
        }
        return documentRepository.findAll(sort);
    }

}