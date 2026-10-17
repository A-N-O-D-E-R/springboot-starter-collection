package com.anode.autoconfiguration.security;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal web application protected by the starter. */
@SpringBootApplication
public class SecurityTestApplication {

    @RestController
    static class Endpoints {
        @GetMapping("/whoami")
        Map<String, Object> whoami(Authentication authentication) {
            return Map.of(
                    "name", authentication.getName(),
                    "authorities", authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority).sorted().collect(Collectors.toList()));
        }

        @PostMapping("/things")
        String create() {
            return "created";
        }

        @GetMapping("/public/ping")
        String ping() {
            return "pong";
        }
    }
}
