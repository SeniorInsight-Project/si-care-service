package com.seniorinsight.backendcore.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seniorinsight.backendcore.dto.IdosoResponseDTO;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import com.seniorinsight.backendcore.service.IdosoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Idosos de um familiar, conforme {@code listarIdososDoFamiliar} do {@code si-contracts/openapi/care-v1.yaml}. */
@RestController
@RequestMapping("/api/v1/familiares/{familiarId}/idosos")
@Tag(name = "Idosos", description = "Cadastro do idoso monitorado")
@SecurityRequirement(name = "bearerAuth")
public class FamiliarController {

    private final IdosoService idosoService;

    public FamiliarController(IdosoService idosoService) {
        this.idosoService = idosoService;
    }

    @Operation(operationId = "listarIdososDoFamiliar",
            summary = "Lista os idosos aos quais um familiar está vinculado",
            description = "Só o próprio familiar ou um Familiar Administrador do idoso consulta; os demais recebem 403.")
    @GetMapping
    public List<IdosoResponseDTO> listar(
            @Parameter(hidden = true) UsuarioAutenticado usuario,
            @PathVariable UUID familiarId) {
        return idosoService.listarDoFamiliar(usuario, familiarId);
    }
}
