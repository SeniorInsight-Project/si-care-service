package com.seniorinsight.backendcore.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seniorinsight.backendcore.dto.ComorbidadeRequestDTO;
import com.seniorinsight.backendcore.dto.ComorbidadesResponseDTO;
import com.seniorinsight.backendcore.model.Usuario;
import com.seniorinsight.backendcore.service.ComorbidadeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controller REST para manipulação de comorbidades do perfil do idoso (HU15).
 * Gerencia a lista nativa JSONB de comorbidades da entidade Idoso.
 */
@RestController
@RequestMapping("/api/v1/idosos/{idosoId}/comorbidades")
@Tag(name = "Comorbidades do Idoso", description = "Endpoints para cadastro e remoção de comorbidades no perfil do idoso via JSONB (HU15)")
@SecurityRequirement(name = "bearerAuth")
public class ComorbidadeController {

    private final ComorbidadeService comorbidadeService;

    public ComorbidadeController(ComorbidadeService comorbidadeService) {
        this.comorbidadeService = comorbidadeService;
    }

    @Operation(
            summary = "Adiciona comorbidades ao perfil do idoso (HU15)",
            description = "Adiciona uma ou mais comorbidades à lista JSONB existente da entidade Idoso. " +
                    "Apenas usuários com perfil autorizado (Familiar ou Médico) podem realizar a alteração. " +
                    "Comorbidades duplicadas são desconsideradas automaticamente."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Comorbidade(s) adicionada(s) com sucesso. Retorna a lista atualizada.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ComorbidadesResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos, lista vazia ou nomes em branco."
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
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Todas as comorbidades informadas já estão cadastradas para este idoso."
            )
    })
    @PostMapping
    public ResponseEntity<ComorbidadesResponseDTO> adicionarComorbidades(
            @PathVariable UUID idosoId,
            @Valid @RequestBody ComorbidadeRequestDTO request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        ComorbidadesResponseDTO response = comorbidadeService.adicionarComorbidades(idosoId, request, usuarioAutenticado);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Remove uma comorbidade específica do perfil do idoso (HU15)",
            description = "Remove uma comorbidade específica da lista JSONB existente da entidade Idoso. " +
                    "Apenas usuários com perfil autorizado (Familiar ou Médico) podem realizar a alteração."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Comorbidade removida com sucesso. Sem conteúdo de retorno."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nome da comorbidade informado é inválido ou vazio."
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
                    description = "Idoso não encontrado ou a comorbidade informada não consta no perfil do idoso."
            )
    })
    @DeleteMapping("/{nomeComorbidade}")
    public ResponseEntity<Void> removerComorbidade(
            @PathVariable UUID idosoId,
            @PathVariable String nomeComorbidade,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        comorbidadeService.removerComorbidade(idosoId, nomeComorbidade, usuarioAutenticado);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Lista as comorbidades do perfil do idoso",
            description = "Recupera a lista atual de comorbidades cadastradas no perfil do idoso. " +
                    "Apenas perfis autorizados (Familiar ou Médico) possuem acesso."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de comorbidades recuperada com sucesso.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ComorbidadesResponseDTO.class))
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
    @GetMapping
    public ResponseEntity<ComorbidadesResponseDTO> listarComorbidades(
            @PathVariable UUID idosoId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        ComorbidadesResponseDTO response = comorbidadeService.listarComorbidades(idosoId, usuarioAutenticado);
        return ResponseEntity.ok(response);
    }
}
