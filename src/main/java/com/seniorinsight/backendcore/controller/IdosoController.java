package com.seniorinsight.backendcore.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seniorinsight.backendcore.dto.IdosoRequestDTO;
import com.seniorinsight.backendcore.dto.IdosoResponseDTO;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import com.seniorinsight.backendcore.service.IdosoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Cadastro e edição do idoso (HU-008 e HU-009), conforme {@code si-contracts/openapi/care-v1.yaml}. */
@RestController
@RequestMapping("/api/v1/idosos")
@Tag(name = "Idosos", description = "Cadastro do idoso monitorado")
@SecurityRequirement(name = "bearerAuth")
public class IdosoController {

    private final IdosoService idosoService;

    public IdosoController(IdosoService idosoService) {
        this.idosoService = idosoService;
    }

    @Operation(operationId = "listarIdosos", summary = "Lista os idosos da rede do usuário autenticado")
    @GetMapping
    public List<IdosoResponseDTO> listar(@Parameter(hidden = true) UsuarioAutenticado usuario) {
        return idosoService.listar(usuario);
    }

    @Operation(operationId = "cadastrarIdoso", summary = "Cadastra um idoso",
            description = "O usuário autenticado vira o Familiar Administrador do idoso. "
                    + "Idade abaixo de 49 anos gera um aviso na resposta, sem bloquear o cadastro.")
    @PostMapping
    public ResponseEntity<IdosoResponseDTO> cadastrar(
            @Parameter(hidden = true) UsuarioAutenticado usuario,
            @Valid @RequestBody IdosoRequestDTO request) {
        IdosoResponseDTO criado = idosoService.cadastrar(usuario, request);
        return ResponseEntity.created(URI.create("/api/v1/idosos/" + criado.id())).body(criado);
    }

    @Operation(operationId = "buscarIdoso", summary = "Busca o cadastro de um idoso")
    @GetMapping("/{idosoId}")
    public IdosoResponseDTO buscar(
            @Parameter(hidden = true) UsuarioAutenticado usuario,
            @PathVariable UUID idosoId) {
        return idosoService.buscar(usuario, idosoId);
    }

    @Operation(operationId = "atualizarIdoso", summary = "Atualiza o cadastro de um idoso",
            description = "Só o Familiar Administrador. Grava auditoria com o antes e o depois.")
    @PutMapping("/{idosoId}")
    public IdosoResponseDTO atualizar(
            @Parameter(hidden = true) UsuarioAutenticado usuario,
            @PathVariable UUID idosoId,
            @Valid @RequestBody IdosoRequestDTO request) {
        return idosoService.atualizar(usuario, idosoId, request);
    }
}
