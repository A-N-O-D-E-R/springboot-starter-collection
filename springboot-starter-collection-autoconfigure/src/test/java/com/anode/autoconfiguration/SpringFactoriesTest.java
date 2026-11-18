package com.anode.autoconfiguration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * An app that uses one starter (say Jira) must start without the others. A {@code spring.factories} that lists a class
 * from another module ({@code EnvironmentPostProcessor}s are instantiated unconditionally) breaks every app that
 * lacks that module: so each file may only list classes found next to it, in the same jar or classes directory.
 */
class SpringFactoriesTest {

    @Test
    void everyClassListedInASpringFactoriesIsInTheSameArtifact() throws IOException {
        var problems = new ArrayList<String>();
        var urls = Thread.currentThread().getContextClassLoader().getResources("META-INF/spring.factories");
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if (!url.toString().contains("springboot-starter-collection") && !url.toString().contains("/target/")) {
                continue; // someone else's (Spring Boot, Spring Security...)
            }
            var factories = new Properties();
            try (InputStream in = url.openStream()) {
                factories.load(in);
            }
            for (String value : factories.stringPropertyNames().stream().map(factories::getProperty).toList()) {
                for (String className : value.split("\\s*,\\s*")) {
                    String entry = className.strip().replace('.', '/') + ".class";
                    if (!className.startsWith("com.anode.") || !existsBeside(url, entry)) {
                        if (className.startsWith("com.anode.")) problems.add(url + " lists " + className);
                    }
                }
            }
        }
        assertThat(problems).isEmpty();
    }

    private static boolean existsBeside(URL factories, String classEntry) throws IOException {
        String s = factories.toString();
        if (s.startsWith("jar:")) {
            String jar = s.substring("jar:file:".length(), s.indexOf("!/"));
            try (var zip = new ZipFile(jar)) {
                return zip.getEntry(classEntry) != null;
            }
        }
        Path root = Path.of(java.net.URI.create(s.substring(0, s.length() - "META-INF/spring.factories".length())));
        return Files.exists(root.resolve(classEntry));
    }
}
