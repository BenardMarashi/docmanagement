package com.project.document_management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.project.document_management.elastic.ElasticDocument;
import com.project.document_management.elastic.ElasticDocumentRepository;
import com.project.document_management.model.Document;

@Component
public class DocumentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(DocumentEventListener.class);

    @Autowired
    private ElasticDocumentRepository elasticDocumentRepository;
    
    @Autowired(required = false)
    private ElasticSearchService elasticSearchService;

    @EventListener
    public void handleDocumentUpdate(Document document) {
        try {
            // Skip if document has no OCR text yet
            if (document.getOcrText() == null || document.getOcrText().isEmpty()) {
                logger.info("Document ID {} has no OCR text yet, skipping Elasticsearch update", document.getId());
                return;
            }
            
            ElasticDocument elasticDocument = new ElasticDocument();
            elasticDocument.setId(document.getId().toString());
            elasticDocument.setTitle(document.getTitle());
            elasticDocument.setOcrText(document.getOcrText());
            elasticDocument.setUploadedAt(document.getUploadedAt());
            elasticDocument.setFileSize(document.getFileSize());
            elasticDocument.setContentType(document.getContentType());
            
            // Use the service if available (for retries), otherwise use direct repository
            if (elasticSearchService != null) {
                elasticSearchService.indexDocument(elasticDocument);
            } else {
                elasticDocumentRepository.save(elasticDocument);
                logger.info("Document ID {} indexed successfully in Elasticsearch", document.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to index document ID {} in Elasticsearch: {}", 
                         document.getId(), e.getMessage(), e);
        }
    }
}