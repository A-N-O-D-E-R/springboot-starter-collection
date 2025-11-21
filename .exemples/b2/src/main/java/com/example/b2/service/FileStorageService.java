package com.example.b2.service;

import com.anode.b2.B2Service;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final String TEMP_DIR = "uploads";

    private final B2Service b2Service;

    public FileStorageService(B2Service b2Service) {
        this.b2Service = b2Service;
        initializeTempDirectory();
    }

    private void initializeTempDirectory() {
        try {
            Path tempPath = Paths.get(TEMP_DIR);
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
                logger.info("Created temporary upload directory: {}", TEMP_DIR);
            }
        } catch (IOException e) {
            logger.error("Failed to create temporary directory", e);
        }
    }

    /**
     * Upload a file to B2.
     *
     * @param file the multipart file to upload
     * @param bucketName optional bucket name (null for default)
     * @return the file ID
     */
    public String uploadFile(MultipartFile file, String bucketName) throws IOException, B2Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Save file temporarily
        String originalFileName = file.getOriginalFilename();
        Path tempFile = Paths.get(TEMP_DIR, originalFileName);

        try {
            // Copy uploaded file to temp location
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Saved temporary file: {}", tempFile);

            // Upload to B2
            B2FileVersion fileVersion = b2Service.uploadFile(tempFile, originalFileName, bucketName);
            logger.info("Uploaded file to B2: {} (ID: {})", originalFileName, fileVersion.getFileId());

            return fileVersion.getFileId();
        } finally {
            // Clean up temp file
            try {
                Files.deleteIfExists(tempFile);
                logger.debug("Deleted temporary file: {}", tempFile);
            } catch (IOException e) {
                logger.warn("Failed to delete temporary file: {}", tempFile, e);
            }
        }
    }

    /**
     * Download a file from B2.
     *
     * @param fileName the file name to download
     * @param bucketName optional bucket name (null for default)
     * @return byte array of file content
     */
    public byte[] downloadFile(String fileName, String bucketName) throws B2Exception {
        logger.info("Downloading file from B2: {}", fileName);
        byte[] content = b2Service.downloadFileAsBytes(fileName, bucketName);
        logger.info("Downloaded {} bytes for file: {}", content.length, fileName);
        return content;
    }

    /**
     * Delete a file from B2.
     *
     * @param fileName the file name to delete
     * @param bucketName optional bucket name (null for default)
     */
    public void deleteFile(String fileName, String bucketName) throws B2Exception {
        logger.info("Deleting file from B2: {}", fileName);
        logger.warn("Direct file deletion requires fileId. Use deleteFileVersion(fileId, fileName) instead.");
        throw new UnsupportedOperationException("Use deleteFileVersion(fileId, fileName) - fileId must be obtained during upload or listing");
    }

    /**
     * Get storage information.
     *
     * @return map with storage info
     */
    public Map<String, Object> getStorageInfo() throws B2Exception {
        Map<String, Object> info = new HashMap<>();

        try {
            var bucket = b2Service.getDefaultBucket();
            info.put("success", true);
            info.put("bucketName", bucket.getBucketName());
            info.put("bucketId", bucket.getBucketId());
            info.put("bucketType", bucket.getBucketType());
            info.put("accountId", bucket.getAccountId());
        } catch (Exception e) {
            info.put("success", false);
            info.put("message", "No default bucket configured or accessible");
        }

        return info;
    }

    /**
     * Run a demo that creates, uploads, downloads, and deletes a test file.
     *
     * @return map with demo results
     */
    public Map<String, Object> runDemo() throws IOException, B2Exception {
        Map<String, Object> results = new HashMap<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String testFileName = "test-file-" + timestamp + ".txt";
        Path tempFile = Paths.get(TEMP_DIR, testFileName);

        try {
            // Step 1: Create test file
            String testContent = "This is a test file created at " + LocalDateTime.now() +
                    "\nThis file demonstrates the B2 Spring Boot Starter capabilities.";
            Files.writeString(tempFile, testContent);
            results.put("step1_create", "Created test file: " + testFileName);

            // Step 2: Upload to B2
            B2FileVersion uploadedFile = b2Service.uploadFile(tempFile, testFileName);
            results.put("step2_upload", Map.of(
                    "fileName", uploadedFile.getFileName(),
                    "fileId", uploadedFile.getFileId(),
                    "size", uploadedFile.getContentLength(),
                    "contentType", uploadedFile.getContentType()
            ));

            // Step 3: Download from B2
            byte[] downloadedContent = b2Service.downloadFileAsBytes(testFileName);
            results.put("step3_download", Map.of(
                    "size", downloadedContent.length,
                    "content", new String(downloadedContent).substring(0, Math.min(100, downloadedContent.length))
            ));

            // Step 4: Delete from B2 using fileId from upload
            b2Service.deleteFileVersion(uploadedFile.getFileId(), testFileName);
            results.put("step4_delete", "Deleted test file: " + testFileName);

            results.put("success", true);
            results.put("message", "Demo completed successfully!");

        } catch (Exception e) {
            results.put("success", false);
            results.put("error", e.getMessage());
            logger.error("Demo failed", e);
            throw e;
        } finally {
            // Clean up temp file
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                logger.warn("Failed to delete temp file", e);
            }
        }

        return results;
    }
}
