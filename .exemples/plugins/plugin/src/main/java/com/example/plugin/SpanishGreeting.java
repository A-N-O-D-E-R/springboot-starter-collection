package com.example.plugin;

import com.example.app.extension.GreetingExtension;
import org.pf4j.Extension;

/**
 * Spanish greeting extension.
 */
@Extension
public class SpanishGreeting implements GreetingExtension {

    @Override
    public String greet(String name) {
        return "¡Hola, " + name + "!";
    }

    @Override
    public String getGreetingType() {
        return "Spanish";
    }
}
