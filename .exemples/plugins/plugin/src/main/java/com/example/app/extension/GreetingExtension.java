package com.example.app.extension;

import org.pf4j.ExtensionPoint;

/**
 * Extension point for greeting functionality.
 * Plugins can implement this interface to provide custom greeting behavior.
 */
public interface GreetingExtension extends ExtensionPoint {

    /**
     * Returns a greeting message.
     *
     * @param name the name to greet
     * @return the greeting message
     */
    String greet(String name);

    /**
     * Returns the name/identifier of this greeting implementation.
     *
     * @return the greeting type name
     */
    String getGreetingType();
}
