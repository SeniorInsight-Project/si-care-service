package com.seniorinsight.backendcore.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.seniorinsight.backendcore.dto.DeteccaoQuedaRequestDTO;
import com.seniorinsight.backendcore.dto.FrequenciaCardiacaRequestDTO;
import com.seniorinsight.backendcore.dto.PassosRequestDTO;
import com.seniorinsight.backendcore.dto.TelemetriaWearableBatchRequestDTO;
import com.seniorinsight.backendcore.dto.TelemetriaWearableRequestDTO;
import com.seniorinsight.backendcore.dto.TelemetriaWearableResponseDTO;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.service.WearableIntegrationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controller REST para a camada de ingestão de telemetria de dispositivos wearable (smartwatches).
 * Recebe payloads emitidos pelo aplicativo mobile em React Native (Expo) seguindo o padrão OpenWearables
 * para frequência cardíaca, contagem de passos, detecção de queda e estado da bateria.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Telemetria de Wearables (OpenWearables)", description = "Endpoints de ingestão de séries temporais IoT e alertas críticos de relógios inteligentes")
@SecurityRequirement(name = "bearerAuth")
public class TelemetriaWearableController {

    private final WearableIntegrationService wearableIntegrationService;

    public TelemetriaWearableController(WearableIntegrationService wearableIntegrationService) {
        this.wearableIntegrationService = wearableIntegrationService;
    }

    @Operation(
            summary = "Ingestão consolidada de telemetria de wearable (OpenWearables)",
            description = "Recebe e persiste uma leitura completa de biometria e sensores (BPM, passos, queda, bateria) para o idoso especificado."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Telemetria registrada e normalizada com sucesso.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TelemetriaWearableResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "202",
                    description = "Queda detectada no payload; alarme de emergência SOS acionado com urgência.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TelemetriaWearableResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload de telemetria inválido ou fora dos limites clínicos."),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado."),
            @ApiResponse(responseCode = "404", description = "Idoso não encontrado na base de dados.")
    })
    @PostMapping({"/idosos/{idosoId}/telemetria-wearable", "/telemetria-wearable/ingestao"})
    public ResponseEntity<TelemetriaWearableResponseDTO> receberTelemetria(
            @PathVariable(required = false) UUID idosoId,
            @Valid @RequestBody TelemetriaWearableRequestDTO request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        UUID finalId = idosoId != null ? idosoId : (request != null ? request.idosoId() : null);
        TelemetriaWearableResponseDTO response = wearableIntegrationService.processarTelemetria(finalId, request);

        if ("ALERTA_QUEDA_DISPARADO".equals(response.statusProcessamento())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Ingestão em lote de telemetrias (Batch Sincronização Offline)",
            description = "Permite ao aplicativo móvel sincronizar múltiplas leituras em lote acumuladas durante períodos sem conexão à internet."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Lote de telemetrias processado e persistido com sucesso.",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TelemetriaWearableResponseDTO.class)))
            ),
            @ApiResponse(responseCode = "400", description = "Lote vazio ou dados inválidos."),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado."),
            @ApiResponse(responseCode = "404", description = "Idoso não encontrado.")
    })
    @PostMapping({"/idosos/{idosoId}/telemetria-wearable/lote", "/telemetria-wearable/lote"})
    public ResponseEntity<List<TelemetriaWearableResponseDTO>> receberTelemetriaLote(
            @PathVariable(required = false) UUID idosoId,
            @Valid @RequestBody TelemetriaWearableBatchRequestDTO batchRequest,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        List<TelemetriaWearableResponseDTO> response = wearableIntegrationService.processarTelemetriaLote(idosoId, batchRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Ingestão rápida de frequência cardíaca",
            description = "Endpoint específico para streaming ou envio pontual de leituras do sensor óptico PPG (BPM e HRV)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Leitura de batimentos cardíacos registrada com sucesso.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TelemetriaWearableResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Frequência cardíaca fora da faixa plausível."),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado."),
            @ApiResponse(responseCode = "404", description = "Idoso não encontrado.")
    })
    @PostMapping({"/idosos/{idosoId}/telemetria-wearable/frequencia-cardiaca", "/telemetria-wearable/frequencia-cardiaca"})
    public ResponseEntity<TelemetriaWearableResponseDTO> receberFrequenciaCardiaca(
            @PathVariable(required = false) UUID idosoId,
            @Valid @RequestBody FrequenciaCardiacaRequestDTO request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        UUID finalId = idosoId != null ? idosoId : (request != null ? request.idosoId() : null);
        TelemetriaWearableResponseDTO response = wearableIntegrationService.processarFrequenciaCardiaca(finalId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Ingestão de passos e locomoção diária",
            description = "Endpoint para envio de contagem de passos, distância percorrida e calorias queimadas."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Leitura de passos registrada com sucesso.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TelemetriaWearableResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Dados de passos inválidos."),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado."),
            @ApiResponse(responseCode = "404", description = "Idoso não encontrado.")
    })
    @PostMapping({"/idosos/{idosoId}/telemetria-wearable/passos", "/telemetria-wearable/passos"})
    public ResponseEntity<TelemetriaWearableResponseDTO> receberPassos(
            @PathVariable(required = false) UUID idosoId,
            @Valid @RequestBody PassosRequestDTO request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        UUID finalId = idosoId != null ? idosoId : (request != null ? request.idosoId() : null);
        TelemetriaWearableResponseDTO response = wearableIntegrationService.processarPassos(finalId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Alerta crítico de detecção de queda em wearable",
            description = "Registra um evento de queda confirmado pelos acelerômetros do relógio e dispara imediatamente um SOS para a rede de proteção."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Queda aceita e registrada. Alarme SOS e notificações despachadas assincronamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TelemetriaWearableResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Dados da detecção de queda inválidos."),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado."),
            @ApiResponse(responseCode = "404", description = "Idoso não encontrado.")
    })
    @PostMapping({"/idosos/{idosoId}/telemetria-wearable/deteccao-queda", "/telemetria-wearable/deteccao-queda"})
    public ResponseEntity<TelemetriaWearableResponseDTO> receberDeteccaoQueda(
            @PathVariable(required = false) UUID idosoId,
            @Valid @RequestBody DeteccaoQuedaRequestDTO request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        UUID finalId = idosoId != null ? idosoId : (request != null ? request.idosoId() : null);
        TelemetriaWearableResponseDTO response = wearableIntegrationService.processarDeteccaoQueda(finalId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(
            summary = "Consulta o histórico recente de telemetrias do idoso",
            description = "Retorna a série temporal ordenada por data de coleta decrescente para apresentação gráfica no app React Native."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Histórico de telemetria retornado com sucesso.",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TelemetriaWearableResponseDTO.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado."),
            @ApiResponse(responseCode = "404", description = "Idoso não encontrado.")
    })
    @GetMapping("/idosos/{idosoId}/telemetria-wearable/ultimas")
    public ResponseEntity<List<TelemetriaWearableResponseDTO>> buscarUltimasTelemetrias(
            @PathVariable UUID idosoId,
            @RequestParam(defaultValue = "30") int limite,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        List<TelemetriaWearableResponseDTO> leituras = wearableIntegrationService.buscarUltimasTelemetrias(idosoId, limite);
        return ResponseEntity.ok(leituras);
    }

    @Operation(
            summary = "Consulta a última leitura de telemetria do idoso",
            description = "Retorna os valores biológicos mais recentes capturados pelo smartwatch do idoso."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Última leitura de telemetria retornada com sucesso.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TelemetriaWearableResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido ou expirado."),
            @ApiResponse(responseCode = "404", description = "Idoso não encontrado.")
    })
    @GetMapping("/idosos/{idosoId}/telemetria-wearable/recente")
    public ResponseEntity<TelemetriaWearableResponseDTO> buscarUltimaLeitura(
            @PathVariable UUID idosoId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        TelemetriaWearableResponseDTO ultima = wearableIntegrationService.buscarUltimaLeitura(idosoId);
        if (ultima == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ultima);
    }
}
