# PF4J Spring Boot Starter

Spring Boot starter for PF4J plugin framework integration.

## Usage

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>spring-boot-starter-plugin</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Configure in your `application.properties`:

```properties
plugins.enabled=true
plugins.plugins-root-folder=plugins
```

Inject and use the `PluginManager`:

```java
@Autowired
private PluginManager pluginManager;

public void loadExtensions() {
    List<GreetingExtension> greetings = pluginManager.getExtensions(GreetingExtension.class);
    greetings.forEach(ext -> System.out.println(ext.getGreeting()));
}
```
