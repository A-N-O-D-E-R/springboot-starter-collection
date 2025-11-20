package com.example.plugin;

import com.example.app.extension.GreetingExtension;
import org.pf4j.Extension;

/**
 * French greeting extension.
 */
@Extension
public class FrenchGreeting implements GreetingExtension {

    @Override
    public String greet(String name) {
        return "Bonjour, " + name + " !";
    }

    @Override
    public String getGreetingType() {
        return "French";
    }
}
