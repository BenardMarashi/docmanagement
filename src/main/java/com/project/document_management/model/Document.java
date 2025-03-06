package com.project.document_management.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String storagePath; // Path to the stored file
    private Long fileSize; // File size in bytes
    private String contentType; // File MIME type (e.g., application/pdf)
    private LocalDateTime uploadedAt; // Timestamp of upload

    @Lob
    private String ocrText; // Field to store extracted OCR text

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getOcrText() {  // <-- Add this getter
        return ocrText;
    }

    public void setOcrText(String ocrText) {  // <-- Add this setter
        this.ocrText = ocrText;
    }

}
