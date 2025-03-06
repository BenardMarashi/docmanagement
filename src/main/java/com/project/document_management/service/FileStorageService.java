package com.project.document_management.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class FileStorageService {

    private final Path rootLocation = Paths.get("uploads").toAbsolutePath().normalize();
    private static final Logger logger = Logger.getLogger(FileStorageService.class.getName());

    public FileStorageService() {
        try {
            if (!Files.exists(rootLocation)) {
                logger.info("Creating uploads directory...");
                Files.createDirectories(rootLocation);
            } else {
                logger.info("Uploads directory already exists.");
            }
        } catch (IOException e) {
            logger.severe("Could not initialize storage directory: " + e.getMessage());
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    public String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    public String storeFile(MultipartFile file) throws IOException {
        String sanitizedName = sanitizeFilename(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "_" + sanitizedName;
        Path targetLocation = this.rootLocation.resolve(filename);

        logger.info("Storing file: " + filename);

        try {
            Files.copy(file.getInputStream(), targetLocation);
            logger.info("File stored successfully at: " + targetLocation.toString());
        } catch (IOException e) {
            logger.severe("Failed to store file: " + e.getMessage());
            throw new IOException("Could not store file " + filename, e);
        }

        // Return the absolute path of the stored file
        return targetLocation.toString();
    }
}