package com.seniorinsight.backendcore;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Health Check & Status", description = "Monitoramento da integridade e informações do serviço")
public class HealthController {

    @Operation(summary = "Status da API", description = "Retorna o status geral da API e links úteis")
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "SeniorInsight Backend Core",
                "status", "ONLINE",
                "version", "0.0.1-SNAPSHOT",
                "documentation", "/swagger-ui.html",
                "healthCheck", "/api/v1/health"
        ));
    }

    @Operation(summary = "Verificação de Saúde", description = "Endpoint de health check do backend")
    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "backend-core",
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }

}
