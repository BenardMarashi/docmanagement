package com.project.document_management.OCR;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.imageio.ImageIO;

import com.project.document_management.model.Document;
import com.project.document_management.ocr.OCRWorker;
import com.project.document_management.repository.DocumentRepository;
import net.sourceforge.tess4j.Tesseract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class OCRWorkerUnitTest {

    @Mock
    private DocumentRepository documentRepository;

    // We'll mock the Tesseract instance used by OCRWorker.
    @Mock
    private Tesseract tesseract;

    private OCRWorker ocrWorker;

    @BeforeEach
    void setUp() {
        // Create an instance of OCRWorker using a dummy tessdata path.
        ocrWorker = new OCRWorker(documentRepository, "dummyPath");
        // Replace the internal Tesseract instance with our mock.
        ReflectionTestUtils.setField(ocrWorker, "tesseract", tesseract);
    }

    @Test
    void processDocument_ValidId_UpdatesOCRText() throws Exception {
        // Prepare a Document with an ID.
        Document doc = new Document();
        doc.setId(1L);

        // Create a temporary valid PNG image.
        Path tempFile = Files.createTempFile("test", ".png");
        BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 100, 50);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Test OCR", 10, 25);
        g2d.dispose();
        ImageIO.write(image, "png", tempFile.toFile());
        tempFile.toFile().deleteOnExit();
        // Set the document's storage path to the temporary image.
        doc.setStoragePath(tempFile.toAbsolutePath().toString());

        // When the repository is asked for the document, return our document.
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        // Stub the Tesseract.doOCR(File) call to return a dummy OCR result.
        when(tesseract.doOCR(any(File.class))).thenReturn("dummy OCR text");

        // Process the document. This should use our mocked Tesseract.
        ocrWorker.processDocument(1L);

        // Verify that the documentRepository.save was called with our updated document.
        verify(documentRepository).save(doc);

        // Assert that the document's OCR text was updated as expected.
        assertEquals("dummy OCR text", doc.getOcrText());
    }
}

