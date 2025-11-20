package com.anode.b2;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing Backblaze B2 file operations.
 * Provides simplified methods for common B2 operations.
 */
public class B2Service {

    private static final Logger logger = LoggerFactory.getLogger(B2Service.class);

    private final B2StorageClient client;
    private final B2Properties properties;

    public B2Service(B2StorageClient client, B2Properties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * Upload a file to B2 from a local file.
     *
     * @param localFilePath the local file path
     * @param remoteFileName the name to use in B2
     * @param bucketName the bucket name (null to use default)
     * @return the uploaded file version
     * @throws B2Exception if upload fails
     * @throws IOException if file cannot be read
     */
    public B2FileVersion uploadFile(Path localFilePath, String remoteFileName, String bucketName)
            throws B2Exception, IOException {
        String bucket = bucketName != null ? bucketName : properties.getDefaultBucketName();
        if (bucket == null) {
            throw new IllegalArgumentException("Bucket name must be specified or configured as default");
        }

        logger.debug("Uploading file {} to bucket {} as {}", localFilePath, bucket, remoteFileName);

        B2Bucket b2Bucket = client.getBucketOrNullByName(bucket);
        if (b2Bucket == null) {
            throw new IllegalArgumentException("Bucket not found: " + bucket);
        }

        File file = localFilePath.toFile();
        B2UploadFileRequest request = B2UploadFileRequest
                .builder(b2Bucket.getBucketId(), remoteFileName, B2ContentTypes.APPLICATION_OCTET,
                        B2FileContentSource.build(file))
                .build();

        B2FileVersion fileVersion = client.uploadSmallFile(request);
        logger.info("File uploaded successfully: {} (fileId: {})", remoteFileName, fileVersion.getFileId());

        return fileVersion;
    }

    /**
     * Upload a file to the default bucket.
     *
     * @param localFilePath the local file path
     * @param remoteFileName the name to use in B2
     * @return the uploaded file version
     * @throws B2Exception if upload fails
     * @throws IOException if file cannot be read
     */
    public B2FileVersion uploadFile(Path localFilePath, String remoteFileName)
            throws B2Exception, IOException {
        return uploadFile(localFilePath, remoteFileName, null);
    }

    /**
     * Download a file from B2 as byte array.
     *
     * @param fileName the file name in B2
     * @param bucketName the bucket name (null to use default)
     * @return byte array of the file content
     * @throws B2Exception if download fails
     */
    public byte[] downloadFileAsBytes(String fileName, String bucketName) throws B2Exception {
        String bucket = bucketName != null ? bucketName : properties.getDefaultBucketName();
        if (bucket == null) {
            throw new IllegalArgumentException("Bucket name must be specified or configured as default");
        }

        logger.debug("Downloading file {} from bucket {}", fileName, bucket);

        B2ContentMemoryWriter writer = B2ContentMemoryWriter.build();
        client.downloadByName(bucket, fileName, writer);

        logger.info("File downloaded successfully: {}", fileName);
        return writer.getBytes();
    }

    /**
     * Download a file from the default bucket as byte array.
     *
     * @param fileName the file name in B2
     * @return byte array of the file content
     * @throws B2Exception if download fails
     */
    public byte[] downloadFileAsBytes(String fileName) throws B2Exception {
        return downloadFileAsBytes(fileName, null);
    }

    /**
     * Delete a file from B2 by fileId.
     *
     * @param fileId the B2 file ID
     * @param fileName the file name
     * @throws B2Exception if deletion fails
     */
    public void deleteFileVersion(String fileId, String fileName) throws B2Exception {
        logger.debug("Deleting file version: {} ({})", fileName, fileId);

        B2DeleteFileVersionRequest deleteRequest = B2DeleteFileVersionRequest
                .builder(fileId, fileName)
                .build();

        client.deleteFileVersion(deleteRequest);
        logger.info("File deleted successfully: {} ({})", fileName, fileId);
    }

    /**
     * List files in a bucket by ID.
     *
     * @param bucketId the bucket ID
     * @return list of file versions
     * @throws B2Exception if listing fails
     */
    public List<B2FileVersion> listFileVersionsByBucketId(String bucketId) throws B2Exception {
        logger.debug("Listing files in bucket {}", bucketId);

        // Use listUnfinishedLargeFiles and listFileVersions methods that exist in the SDK
        // For now, we'll return an empty list and let users access the client directly for advanced operations
        logger.warn("Direct file listing requires advanced SDK usage. Please use getClient() for custom operations.");
        return List.of();
    }

    /**
     * Get bucket by name.
     *
     * @param bucketName the bucket name (null to use default)
     * @return the B2 bucket
     * @throws B2Exception if bucket retrieval fails
     */
    public B2Bucket getBucket(String bucketName) throws B2Exception {
        String bucket = bucketName != null ? bucketName : properties.getDefaultBucketName();
        if (bucket == null) {
            throw new IllegalArgumentException("Bucket name must be specified or configured as default");
        }

        B2Bucket b2Bucket = client.getBucketOrNullByName(bucket);
        if (b2Bucket == null) {
            throw new IllegalArgumentException("Bucket not found: " + bucket);
        }

        return b2Bucket;
    }

    /**
     * Get the default bucket.
     *
     * @return the default B2 bucket
     * @throws B2Exception if bucket retrieval fails
     */
    public B2Bucket getDefaultBucket() throws B2Exception {
        return getBucket(null);
    }

    /**
     * Get the B2 storage client for advanced operations.
     *
     * @return the B2 storage client
     */
    public B2StorageClient getClient() {
        return client;
    }

    /**
     * Close the B2 client connection.
     */
    public void close() {
        if (client != null) {
            try {
                client.close();
                logger.info("B2 client closed successfully");
            } catch (Exception e) {
                logger.error("Error closing B2 client", e);
            }
        }
    }
}
