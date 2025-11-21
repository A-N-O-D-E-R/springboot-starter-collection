# Quick Start Guide

Get started with the PF4J Spring Boot Starter in 5 minutes!

## Prerequisites

- Java 21
- Maven 3.6+

## Option 1: Run the Example (Fastest)

```bash
cd exemples
./build-and-run.sh
```

This script will:
1. Install the PF4J Spring Boot Starter
2. Build the example plugin
3. Set up the application
4. Start the Spring Boot app on http://localhost:8080

### Test the Example

```bash
# Check loaded plugins
curl http://localhost:8080/api/plugins

# Get greetings from all plugins
curl "http://localhost:8080/api/greet?name=John"
```

Expected output:
```json
{
  "name": "John",
  "greetings": [
    "English: Hello, John!",
    "French: Bonjour, John !",
    "Spanish: ¡Hola, John!"
  ],
  "pluginCount": 3
}
```

## Option 2: Manual Setup

### 1. Install the Starter

```bash
cd springboot-starter-collection
mvn clean install
```

### 2. Add to Your Project

Add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>spring-boot-starter-plugin</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 3. Configure

Add to `application.properties`:

```properties
plugins.enabled=true
plugins.pluginsRootFolder=plugins
```

### 4. Define Extension Point

```java
import org.pf4j.ExtensionPoint;

public interface MyExtension extends ExtensionPoint {
    void doSomething();
}
```

### 5. Use in Your Service

```java
import org.pf4j.PluginManager;
import org.springframework.stereotype.Service;

@Service
public class MyService {
    private final PluginManager pluginManager;

    public MyService(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void useExtensions() {
        List<MyExtension> extensions =
            pluginManager.getExtensions(MyExtension.class);
        extensions.forEach(MyExtension::doSomething);
    }
}
```

### 6. Create a Plugin

```java
// Plugin main class
public class MyPlugin extends Plugin {
    public MyPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
}

// Extension implementation
@Extension
public class MyExtensionImpl implements MyExtension {
    @Override
    public void doSomething() {
        System.out.println("Plugin doing something!");
    }
}
```

### 7. Package and Deploy

Build your plugin:
```bash
mvn clean package
```

Copy to plugins directory:
```bash
cp target/my-plugin-1.0.0.jar /path/to/app/plugins/
```

## Next Steps

- Read the [full README](README.md) for detailed documentation
- Explore the [examples](exemples/README.md) for complete working code
- Check out [PF4J documentation](https://pf4j.org/) for advanced features

## Troubleshooting

### Plugins not loading?

1. Check `plugins.enabled=true` in your configuration
2. Verify the plugins directory exists and contains JAR files
3. Check application logs for errors
4. Ensure plugin manifest entries are correct in pom.xml

### Need help?

- Review the examples in `exemples/` directory
- Check the [troubleshooting section](README.md#troubleshooting) in the README
- Review PF4J documentation at https://pf4j.org/

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `plugins.enabled` | `true` | Enable/disable plugin system |
| `plugins.pluginsRootFolder` | `plugins` | Plugin directory (relative or absolute) |

## Architecture

```
Application
    ├── Extension Points (Interfaces)
    ├── Services (Using extensions)
    └── PluginManager (Auto-configured)
            ├── Plugin 1
            │   ├── Extension Impl A
            │   └── Extension Impl B
            └── Plugin 2
                └── Extension Impl C
```

Happy plugin development!
