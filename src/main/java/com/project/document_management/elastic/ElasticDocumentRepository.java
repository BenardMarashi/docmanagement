package com.project.document_management.elastic;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ElasticDocumentRepository extends ElasticsearchRepository<ElasticDocument, String> {

    // Basic search (original method)
    List<ElasticDocument> findByOcrTextContaining(String query);
    
    // Same search with pagination
    Page<ElasticDocument> findByOcrTextContaining(String query, Pageable pageable);
    
    // Fuzzy search based on ocrText and title
    @Query("{\"bool\": {\"should\": [{\"match\": {\"ocrText\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}, {\"match\": {\"title\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}]}}")
    List<ElasticDocument> searchByOcrTextOrTitleFuzzy(String query);
    
    // Fuzzy search with pagination
    @Query("{\"bool\": {\"should\": [{\"match\": {\"ocrText\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}, {\"match\": {\"title\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}]}}")
    Page<ElasticDocument> searchByOcrTextOrTitleFuzzy(String query, Pageable pageable);
    
    // Search using the n-gram field for partial matches
    List<ElasticDocument> findByOcrTextNgramContaining(String query);
}