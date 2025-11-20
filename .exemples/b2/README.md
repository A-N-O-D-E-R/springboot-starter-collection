# Backblaze B2 Demo Application

A Spring Boot REST API demonstrating the use of the Backblaze B2 Spring Boot Starter for cloud file storage operations.

## Overview

This example application showcases:
- File upload to B2 cloud storage
- File download from B2
- File deletion
- Storage information retrieval
- Complete demo workflow

## Prerequisites

1. **Java 21** or higher
2. **Maven 3.6+**
3. **Backblaze B2 Account**
   - Sign up at https://www.backblaze.com/b2/sign-up.html
   - Create an application key with appropriate permissions
   - Create a bucket for testing

## Setup

### 1. Install the B2 Spring Boot Starter

First, install the B2 starter to your local Maven repository:

```bash
cd ../../b2-springboot-starter
mvn clean install
```

### 2. Configure B2 Credentials

Edit `src/main/resources/application.properties` and add your B2 credentials:

```properties
b2.applicationKeyId=YOUR_APPLICATION_KEY_ID_HERE
b2.applicationKey=YOUR_APPLICATION_KEY_HERE
b2.defaultBucketName=YOUR_BUCKET_NAME_HERE
```

**Security Note**: For production, use environment variables instead:

```properties
b2.applicationKeyId=${B2_APPLICATION_KEY_ID}
b2.applicationKey=${B2_APPLICATION_KEY}
b2.defaultBucketName=${B2_DEFAULT_BUCKET}
```

Then set the environment variables:
```bash
export B2_APPLICATION_KEY_ID=your_key_id
export B2_APPLICATION_KEY=your_secret_key
export B2_DEFAULT_BUCKET=your_bucket_name
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will start on http://localhost:8081

## API Endpoints

### 1. Upload File

Upload a file to B2 cloud storage.

```bash
curl -X POST -F "file=@/path/to/your/file.txt" http://localhost:8081/api/files/upload
```

Response:
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "fileName": "file.txt",
  "fileId": "4_z27c88d2f74b6e5_f123abc...",
  "size": 1024
}
```

Upload to a specific bucket:
```bash
curl -X POST -F "file=@file.txt" "http://localhost:8081/api/files/upload?bucket=my-other-bucket"
```

### 2. Download File

Download a file from B2.

```bash
curl -O -J "http://localhost:8081/api/files/download/file.txt"
```

Download from a specific bucket:
```bash
curl -O -J "http://localhost:8081/api/files/download/file.txt?bucket=my-other-bucket"
```

### 3. Delete File

Delete a file from B2.

```bash
curl -X DELETE "http://localhost:8081/api/files/file.txt"
```

Response:
```json
{
  "success": true,
  "message": "File deleted successfully",
  "fileName": "file.txt"
}
```

Delete from a specific bucket:
```bash
curl -X DELETE "http://localhost:8081/api/files/file.txt?bucket=my-other-bucket"
```

### 4. Get Storage Info

Get information about your B2 bucket.

```bash
curl "http://localhost:8081/api/files/info"
```

Response:
```json
{
  "success": true,
  "bucketName": "my-bucket",
  "bucketId": "abc123...",
  "bucketType": "allPrivate",
  "accountId": "123456789abc"
}
```

### 5. Run Demo

Run a complete demo that creates, uploads, downloads, and deletes a test file.

```bash
curl -X POST "http://localhost:8081/api/files/demo"
```

Response:
```json
{
  "success": true,
  "message": "Demo completed successfully!",
  "step1_create": "Created test file: test-file-20250120-235959.txt",
  "step2_upload": {
    "fileName": "test-file-20250120-235959.txt",
    "fileId": "4_z27c88d2f74b6e5_f123...",
    "size": 123,
    "contentType": "application/octet-stream"
  },
  "step3_download": {
    "size": 123,
    "content": "This is a test file created at 2025-01-20T23:59:59..."
  },
  "step4_info": {
    "fileName": "test-file-20250120-235959.txt",
    "fileId": "4_z27c88d2f74b6e5_f123...",
    "uploadTimestamp": 1737417599000
  },
  "step5_delete": "File deleted successfully"
}
```

## Project Structure

```
b2/
├── src/
│   └── main/
│       ├── java/com/example/b2/
│       │   ├── B2DemoApplication.java              # Main application
│       │   ├── controller/
│       │   │   └── FileController.java             # REST endpoints
│       │   └── service/
│       │       └── FileStorageService.java         # Business logic
│       └── resources/
│           ├── application.properties               # Configuration
│           └── application-example.properties       # Example config
├── uploads/                                         # Temporary upload directory
└── pom.xml
```

## How It Works

### File Upload Flow

1. Client sends multipart file via REST API
2. File is temporarily saved to `uploads/` directory
3. `FileStorageService` uses `B2Service` to upload to B2
4. Temporary file is deleted
5. File ID and metadata are returned to client

### File Download Flow

1. Client requests file by name
2. `B2Service` downloads file as byte array
3. File is returned with appropriate headers

### File Deletion Flow

1. Client requests file deletion by name
2. `B2Service` retrieves file info to get file ID
3. File is deleted using file ID and name
4. Success response is returned

## Configuration Options

All configuration is in `application.properties`:

```properties
# Required B2 Configuration
b2.applicationKeyId=YOUR_KEY_ID
b2.applicationKey=YOUR_SECRET_KEY
b2.defaultBucketName=YOUR_BUCKET

# Optional B2 Configuration
b2.enabled=true                      # Enable/disable B2
b2.connectionTimeoutSeconds=30       # Connection timeout
b2.socketTimeoutSeconds=60           # Socket timeout
b2.maxRetries=3                      # Max retry attempts
b2.userAgent=b2-demo-app/1.0        # User agent string

# File Upload Limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

## Testing with cURL

### Upload a Text File

```bash
echo "Hello from B2!" > test.txt
curl -X POST -F "file=@test.txt" http://localhost:8081/api/files/upload
```

### Download and View

```bash
curl "http://localhost:8081/api/files/download/test.txt" -o downloaded.txt
cat downloaded.txt
```

### Delete the File

```bash
curl -X DELETE "http://localhost:8081/api/files/test.txt"
```

## Testing with Postman

### Upload File
- Method: `POST`
- URL: `http://localhost:8081/api/files/upload`
- Body: form-data
  - Key: `file` (type: File)
  - Value: Select your file

### Download File
- Method: `GET`
- URL: `http://localhost:8081/api/files/download/yourfile.txt`
- Send and Save Response

### Delete File
- Method: `DELETE`
- URL: `http://localhost:8081/api/files/yourfile.txt`

## Error Handling

The application includes comprehensive error handling:

```json
{
  "success": false,
  "message": "Upload failed: Bucket not found: invalid-bucket"
}
```

Common errors:
- **401 Unauthorized**: Invalid B2 credentials
- **404 Not Found**: File or bucket doesn't exist
- **500 Internal Server Error**: Upload/download/delete failure

## Advanced Usage

### Using Multiple Buckets

You can work with multiple buckets by specifying the bucket parameter:

```bash
# Upload to different buckets
curl -X POST -F "file=@file.txt" "http://localhost:8081/api/files/upload?bucket=bucket-1"
curl -X POST -F "file=@file.txt" "http://localhost:8081/api/files/upload?bucket=bucket-2"

# Download from specific bucket
curl "http://localhost:8081/api/files/download/file.txt?bucket=bucket-1" -O
```

### Accessing B2Service Directly

For advanced operations, inject `B2Service` in your own services:

```java
@Service
public class MyCustomService {
    private final B2Service b2Service;

    public MyCustomService(B2Service b2Service) {
        this.b2Service = b2Service;
    }

    public void customOperation() throws B2Exception {
        // Use B2Service methods
        B2Bucket bucket = b2Service.getDefaultBucket();

        // Or access raw client for advanced features
        B2StorageClient client = b2Service.getClient();
        // ... advanced operations
    }
}
```

## Security Best Practices

1. **Never commit credentials** to version control
2. Use **environment variables** for production
3. Use **application keys with minimal permissions** (not master application key)
4. **Restrict keys** to specific buckets when possible
5. **Rotate keys** regularly
6. Use **private buckets** for sensitive data

## Troubleshooting

### Application won't start

**Error**: `B2 credentials not configured`

**Solution**: Ensure you've set valid credentials in `application.properties`

### Upload fails

**Error**: `Bucket not found`

**Solution**:
1. Verify bucket name is correct
2. Ensure bucket exists in your B2 account
3. Check application key has access to the bucket

### Connection timeout

**Error**: Connection timeout errors

**Solution**:
1. Increase timeout values in configuration
2. Check network connectivity
3. Verify firewall allows B2 connections

### File not found on download

**Error**: `File not found`

**Solution**:
1. Verify file name is exact (case-sensitive)
2. Check you're using the correct bucket
3. Ensure file was uploaded successfully

## Performance Tips

1. **Parallel uploads**: Upload multiple files concurrently
2. **Large files**: For files > 100MB, consider using B2's large file API directly
3. **Caching**: Implement caching for frequently accessed files
4. **CDN**: Use Backblaze's CDN for public files

## Resources

- [Backblaze B2 Documentation](https://www.backblaze.com/b2/docs/)
- [B2 Java SDK](https://github.com/Backblaze/b2-sdk-java)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [B2 Spring Boot Starter README](../../b2-springboot-starter/README.md)

## License

This example application is provided for educational purposes.

## Support

For issues:
- **B2 Starter**: Check the [B2 starter README](../../b2-springboot-starter/README.md)
- **B2 Service**: Contact Backblaze support
- **This Example**: Open an issue in the repository
