package com.project.document_management.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.project.document_management.elastic.ElasticDocumentRepository;
import com.project.document_management.model.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentEventListenerUnitTest {

    @Mock
    private ElasticDocumentRepository elasticRepo;

    @InjectMocks
    private DocumentEventListener listener;

    @Test
    void handleDocumentUpdate_SavesToElasticsearch() {
        Document doc = new Document();
        doc.setId(1L);

        listener.handleDocumentUpdate(doc);
        verify(elasticRepo).save(any());
    }
}