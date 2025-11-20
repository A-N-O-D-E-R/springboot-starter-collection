package com.example.plugin;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * Main plugin class for the Greeting Plugin.
 */
public class GreetingPlugin extends Plugin {

    public GreetingPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("GreetingPlugin started!");
    }

    @Override
    public void stop() {
        System.out.println("GreetingPlugin stopped!");
    }
}
