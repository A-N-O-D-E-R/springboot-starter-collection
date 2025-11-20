# Spring Boot Starters Collection

A collection of custom Spring Boot starters for seamless integration with various services and frameworks.

## Overview

This repository contains multiple Spring Boot starters that simplify the integration of external services into your Spring Boot applications through auto-configuration.

## Available Starters

### 1. PF4J Spring Boot Starter

**Location**: `plugin-springboot-starter/`

A Spring Boot starter for [PF4J](https://pf4j.org/) (Plugin Framework for Java) that enables plugin-based architecture in your Spring Boot applications.

**Features**:
- Auto-configuration of PF4J PluginManager
- Automatic plugin loading and starting at application startup
- Configurable plugin directory
- Enable/disable plugin system via configuration
- Spring Boot 3.4.2 compatible
- Java 21 support

**Quick Start**:
```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>plugin-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

```properties
plugins.enabled=true
plugins.pluginsRootFolder=plugins
```

[View Full Documentation](plugin-springboot-starter/README.md)

### 2. Backblaze B2 Spring Boot Starter

**Location**: `b2-springboot-starter/`

A Spring Boot starter for [Backblaze B2](https://www.backblaze.com/b2/cloud-storage.html) cloud storage integration.

**Features**:
- Auto-configuration of Backblaze B2 client
- Simple service layer for file operations (upload, download, delete)
- Configurable timeouts and retry logic
- Support for multiple buckets
- Spring Boot 3.4.2 compatible
- Java 21 support

**Quick Start**:
```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>b2-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

```properties
b2.applicationKeyId=your_key_id
b2.applicationKey=your_secret_key
b2.defaultBucketName=my-bucket
```

[View Full Documentation](b2-springboot-starter/README.md)

## Examples

Working example applications are available in the `.exemples/` directory:

### PF4J Plugin Example

**Location**: `.exemples/app/` and `.exemples/plugin/`

A complete example demonstrating:
- Spring Boot REST API with plugin support
- Extension point pattern implementation
- Multi-language greeting plugin with 3 implementations (English, French, Spanish)
- Plugin discovery and loading
- One-command setup and run

[View Example Documentation](.exemples/README.md)

### B2 Cloud Storage Example

**Location**: `.exemples/b2/`

A REST API demonstrating:
- File upload to B2 cloud storage
- File download from B2
- File deletion
- Multi-bucket support
- Complete demo workflow

[View Example Documentation](.exemples/b2/README.md)

## Quick Start Guide

See [QUICKSTART.md](QUICKSTART.md) for a 5-minute guide to get started with the PF4J starter.

## Project Structure

```
.
├── plugin-springboot-starter/     # PF4J Spring Boot Starter
│   ├── src/
│   ├── pom.xml
│   └── README.md
│
├── b2-springboot-starter/         # Backblaze B2 Spring Boot Starter
│   ├── src/
│   ├── pom.xml
│   └── README.md
│
├── .exemples/                     # Example applications
│   ├── app/                       # PF4J demo application
│   ├── plugin/                    # Example plugin
│   ├── b2/                        # B2 demo application
│   └── README.md
│
├── QUICKSTART.md                  # Quick start guide
└── README.md                      # This file
```

## Requirements

- **Java**: 21 or higher
- **Spring Boot**: 3.4.2 or higher
- **Maven**: 3.6+

## Installation

### Install All Starters

```bash
# Install PF4J starter
cd plugin-springboot-starter
mvn clean install

# Install B2 starter
cd ../b2-springboot-starter
mvn clean install
```

### Install Specific Starter

```bash
# PF4J only
cd plugin-springboot-starter
mvn clean install

# B2 only
cd b2-springboot-starter
mvn clean install
```

## Usage Examples

### Using PF4J Starter

```java
import org.pf4j.PluginManager;
import org.springframework.stereotype.Service;

@Service
public class MyService {
    private final PluginManager pluginManager;

    public MyService(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void usePlugins() {
        List<MyExtension> extensions =
            pluginManager.getExtensions(MyExtension.class);
        extensions.forEach(MyExtension::doSomething);
    }
}
```

### Using B2 Starter

```java
import com.anode.b2.B2Service;
import org.springframework.stereotype.Service;

@Service
public class FileService {
    private final B2Service b2Service;

    public FileService(B2Service b2Service) {
        this.b2Service = b2Service;
    }

    public void uploadFile() throws Exception {
        Path file = Paths.get("example.txt");
        b2Service.uploadFile(file, "remote-file.txt");
    }
}
```

## Running Examples

### PF4J Plugin Example

```bash
cd .exemples
./build-and-run.sh
```

Then test:
```bash
curl "http://localhost:8080/api/greet?name=John"
```

### B2 Cloud Storage Example

```bash
cd .exemples/b2
# Configure your B2 credentials in application.properties
mvn spring-boot:run
```

Then test:
```bash
curl -X POST "http://localhost:8081/api/files/demo"
```

## Testing

Each starter includes comprehensive tests:

### PF4J Starter Tests
```bash
cd plugin-springboot-starter
mvn test
```
**Results**: 16 tests - All passing ✓

### B2 Starter Tests
```bash
cd b2-springboot-starter
mvn test
```
**Results**: 13 tests - All passing ✓

## Configuration Reference

### PF4J Starter Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `plugins.enabled` | boolean | `true` | Enable/disable plugin system |
| `plugins.pluginsRootFolder` | String | `"plugins"` | Root folder for plugins |

### B2 Starter Configuration

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

## Architecture

### Auto-Configuration Pattern

All starters follow Spring Boot's auto-configuration pattern:

1. **Properties Class**: `@ConfigurationProperties` for external configuration
2. **Auto-Configuration Class**: `@Configuration` with conditional bean creation
3. **Service Layer**: High-level service beans for common operations
4. **META-INF Registration**: Auto-configuration registered in `AutoConfiguration.imports`

### Example Auto-Configuration Flow

```
Application Startup
    ↓
Spring Boot detects AutoConfiguration.imports
    ↓
Conditional checks (@ConditionalOnProperty, @ConditionalOnClass)
    ↓
Beans created (Client, Service, etc.)
    ↓
Ready for injection into your components
```

## Development

### Project Standards

- **Java Version**: 21
- **Spring Boot Version**: 3.4.2
- **Code Style**: Standard Java conventions
- **Testing**: JUnit 5 with Spring Boot Test
- **Documentation**: Comprehensive README for each module

### Adding a New Starter

1. Create a new module directory
2. Add `pom.xml` with dependencies
3. Create configuration properties class
4. Create auto-configuration class
5. Create service layer
6. Add `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
7. Write tests
8. Document in README.md

## Troubleshooting

### PF4J Issues

**Problem**: Plugins not loading

**Solutions**:
1. Check `plugins.enabled=true`
2. Verify plugin directory exists
3. Check application logs
4. Ensure plugin manifest is correct

### B2 Issues

**Problem**: Authentication errors

**Solutions**:
1. Verify credentials are correct
2. Check application key hasn't expired
3. Ensure key has bucket permissions

See individual starter READMEs for more troubleshooting tips.

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Update documentation
5. Submit a pull request

## Security Best Practices

### For PF4J
- Review plugin code before loading
- Use plugin versioning
- Implement plugin sandboxing if needed
- Monitor plugin behavior

### For B2
- Never commit credentials to version control
- Use environment variables for production
- Use application keys with minimal permissions
- Rotate keys regularly
- Use private buckets for sensitive data

## License

This project is licensed under the terms specified in the repository.

## Resources

### PF4J Resources
- [PF4J Documentation](https://pf4j.org/)
- [Spring Boot Auto-Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)

### B2 Resources
- [Backblaze B2 Documentation](https://www.backblaze.com/b2/docs/)
- [B2 Java SDK](https://github.com/Backblaze/b2-sdk-java)

### Spring Boot Resources
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Creating Your Own Auto-Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)

## Support

For issues:
- **PF4J Starter**: See [plugin-springboot-starter/README.md](plugin-springboot-starter/README.md)
- **B2 Starter**: See [b2-springboot-starter/README.md](b2-springboot-starter/README.md)
- **General**: Open an issue in this repository

## Changelog

### Version 0.0.1-SNAPSHOT

**PF4J Spring Boot Starter**:
- Initial release
- Auto-configuration support
- Basic plugin management
- 16 passing tests

**B2 Spring Boot Starter**:
- Initial release
- Auto-configuration support
- File upload/download/delete operations
- Multi-bucket support
- 13 passing tests

## Roadmap

### Planned Features

**PF4J Starter**:
- [ ] Plugin hot-reload support
- [ ] Plugin dependency management
- [ ] Enhanced security features
- [ ] Plugin metrics and monitoring

**B2 Starter**:
- [ ] Large file upload support (>5GB)
- [ ] File streaming support
- [ ] Bucket management operations
- [ ] File listing with pagination
- [ ] CDN integration

**General**:
- [ ] Spring Boot 3.5+ compatibility
- [ ] Reactive programming support
- [ ] Cloud-native features (health checks, metrics)
- [ ] Additional starters for other services

## Acknowledgments

- Spring Boot team for the excellent framework
- PF4J team for the plugin framework
- Backblaze for B2 cloud storage and SDK
- Contributors and users of these starters

---

**Happy Coding!** 🚀
