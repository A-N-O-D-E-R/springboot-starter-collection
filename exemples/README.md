# PF4J Spring Boot Starter - Examples

This directory contains example projects demonstrating how to use the PF4J Spring Boot Starter.

## Overview

The examples consist of two projects:

1. **app** - A Spring Boot application that uses the PF4J starter
2. **plugin** - An example plugin that extends the application's functionality

## Project Structure

```
exemples/
├── app/                          # Main Spring Boot application
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/example/app/
│   │       │       ├── DemoApplication.java           # Main application class
│   │       │       ├── controller/
│   │       │       │   └── GreetingController.java    # REST controller
│   │       │       ├── service/
│   │       │       │   └── GreetingService.java       # Business logic
│   │       │       └── extension/
│   │       │           └── GreetingExtension.java     # Extension point interface
│   │       └── resources/
│   │           └── application.properties             # Configuration
│   └── pom.xml
│
└── plugin/                       # Example plugin
    ├── src/
    │   └── main/
    │       └── java/
    │           └── com/example/plugin/
    │               ├── GreetingPlugin.java            # Main plugin class
    │               ├── EnglishGreeting.java           # English greeting extension
    │               ├── FrenchGreeting.java            # French greeting extension
    │               └── SpanishGreeting.java           # Spanish greeting extension
    └── pom.xml
```

## Getting Started

### Step 1: Install the PF4J Spring Boot Starter

First, install the starter to your local Maven repository:

```bash
cd ../plugin-springboot-starter
mvn clean install
```

### Step 2: Build the Plugin

Build the example plugin:

```bash
cd ../exemples/plugin
mvn clean package
```

This creates `greeting-plugin-1.0.0.jar` in the `target/` directory.

### Step 3: Set Up the Application

Create a `plugins` directory in the app folder and copy the plugin JAR:

```bash
cd ../app
mkdir -p plugins
cp ../plugin/target/greeting-plugin-1.0.0.jar plugins/
```

### Step 4: Run the Application

```bash
mvn spring-boot:run
```

The application will start on port 8080 and automatically load the greeting plugin.

## Testing the Application

### Check Loaded Plugins

```bash
curl http://localhost:8080/api/plugins
```

Response:
```json
{
  "plugins": [
    "Plugin: greeting-plugin (v1.0.0) - STARTED"
  ],
  "count": 3
}
```

### Get Greetings

```bash
curl "http://localhost:8080/api/greet?name=John"
```

Response:
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

### Test Without Name Parameter

```bash
curl http://localhost:8080/api/greet
```

Response:
```json
{
  "name": "World",
  "greetings": [
    "English: Hello, World!",
    "French: Bonjour, World !",
    "Spanish: ¡Hola, World!"
  ],
  "pluginCount": 3
}
```

## How It Works

### Extension Point Pattern

1. **Define Extension Point** (`GreetingExtension.java`):
   - Interface that extends `ExtensionPoint`
   - Defines the contract for extensions

2. **Implement Extensions** (in the plugin):
   - Classes annotated with `@Extension`
   - Implement the extension point interface

3. **Use Extensions** (in the application):
   - Inject `PluginManager`
   - Call `pluginManager.getExtensions(GreetingExtension.class)`
   - Use the extensions

### Plugin Metadata

The plugin metadata is defined in the `pom.xml` manifest entries:

```xml
<manifestEntries>
    <Plugin-Id>greeting-plugin</Plugin-Id>
    <Plugin-Version>1.0.0</Plugin-Version>
    <Plugin-Provider>Example Plugin Provider</Plugin-Provider>
    <Plugin-Class>com.example.plugin.GreetingPlugin</Plugin-Class>
</manifestEntries>
```

## Creating Your Own Plugin

### 1. Create a New Maven Project

```bash
mvn archetype:generate \
  -DgroupId=com.example.plugin \
  -DartifactId=my-plugin \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

### 2. Add Dependencies

```xml
<dependency>
    <groupId>org.pf4j</groupId>
    <artifactId>pf4j</artifactId>
    <version>3.10.0</version>
    <scope>provided</scope>
</dependency>
```

### 3. Create Plugin Class

```java
public class MyPlugin extends Plugin {
    public MyPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
}
```

### 4. Implement Extensions

```java
@Extension
public class MyExtension implements GreetingExtension {
    @Override
    public String greet(String name) {
        return "My custom greeting: " + name;
    }

    @Override
    public String getGreetingType() {
        return "Custom";
    }
}
```

### 5. Configure Maven Assembly Plugin

Add the plugin configuration from `plugin/pom.xml` to package your plugin with dependencies and proper manifest entries.

### 6. Build and Deploy

```bash
mvn clean package
cp target/my-plugin-1.0.0.jar ../app/plugins/
```

## Configuration Options

Edit `app/src/main/resources/application.properties`:

```properties
# Enable/disable plugin system
plugins.enabled=true

# Change plugin directory (relative or absolute path)
plugins.pluginsRootFolder=plugins

# Or use absolute path
# plugins.pluginsRootFolder=/opt/myapp/plugins
```

## Disabling Plugins

To run the application without plugins:

```properties
plugins.enabled=false
```

Or remove all JARs from the `plugins` directory.

## Hot Reload (Development)

During development, you can:

1. Rebuild the plugin: `cd plugin && mvn package`
2. Copy to plugins dir: `cp target/greeting-plugin-1.0.0.jar ../app/plugins/`
3. Restart the application

For true hot reload, consider using PF4J's development mode features.

## Troubleshooting

### Plugin Not Loading

1. Check the plugin JAR is in the correct directory
2. Verify `plugins.enabled=true`
3. Check application logs for errors
4. Ensure plugin manifest entries are correct

### Extension Not Found

1. Verify the extension class is annotated with `@Extension`
2. Check the extension implements the correct interface
3. Ensure the interface package matches between app and plugin

### ClassNotFoundException

1. Ensure extension point interfaces are available to both app and plugin
2. Consider extracting common interfaces to a separate artifact
3. Check dependency scopes in pom.xml

## Best Practices

1. **Separate Extension Interfaces**: In production, create a separate artifact for extension point interfaces that both the app and plugins depend on

2. **Version Compatibility**: Use semantic versioning for plugins and document compatibility with app versions

3. **Error Handling**: Implement proper error handling for plugin operations

4. **Testing**: Write tests for your plugins independently

5. **Documentation**: Document your extension points clearly

## Further Reading

- [PF4J Documentation](https://pf4j.org/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Plugin Design Patterns](https://en.wikipedia.org/wiki/Plug-in_(computing))

## License

This example project is provided for educational purposes.
