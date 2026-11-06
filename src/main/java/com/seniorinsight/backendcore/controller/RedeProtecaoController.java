package com.seniorinsight.backendcore.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.seniorinsight.backendcore.dto.AceiteConviteResponseDTO;
import com.seniorinsight.backendcore.dto.ConviteRedeRequestDTO;
import com.seniorinsight.backendcore.dto.ConviteRedeResponseDTO;
import com.seniorinsight.backendcore.dto.MembroRedeResponseDTO;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import com.seniorinsight.backendcore.service.RedeProtecaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controller REST para Gestão e Convites da Rede de Proteção do Idoso (HU16).
 * Fornece endpoints para geração de convites temporais, aceite de convites e visualização dos membros da rede.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Rede de Proteção", description = "Endpoints para gestão de convites temporais e membros da rede de proteção do idoso (HU16)")
@SecurityRequirement(name = "bearerAuth")
public class RedeProtecaoController {

    private final RedeProtecaoService redeProtecaoService;

    public RedeProtecaoController(RedeProtecaoService redeProtecaoService) {
        this.redeProtecaoService = redeProtecaoService;
    }

    @Operation(
            summary = "Gera um convite para a Rede de Proteção do Idoso (HU16)",
            description = "Gera um token UUID temporal válido por 48 horas para convidar um novo Familiar para a " +
                    "Rede de Proteção do Idoso indicado e simula o disparo do respectivo e-mail com as instruções de aceite."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Convite gerado com sucesso e simulação de e-mail disparada.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConviteRedeResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de requisição inválidos ou e-mail com formato incorreto."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso proibido: usuário logado não possui permissão para emitir convites para este idoso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Idoso não encontrado com o ID informado."
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "O familiar convidado já possui vínculo ativo com este idoso."
            )
    })
    @PostMapping("/idosos/{idosoId}/convites")
    public ResponseEntity<ConviteRedeResponseDTO> gerarConvite(
            @PathVariable UUID idosoId,
            @Valid @RequestBody ConviteRedeRequestDTO request,
            @Parameter(hidden = true) UsuarioAutenticado usuarioAutenticado) {
        ConviteRedeResponseDTO response = redeProtecaoService.gerarConvite(idosoId, request, usuarioAutenticado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Aceita um convite para a Rede de Proteção (HU16)",
            description = "Valida o token temporal de 48h recebido via parâmetro e cria o registro na tabela " +
                    "`vinculos_rede`, associando o familiar logado ao respectivo idoso monitorado."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Convite aceito com sucesso e vínculo estabelecido na Rede de Proteção.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AceiteConviteResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token inválido, inexistente ou convite já processado/cancelado."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso proibido: usuário logado não possui perfil de Familiar completo."
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "O familiar logado já possui vínculo ativo com o idoso."
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "O prazo de validade temporal de 48 horas do convite expirou."
            )
    })
    @PostMapping("/convites/aceitar")
    public ResponseEntity<AceiteConviteResponseDTO> aceitarConvite(
            @RequestParam("token") String token,
            @Parameter(hidden = true) UsuarioAutenticado usuarioAutenticado) {
        AceiteConviteResponseDTO response = redeProtecaoService.aceitarConvite(token, usuarioAutenticado);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Lista todos os membros da Rede de Proteção do Idoso (HU16)",
            description = "Recupera todos os membros vinculados ao idoso na tabela `vinculos_rede`, convertendo " +
                    "os dados internos em um DTO amigável contendo nome, e-mail, telefone, parentesco e papel de acesso."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de membros vinculados recuperada com sucesso.",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MembroRedeResponseDTO.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado."
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso proibido: usuário logado não possui perfil autorizado (Familiar ou Médico)."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Idoso não encontrado com o ID informado."
            )
    })
    @GetMapping("/idosos/{idosoId}/rede")
    public ResponseEntity<List<MembroRedeResponseDTO>> listarMembrosRede(
            @PathVariable UUID idosoId,
            @Parameter(hidden = true) UsuarioAutenticado usuarioAutenticado) {
        List<MembroRedeResponseDTO> response = redeProtecaoService.listarMembrosRede(idosoId, usuarioAutenticado);
        return ResponseEntity.ok(response);
    }
}
