package com.example.plugin;

import com.example.app.extension.GreetingExtension;
import org.pf4j.Extension;

/**
 * English greeting extension.
 */
@Extension
public class EnglishGreeting implements GreetingExtension {

    @Override
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    @Override
    public String getGreetingType() {
        return "English";
    }
}
