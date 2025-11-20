package com.example.app.controller;

import com.example.app.service.GreetingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GreetingController {

    private final GreetingService greetingService;

    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping("/greet")
    public Map<String, Object> greet(@RequestParam(defaultValue = "World") String name) {
        Map<String, Object> response = new HashMap<>();
        response.put("name", name);
        response.put("greetings", greetingService.getAllGreetings(name));
        response.put("pluginCount", greetingService.getGreetingPluginCount());
        return response;
    }

    @GetMapping("/plugins")
    public Map<String, Object> getPlugins() {
        Map<String, Object> response = new HashMap<>();
        response.put("plugins", greetingService.getLoadedPluginsInfo());
        response.put("count", greetingService.getGreetingPluginCount());
        return response;
    }
}
