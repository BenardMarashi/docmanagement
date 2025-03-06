package com.project.document_management.ocr;

import com.project.document_management.model.Document;
import com.project.document_management.repository.DocumentRepository;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class OCRWorker {

    private static final Logger logger = LoggerFactory.getLogger(OCRWorker.class);
    private final DocumentRepository documentRepository;
    private final Tesseract tesseract;

    @Autowired
    public OCRWorker(DocumentRepository documentRepository,
                     @Value("${tesseract.datapath:/usr/share/tessdata}") String tesseractDataPath) {
        this.documentRepository = documentRepository;
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(tesseractDataPath);
    }

    @RabbitListener(queues = "${rabbitmq.queue.name:documentQueue}")
    public void processDocument(Long documentId) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));

            Path filePath = Path.of(document.getStoragePath());
            validateFileExists(filePath);

            String ocrText = performOCR(filePath.toFile());
            updateDocumentWithOCR(document, ocrText);

            logger.info("Successfully processed document ID: {}", documentId);
        } catch (Exception e) {
            logger.error("OCR processing failed for document ID: {}", documentId, e);
            throw new RuntimeException("OCR processing failed", e);
        }
    }

    private void validateFileExists(Path filePath) {
        if (!Files.exists(filePath)) {
            logger.error("File not found at path: {}", filePath);
            throw new RuntimeException("File not found: " + filePath);
        }
    }

    private String performOCR(File file) throws TesseractException {
        logger.debug("Performing OCR on file: {}", file.getAbsolutePath());
        return tesseract.doOCR(file);
    }

    private void updateDocumentWithOCR(Document document, String ocrText) {
        document.setOcrText(ocrText);
        documentRepository.save(document);
        logger.info("Updated document ID {} with OCR text", document.getId());
    }
}