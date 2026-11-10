package com.seniorinsight.backendcore.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seniorinsight.backendcore.dto.ContatoEmergenciaRequestDTO;
import com.seniorinsight.backendcore.dto.ContatoEmergenciaResponseDTO;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import com.seniorinsight.backendcore.service.ContatoEmergenciaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Contatos de emergência do idoso (HU-011), conforme {@code si-contracts/openapi/care-v1.yaml}. */
@RestController
@RequestMapping("/api/v1/idosos/{idosoId}/contatos-emergencia")
@Tag(name = "Contatos de emergência", description = "Quem acionar em uma emergência com o idoso")
@SecurityRequirement(name = "bearerAuth")
public class ContatoEmergenciaController {

    private final ContatoEmergenciaService service;

    public ContatoEmergenciaController(ContatoEmergenciaService service) {
        this.service = service;
    }

    @Operation(operationId = "listarContatosEmergencia",
            summary = "Lista os contatos de emergência do idoso, em ordem de prioridade")
    @GetMapping
    public List<ContatoEmergenciaResponseDTO> listar(
            @Parameter(hidden = true) UsuarioAutenticado usuario,
            @PathVariable UUID idosoId) {
        return service.listar(usuario, idosoId);
    }

    @Operation(operationId = "adicionarContatoEmergencia", summary = "Adiciona um contato de emergência",
            description = "Só o Familiar Administrador. Máximo de 3 contatos por idoso (409 ao exceder).")
    @PostMapping
    public ResponseEntity<ContatoEmergenciaResponseDTO> adicionar(
            @Parameter(hidden = true) UsuarioAutenticado usuario,
            @PathVariable UUID idosoId,
            @Valid @RequestBody ContatoEmergenciaRequestDTO request) {
        ContatoEmergenciaResponseDTO criado = service.adicionar(usuario, idosoId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/idosos/" + idosoId + "/contatos-emergencia/" + criado.id()))
                .body(criado);
    }

    @Operation(operationId = "atualizarContatoEmergencia", summary = "Atualiza um contato de emergência",
            description = "Só o Familiar Administrador.")
    @PutMapping("/{contatoId}")
    public ContatoEmergenciaResponseDTO atualizar(
            @Parameter(hidden = true) UsuarioAutenticado usuario,
            @PathVariable UUID idosoId,
            @PathVariable UUID contatoId,
            @Valid @RequestBody ContatoEmergenciaRequestDTO request) {
        return service.atualizar(usuario, idosoId, contatoId, request);
    }

    @Operation(operationId = "removerContatoEmergencia", summary = "Remove um contato de emergência",
            description = "Só o Familiar Administrador.")
    @DeleteMapping("/{contatoId}")
    public ResponseEntity<Void> remover(
            @Parameter(hidden = true) UsuarioAutenticado usuario,
            @PathVariable UUID idosoId,
            @PathVariable UUID contatoId) {
        service.remover(usuario, idosoId, contatoId);
        return ResponseEntity.noContent().build();
    }
}
