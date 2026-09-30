package com.seniorinsight.backendcore.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seniorinsight.backendcore.dto.AlarmeEmergenciaResponseDTO;
import com.seniorinsight.backendcore.dto.AtualizarStatusAlarmeRequestDTO;
import com.seniorinsight.backendcore.dto.DispararSosRequestDTO;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.service.AlarmeEmergenciaService;

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
 * Controller REST para o Botão de Alarme / SOS de Emergência (HU35).
 * Expõe endpoints críticos para registro de socorro com SLA < 100ms (HTTP 202 Accepted)
 * e gestão dos estados de atendimento do alarme.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Alarmes de Emergência / SOS", description = "Endpoints para acionamento de socorro imediato e gestão do status de atendimento (HU35)")
@SecurityRequirement(name = "bearerAuth")
public class AlarmeEmergenciaController {

    private final AlarmeEmergenciaService alarmeEmergenciaService;

    public AlarmeEmergenciaController(AlarmeEmergenciaService alarmeEmergenciaService) {
        this.alarmeEmergenciaService = alarmeEmergenciaService;
    }

    @Operation(
            summary = "Dispara o botão de alarme / SOS de emergência (HU35)",
            description = "Registra o acionamento em tempo real do botão de socorro para o idoso e despacha o evento assíncrono " +
                    "para a rede de proteção. Responde imediatamente com HTTP 202 Accepted para cumprimento rigoroso de SLA (< 100ms)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Alarme aceito e registrado com sucesso. Notificações sendo processadas assincronamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlarmeEmergenciaResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados da requisição inválidos ou fora dos limites tolerados."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Idoso não encontrado na base de dados."
            )
    })
    @PostMapping("/idosos/{idosoId}/sos")
    public ResponseEntity<AlarmeEmergenciaResponseDTO> dispararSos(
            @PathVariable UUID idosoId,
            @Valid @RequestBody(required = false) DispararSosRequestDTO request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        AlarmeEmergenciaResponseDTO response = alarmeEmergenciaService.dispararAlarme(idosoId, request, usuarioAutenticado);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(
            summary = "Atualiza o status do alarme de emergência (HU35)",
            description = "Permite atualizar o status do atendimento do alarme (ex: EM_ATENDIMENTO, RESOLVIDO, CANCELADO) com notas e justificativas."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Status do alarme atualizado com sucesso.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlarmeEmergenciaResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Status inválido ou alarme já finalizado."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Alarme de emergência não encontrado."
            )
    })
    @PutMapping("/sos/{alarmeId}/status")
    public ResponseEntity<AlarmeEmergenciaResponseDTO> atualizarStatus(
            @PathVariable UUID alarmeId,
            @Valid @RequestBody AtualizarStatusAlarmeRequestDTO request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        AlarmeEmergenciaResponseDTO response = alarmeEmergenciaService.atualizarStatus(alarmeId, request, usuarioAutenticado);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Consulta os detalhes de um alarme de emergência (HU35)",
            description = "Retorna os detalhes completos do alarme, incluindo coordenadas, nível de bateria e status atual."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Alarme localizado com sucesso.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlarmeEmergenciaResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Alarme de emergência não encontrado."
            )
    })
    @GetMapping("/sos/{alarmeId}")
    public ResponseEntity<AlarmeEmergenciaResponseDTO> buscarPorId(@PathVariable UUID alarmeId) {
        AlarmeEmergenciaResponseDTO response = alarmeEmergenciaService.buscarPorId(alarmeId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Lista o histórico de alarmes de emergência de um idoso (HU35)",
            description = "Retorna a lista de alarmes de socorro disparados para o idoso especificado, ordenados do mais recente ao mais antigo."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Histórico retornado com sucesso.",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AlarmeEmergenciaResponseDTO.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Idoso não encontrado."
            )
    })
    @GetMapping("/idosos/{idosoId}/sos")
    public ResponseEntity<List<AlarmeEmergenciaResponseDTO>> listarPorIdoso(@PathVariable UUID idosoId) {
        List<AlarmeEmergenciaResponseDTO> response = alarmeEmergenciaService.listarPorIdoso(idosoId);
        return ResponseEntity.ok(response);
    }
}
