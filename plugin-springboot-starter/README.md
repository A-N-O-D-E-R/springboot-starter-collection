# PF4J Spring Boot Starter

A Spring Boot starter that provides seamless integration with [PF4J](https://github.com/pf4j/pf4j) (Plugin Framework for Java), enabling easy plugin-based architecture in your Spring Boot applications.

## Features

- Auto-configuration of PF4J PluginManager
- Automatic plugin loading and starting at application startup
- Configurable plugin directory
- Enable/disable plugin system via configuration
- Spring Boot 3.4.2 compatible
- Java 21 support

## Requirements

- Java 21 or higher
- Spring Boot 3.4.2 or higher
- Maven 3.6+

## Installation

Add the following dependency to your Spring Boot application's `pom.xml`:

```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>plugin-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Usage

### Basic Configuration

The starter works out of the box with sensible defaults. By default:
- Plugins are **enabled**
- Plugin directory is set to `plugins` (relative to application root)

### Configuration Properties

Configure the plugin system in your `application.properties` or `application.yml`:

#### application.properties
```properties
# Enable or disable plugin system (default: true)
plugins.enabled=true

# Plugin root folder (default: "plugins")
plugins.pluginsRootFolder=/opt/myapp/plugins
```

#### application.yml
```yaml
plugins:
  enabled: true
  pluginsRootFolder: /opt/myapp/plugins
```

### Configuration Options

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `plugins.enabled` | boolean | `true` | Enable or disable the plugin system |
| `plugins.pluginsRootFolder` | String | `"plugins"` | Root folder where plugins are located (relative or absolute path) |

### Using PluginManager

Once the starter is configured, you can inject the `PluginManager` bean into your Spring components:

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
        // Get all loaded plugins
        pluginManager.getPlugins().forEach(plugin -> {
            System.out.println("Plugin: " + plugin.getPluginId());
        });

        // Get extensions
        List<MyExtension> extensions = pluginManager.getExtensions(MyExtension.class);
        extensions.forEach(ext -> ext.doSomething());
    }
}
```

### Plugin Lifecycle

The starter automatically:
1. Creates a `PluginManager` bean configured with your specified plugin directory
2. Loads all plugins from the plugin directory at application startup
3. Starts all loaded plugins

The startup sequence is handled by an `ApplicationRunner` bean, ensuring plugins are ready before your application starts processing requests.

## Creating Plugins

To create a plugin compatible with this starter, follow the [PF4J documentation](https://pf4j.org/).

### Basic Plugin Example

1. Create a plugin project with this structure:
```
my-plugin/
├── pom.xml
└── src/main/java/
    └── com/example/
        ├── MyPlugin.java
        └── MyExtension.java
```

2. Define your plugin:
```java
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class MyPlugin extends Plugin {
    public MyPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("MyPlugin started");
    }

    @Override
    public void stop() {
        System.out.println("MyPlugin stopped");
    }
}
```

3. Create your extension:
```java
import org.pf4j.Extension;

@Extension
public class MyExtension {
    public void doSomething() {
        System.out.println("Extension doing something!");
    }
}
```

4. Package your plugin as a JAR and place it in the configured plugins directory.

## Disabling the Plugin System

To disable the plugin system entirely:

```properties
plugins.enabled=false
```

When disabled, the `PluginManager` and related beans will not be created.

## Project Structure

```
plugin-springboot-starter/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/anode/plugin/
│   │   │       ├── PF4JAutoConfiguration.java
│   │   │       └── PluginsProperties.java
│   │   └── resources/
│   │       └── META-INF/
│   │           └── spring/
│   │               └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   └── test/
│       └── java/
│           └── com/anode/plugin/
│               ├── PluginsPropertiesTest.java
│               ├── PF4JAutoConfigurationTest.java
│               └── PF4JAutoConfigurationIntegrationTest.java
└── pom.xml
```

## Testing

The starter includes comprehensive tests:
- **Unit tests** for configuration properties
- **Auto-configuration tests** for Spring context loading
- **Integration tests** for the complete plugin system

Run tests with:
```bash
mvn test
```

## How It Works

1. **Auto-Configuration**: The `PF4JAutoConfiguration` class is automatically discovered by Spring Boot via the `AutoConfiguration.imports` file
2. **Conditional Loading**: The configuration only activates when `plugins.enabled=true` (default)
3. **Bean Creation**: Creates a `PluginManager` bean configured with the specified plugin directory
4. **Automatic Startup**: An `ApplicationRunner` bean ensures plugins are loaded and started when the application starts

## Dependencies

- **PF4J**: 3.10.0
- **Spring Boot**: 3.4.2
- **Java**: 21

## Troubleshooting

### Plugins not loading

1. Check that the plugin directory exists and is accessible
2. Verify `plugins.enabled=true`
3. Check application logs for plugin loading errors
4. Ensure plugins are compatible with PF4J 3.10.0

### Plugin directory not found

If using a relative path, remember it's relative to the application's working directory. Consider using an absolute path for production deployments:

```properties
plugins.pluginsRootFolder=/opt/myapp/plugins
```

## License

This project is licensed under the terms specified in the repository.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## References

- [PF4J Documentation](https://pf4j.org/)
- [Spring Boot Auto-Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
