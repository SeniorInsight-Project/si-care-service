package com.seniorinsight.backendcore;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "SeniorInsight Backend Core",
                "status", "ONLINE",
                "version", "0.0.1-SNAPSHOT",
                "healthCheck", "/api/v1/health"
        ));
    }

    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "backend-core",
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }
}
