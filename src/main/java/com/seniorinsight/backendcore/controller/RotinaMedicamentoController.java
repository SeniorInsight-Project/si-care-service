package com.seniorinsight.backendcore.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.seniorinsight.backendcore.dto.AtualizarRotinaRequestDTO;
import com.seniorinsight.backendcore.dto.ConfirmarIngestaoRequestDTO;
import com.seniorinsight.backendcore.dto.HistoricoMedicacaoResponseDTO;
import com.seniorinsight.backendcore.dto.RotinaMedicamentoRequestDTO;
import com.seniorinsight.backendcore.dto.RotinaMedicamentoResponseDTO;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.service.RotinaMedicamentoService;

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
 * Controller REST para o CRUD e gestão da Rotina de Medicamentos (HU26).
 * Fornece endpoints otimizados para React Native para agendamento, consulta da rotina diária
 * e confirmação de ingestão com verificação de tolerância clínica de 30 minutos.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Rotinas de Medicamentos", description = "Endpoints para agendamento de remédios, listagem diária e confirmação de ingestão (HU26)")
@SecurityRequirement(name = "bearerAuth")
public class RotinaMedicamentoController {

    private final RotinaMedicamentoService rotinaMedicamentoService;

    public RotinaMedicamentoController(RotinaMedicamentoService rotinaMedicamentoService) {
        this.rotinaMedicamentoService = rotinaMedicamentoService;
    }

    @Operation(
            summary = "Cria a agenda de um medicamento para o idoso (HU26)",
            description = "Cadastra uma nova rotina de medicamento para o idoso informado, definindo horário diário, " +
                    "frequência, dosagem e instruções de uso. Aceita medicamento novo ou ID de medicamento existente."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Rotina de medicamento criada com sucesso.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RotinaMedicamentoResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de requisição inválidos, horário ausente ou frequência em branco."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso proibido: usuário logado não possui perfil de Familiar ou Médico."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Idoso ou Medicamento informado não encontrado."
            )
    })
    @PostMapping("/idosos/{idosoId}/rotinas-medicamentos")
    public ResponseEntity<RotinaMedicamentoResponseDTO> criarRotina(
            @PathVariable UUID idosoId,
            @Valid @RequestBody RotinaMedicamentoRequestDTO request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        RotinaMedicamentoResponseDTO response = rotinaMedicamentoService.criarRotina(idosoId, request, usuarioAutenticado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Lista a rotina diária de medicamentos do idoso (HU26)",
            description = "Retorna a rotina diária de medicamentos ordenada cronologicamente por horário, " +
                    "com status em tempo real da ingestão do dia (PENDENTE, TOMADO, ATRASADO, IGNORADO), ideal para React Native."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rotina diária recuperada com sucesso.",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RotinaMedicamentoResponseDTO.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso proibido: usuário logado não possui perfil de Familiar ou Médico."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Idoso não encontrado com o ID informado."
            )
    })
    @GetMapping("/idosos/{idosoId}/rotinas-medicamentos")
    public ResponseEntity<List<RotinaMedicamentoResponseDTO>> listarRotinasDiarias(
            @PathVariable UUID idosoId,
            @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia,
            @RequestParam(value = "apenasAtivos", required = false, defaultValue = "true") Boolean apenasAtivos,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        List<RotinaMedicamentoResponseDTO> response = rotinaMedicamentoService.listarRotinasDiarias(
                idosoId, dataReferencia, apenasAtivos, usuarioAutenticado);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Confirma a ingestão de um medicamento na rotina (HU26)",
            description = "Salva o registro no histórico de medicação e valida automaticamente a janela de tolerância " +
                    "clínica de 30 minutos em relação ao horário agendado. Define o status como TOMADO ou ATRASADO, " +
                    "ou aceita marcação como IGNORADO caso a dose tenha sido pulada."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Ingestão confirmada e salva no histórico com sucesso.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = HistoricoMedicacaoResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requisição inválida ou formato de dados incorreto."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso proibido: usuário logado não possui perfil de Familiar ou Médico."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Rotina de medicamento não encontrada com o ID informado."
            )
    })
    @PostMapping("/rotinas-medicamentos/{rotinaId}/confirmar-ingestao")
    public ResponseEntity<HistoricoMedicacaoResponseDTO> confirmarIngestao(
            @PathVariable UUID rotinaId,
            @RequestBody(required = false) ConfirmarIngestaoRequestDTO request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        HistoricoMedicacaoResponseDTO response = rotinaMedicamentoService.confirmarIngestao(rotinaId, request, usuarioAutenticado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Obtém os detalhes de uma rotina de medicamento específica por ID",
            description = "Retorna os detalhes completos da rotina de medicamento indicada."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rotina recuperada com sucesso.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RotinaMedicamentoResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso proibido."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Rotina de medicamento não encontrada."
            )
    })
    @GetMapping("/rotinas-medicamentos/{rotinaId}")
    public ResponseEntity<RotinaMedicamentoResponseDTO> obterRotinaPorId(
            @PathVariable UUID rotinaId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        RotinaMedicamentoResponseDTO response = rotinaMedicamentoService.obterRotinaPorId(rotinaId, usuarioAutenticado);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Atualiza uma rotina de medicamento existente",
            description = "Permite alterar horário, dosagem, frequência, vigência e status ativo da rotina."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rotina atualizada com sucesso.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RotinaMedicamentoResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de atualização inválidos."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso proibido."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Rotina de medicamento não encontrada."
            )
    })
    @PutMapping("/rotinas-medicamentos/{rotinaId}")
    public ResponseEntity<RotinaMedicamentoResponseDTO> atualizarRotina(
            @PathVariable UUID rotinaId,
            @RequestBody AtualizarRotinaRequestDTO request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        RotinaMedicamentoResponseDTO response = rotinaMedicamentoService.atualizarRotina(rotinaId, request, usuarioAutenticado);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Exclui uma rotina de medicamento",
            description = "Remove a rotina de medicamento e seus históricos associados."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Rotina removida com sucesso."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso proibido."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Rotina de medicamento não encontrada."
            )
    })
    @DeleteMapping("/rotinas-medicamentos/{rotinaId}")
    public ResponseEntity<Void> removerRotina(
            @PathVariable UUID rotinaId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        rotinaMedicamentoService.removerRotina(rotinaId, usuarioAutenticado);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Lista o histórico de ingestões de uma rotina de medicamento",
            description = "Retorna todos os logs de ingestão registrados para a rotina selecionada em ordem decrescente."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Histórico recuperado com sucesso.",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = HistoricoMedicacaoResponseDTO.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso proibido."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Rotina de medicamento não encontrada."
            )
    })
    @GetMapping("/rotinas-medicamentos/{rotinaId}/historico")
    public ResponseEntity<List<HistoricoMedicacaoResponseDTO>> listarHistoricoRotina(
            @PathVariable UUID rotinaId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        List<HistoricoMedicacaoResponseDTO> response = rotinaMedicamentoService.listarHistoricoRotina(rotinaId, usuarioAutenticado);
        return ResponseEntity.ok(response);
    }
}
