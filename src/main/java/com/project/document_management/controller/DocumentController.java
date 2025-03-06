package com.project.document_management.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.document_management.elastic.ElasticDocument;
import com.project.document_management.elastic.ElasticDocumentRepository;
import com.project.document_management.model.Document;
import com.project.document_management.service.DocumentService;
import com.project.document_management.service.ElasticSearchService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    @Autowired
    private DocumentService documentService;
    @Autowired(required = false)
    private ElasticSearchService elasticSearchService;
    @Autowired
    private ElasticDocumentRepository elasticDocumentRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title) throws IOException {
        Document savedDocument = documentService.uploadDocument(file, title);
        return ResponseEntity.ok(savedDocument);
    }

    @GetMapping("/{id}/ocr")
    public ResponseEntity<String> getOCRText(@PathVariable Long id) {
        String ocrText = documentService.getOCRText(id);
        return ResponseEntity.ok(ocrText);
    }

    @GetMapping("/search")
public ResponseEntity<?> searchDocuments(
        @RequestParam String query,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "false") boolean fuzzy) {
    
    try {
        Page<ElasticDocument> results;
        Pageable pageable = PageRequest.of(page, size);
        
        // Use service if available, otherwise direct repository
        if (elasticSearchService != null) {
            results = elasticSearchService.search(query, pageable, fuzzy);
        } else if (fuzzy) {
            results = elasticDocumentRepository.searchByOcrTextOrTitleFuzzy(query, pageable);
        } else {
            results = elasticDocumentRepository.findByOcrTextContaining(query, pageable);
        }
        
        return ResponseEntity.ok(results);
    } catch (Exception e) {
        logger.error("Error searching documents: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error searching documents: " + e.getMessage());
    }
}
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) throws IOException {
        Document document = documentService.getDocumentById(id);
        Resource resource = documentService.getDocumentFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Document>> getDocuments(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "uploadedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        List<Document> documents = documentService.getDocuments(search, sort, direction);
        return ResponseEntity.ok(documents);
    }
}