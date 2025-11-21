# Backblaze B2 Spring Boot Starter

Spring Boot starter for Backblaze B2 cloud storage integration.

## Usage

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>spring-boot-starter-b2</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Configure in your `application.properties`:

```properties
b2.application-key-id=your-key-id
b2.application-key=your-secret-key
b2.default-bucket-name=your-bucket
```

Inject and use the `B2Service`:

```java
@Autowired
private B2Service b2Service;

public void uploadFile() throws B2Exception, IOException {
    Path file = Paths.get("/path/to/file.txt");
    b2Service.uploadFile(file, "remote-name.txt");
}
```
