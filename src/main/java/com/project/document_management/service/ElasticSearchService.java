package com.project.document_management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.project.document_management.elastic.ElasticDocument;
import com.project.document_management.elastic.ElasticDocumentRepository;

@Service
public class ElasticSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchService.class);
    
    @Autowired
    private ElasticDocumentRepository elasticDocumentRepository;
    
    @Retryable(value = {Exception.class}, 
              maxAttempts = 3, 
              backoff = @Backoff(delay = 1000, multiplier = 2))
    public void indexDocument(ElasticDocument document) {
        try {
            elasticDocumentRepository.save(document);
            logger.info("Document ID {} indexed successfully in Elasticsearch", document.getId());
        } catch (Exception e) {
            logger.error("Error indexing document in Elasticsearch: {}", e.getMessage());
            throw e; // Rethrow for retry
        }
    }
    
    @Recover
    public void recoverIndexDocument(Exception e, ElasticDocument document) {
        logger.error("Failed to index document after retries: {}", e.getMessage(), e);
        // Could add to a "failed index" queue for later processing
    }
    
    @Retryable(value = {Exception.class}, 
              maxAttempts = 3, 
              backoff = @Backoff(delay = 1000, multiplier = 2))
    public Page<ElasticDocument> search(String query, Pageable pageable, boolean fuzzy) {
        try {
            if (fuzzy) {
                return elasticDocumentRepository.searchByOcrTextOrTitleFuzzy(query, pageable);
            } else {
                return elasticDocumentRepository.findByOcrTextContaining(query, pageable);
            }
        } catch (Exception e) {
            logger.error("Error searching in Elasticsearch: {}", e.getMessage());
            throw e; // Rethrow for retry
        }
    }
    
    @Recover
    public Page<ElasticDocument> recoverSearch(Exception e, String query, Pageable pageable, boolean fuzzy) {
        logger.error("Failed to search Elasticsearch after retries: {}", e.getMessage(), e);
        // Return empty result
        return Page.empty(pageable);
    }
}