# Backblaze B2 Spring Boot Starter

A Spring Boot starter that provides seamless integration with [Backblaze B2](https://www.backblaze.com/b2/cloud-storage.html) cloud storage, enabling easy file management in your Spring Boot applications.

## Features

- Auto-configuration of Backblaze B2 client
- Simple service layer for common file operations
- Configurable timeouts and retry logic
- Support for multiple buckets
- Spring Boot 3.4.2 compatible
- Java 21 support

## Requirements

- Java 21 or higher
- Spring Boot 3.4.2 or higher
- Maven 3.6+
- Backblaze B2 account with application key

## Installation

Add the following dependency to your Spring Boot application's `pom.xml`:

```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>b2-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Configuration

### Required Configuration

Configure your B2 credentials in `application.properties`:

```properties
# B2 Application Key ID (formerly Account ID)
b2.applicationKeyId=your_application_key_id

# B2 Application Key (secret)
b2.applicationKey=your_application_key

# Default bucket name (optional but recommended)
b2.defaultBucketName=my-bucket
```

### Optional Configuration

```properties
# Enable or disable B2 integration (default: true)
b2.enabled=true

# Connection timeout in seconds (default: 30)
b2.connectionTimeoutSeconds=30

# Socket timeout in seconds (default: 60)
b2.socketTimeoutSeconds=60

# Maximum number of retry attempts (default: 3)
b2.maxRetries=3

# User agent for API requests (default: b2-spring-boot-starter/0.0.1)
b2.userAgent=my-app/1.0
```

### Using YAML Configuration

```yaml
b2:
  enabled: true
  applicationKeyId: your_application_key_id
  applicationKey: your_application_key
  defaultBucketName: my-bucket
  connectionTimeoutSeconds: 30
  socketTimeoutSeconds: 60
  maxRetries: 3
  userAgent: my-app/1.0
```

### Environment Variables

For production, use environment variables to avoid storing credentials in configuration files:

```properties
b2.applicationKeyId=${B2_APPLICATION_KEY_ID}
b2.applicationKey=${B2_APPLICATION_KEY}
b2.defaultBucketName=${B2_DEFAULT_BUCKET}
```

## Usage

### Inject B2Service

Once configured, inject the `B2Service` bean into your Spring components:

```java
import com.anode.b2.B2Service;
import org.springframework.stereotype.Service;

@Service
public class FileStorageService {

    private final B2Service b2Service;

    public FileStorageService(B2Service b2Service) {
        this.b2Service = b2Service;
    }

    // Use b2Service methods...
}
```

### Upload Files

```java
import java.nio.file.Path;
import java.nio.file.Paths;

// Upload to default bucket
Path localFile = Paths.get("/path/to/local/file.txt");
b2Service.uploadFile(localFile, "remote-file.txt");

// Upload to specific bucket
b2Service.uploadFile(localFile, "remote-file.txt", "specific-bucket");
```

### Download Files

```java
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

// Download from default bucket
InputStream inputStream = b2Service.downloadFile("remote-file.txt");

// Save to local file
Path outputPath = Paths.get("/path/to/output/file.txt");
Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);

// Download from specific bucket
InputStream stream = b2Service.downloadFile("remote-file.txt", "specific-bucket");
```

### List Files

```java
import java.util.List;

// List up to 100 files in default bucket
List<String> files = b2Service.listFiles(100);

// List files with prefix
List<String> filteredFiles = b2Service.listFiles("documents/", 50);

// List files in specific bucket
List<String> bucketFiles = b2Service.listFiles("specific-bucket", "prefix/", 100);
```

### Delete Files

```java
// Delete from default bucket
b2Service.deleteFile("remote-file.txt");

// Delete from specific bucket
b2Service.deleteFile("remote-file.txt", "specific-bucket");
```

### Check File Existence

```java
// Check in default bucket
boolean exists = b2Service.fileExists("remote-file.txt");

// Check in specific bucket
boolean exists = b2Service.fileExists("remote-file.txt", "specific-bucket");
```

### Get File Information

```java
import com.backblaze.b2.client.structures.B2FileVersion;

// Get file info from default bucket
B2FileVersion fileInfo = b2Service.getFileInfo("remote-file.txt");
System.out.println("File ID: " + fileInfo.getFileId());
System.out.println("File size: " + fileInfo.getContentLength());
System.out.println("Upload timestamp: " + fileInfo.getUploadTimestamp());

// Get file info from specific bucket
B2FileVersion info = b2Service.getFileInfo("remote-file.txt", "specific-bucket");
```

### Advanced Usage - Direct Client Access

For advanced B2 operations not covered by `B2Service`, access the underlying client:

```java
import com.backblaze.b2.client.B2StorageClient;

B2StorageClient client = b2Service.getClient();
// Use client directly for advanced operations
```

## Complete Example

```java
package com.example.app;

import com.anode.b2.B2Service;
import com.backblaze.b2.client.exceptions.B2Exception;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class B2DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(B2DemoApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(B2Service b2Service) {
        return args -> {
            try {
                // Upload a file
                Path localFile = Paths.get("example.txt");
                Files.writeString(localFile, "Hello from B2 Spring Boot Starter!");
                b2Service.uploadFile(localFile, "example.txt");
                System.out.println("File uploaded successfully");

                // List files
                List<String> files = b2Service.listFiles(10);
                System.out.println("Files in bucket: " + files);

                // Check if file exists
                boolean exists = b2Service.fileExists("example.txt");
                System.out.println("File exists: " + exists);

                // Download the file
                InputStream inputStream = b2Service.downloadFile("example.txt");
                Path downloadPath = Paths.get("downloaded-example.txt");
                Files.copy(inputStream, downloadPath);
                System.out.println("File downloaded successfully");

                // Clean up
                b2Service.deleteFile("example.txt");
                System.out.println("File deleted successfully");

            } catch (B2Exception | IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        };
    }
}
```

## Error Handling

All B2 operations throw `B2Exception` for B2-specific errors. Always wrap calls in try-catch blocks:

```java
import com.backblaze.b2.client.exceptions.B2Exception;

try {
    b2Service.uploadFile(localPath, "remote-file.txt");
} catch (B2Exception e) {
    // Handle B2-specific errors (auth, network, etc.)
    logger.error("B2 error: " + e.getMessage(), e);
} catch (IOException e) {
    // Handle file I/O errors
    logger.error("I/O error: " + e.getMessage(), e);
}
```

## Common B2 Exceptions

- `B2UnauthorizedException` - Invalid credentials or expired authorization
- `B2NotFoundException` - Bucket or file not found
- `B2BadRequestException` - Invalid request parameters
- `B2NetworkException` - Network connectivity issues

## Getting B2 Credentials

1. Sign up for a Backblaze account at https://www.backblaze.com/b2/sign-up.html
2. Navigate to "App Keys" in your B2 dashboard
3. Create a new application key with appropriate permissions
4. Copy the Application Key ID and Application Key
5. Create a bucket in the B2 dashboard

## Security Best Practices

1. **Never commit credentials** to version control
2. Use **environment variables** for credentials in production
3. Use **application keys with minimal permissions** (not master key)
4. **Restrict key access** to specific buckets when possible
5. **Rotate keys regularly**

Example with restricted permissions:

```properties
# Use environment variables
b2.applicationKeyId=${B2_APP_KEY_ID}
b2.applicationKey=${B2_APP_KEY}
```

## Testing

The starter includes comprehensive tests. Run with:

```bash
mvn test
```

### Unit Tests

- `B2PropertiesTest` - Tests configuration properties
- `B2AutoConfigurationTest` - Tests auto-configuration

## Configuration Properties Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `b2.enabled` | boolean | `true` | Enable/disable B2 integration |
| `b2.applicationKeyId` | String | `null` | B2 Application Key ID (required) |
| `b2.applicationKey` | String | `null` | B2 Application Key (required) |
| `b2.defaultBucketName` | String | `null` | Default bucket for operations |
| `b2.connectionTimeoutSeconds` | int | `30` | Connection timeout |
| `b2.socketTimeoutSeconds` | int | `60` | Socket timeout |
| `b2.maxRetries` | int | `3` | Maximum retry attempts |
| `b2.userAgent` | String | `b2-spring-boot-starter/0.0.1` | User agent string |

## B2Service API Reference

### Upload Operations
- `uploadFile(Path localFilePath, String remoteFileName)` - Upload to default bucket
- `uploadFile(Path localFilePath, String remoteFileName, String bucketName)` - Upload to specific bucket

### Download Operations
- `downloadFile(String fileName)` - Download from default bucket
- `downloadFile(String fileName, String bucketName)` - Download from specific bucket

### List Operations
- `listFiles(int maxCount)` - List files in default bucket
- `listFiles(String prefix, int maxCount)` - List files with prefix
- `listFiles(String bucketName, String prefix, int maxCount)` - List files in specific bucket

### Delete Operations
- `deleteFile(String fileName)` - Delete from default bucket
- `deleteFile(String fileName, String bucketName)` - Delete from specific bucket

### Info Operations
- `getFileInfo(String fileName)` - Get file info from default bucket
- `getFileInfo(String fileName, String bucketName)` - Get file info from specific bucket
- `fileExists(String fileName)` - Check existence in default bucket
- `fileExists(String fileName, String bucketName)` - Check existence in specific bucket

### Advanced Operations
- `getClient()` - Get underlying B2StorageClient for advanced operations
- `close()` - Close client connection (called automatically by Spring)

## Troubleshooting

### Authentication Errors

**Problem**: `B2UnauthorizedException` when connecting

**Solutions**:
1. Verify credentials are correct
2. Check that the application key has not expired
3. Ensure the key has permissions for the bucket

### Bucket Not Found

**Problem**: `Bucket not found` error

**Solutions**:
1. Verify the bucket name is correct
2. Ensure the bucket exists in your B2 account
3. Check that your application key has access to the bucket

### Connection Timeouts

**Problem**: Operations timing out

**Solutions**:
1. Increase timeout values in configuration
2. Check network connectivity
3. Verify firewall settings allow B2 connections

### Large File Uploads

The current implementation uses `uploadSmallFile` which is suitable for files up to 5GB. For larger files, access the B2 client directly and use the large file API.

## Dependencies

- **B2 SDK Core**: 6.2.1
- **B2 SDK HTTP Client**: 6.2.1
- **Spring Boot**: 3.4.2

## Disabling B2 Integration

To disable B2 integration:

```properties
b2.enabled=false
```

## License

This project is licensed under the terms specified in the repository.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## Resources

- [Backblaze B2 Documentation](https://www.backblaze.com/b2/docs/)
- [B2 Java SDK](https://github.com/Backblaze/b2-sdk-java)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

## Support

For issues related to:
- **This starter**: Open an issue in this repository
- **B2 service**: Contact Backblaze support
- **B2 SDK**: Check the [B2 SDK repository](https://github.com/Backblaze/b2-sdk-java)
