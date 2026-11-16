package com.seniorinsight.backendcore.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seniorinsight.backendcore.dto.AvisoExclusaoRedeResponseDTO;
import com.seniorinsight.backendcore.service.AvisoExclusaoRedeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Rota interna, só para outros serviços (token de serviço): o aviso à rede de uma exclusão de dados (HU-022). */
@RestController
@RequestMapping("/internal")
@Tag(name = "Interno", description = "Rotas para outros serviços, com a role de serviço")
@SecurityRequirement(name = "bearerAuth")
public class ExclusaoInternaController {

    private final AvisoExclusaoRedeService avisoService;

    public ExclusaoInternaController(AvisoExclusaoRedeService avisoService) {
        this.avisoService = avisoService;
    }

    @GetMapping("/exclusoes/{protocolo}/rede")
    @Operation(summary = "Quem da rede do idoso deve ser avisado da exclusão dos dados dele",
            description = "Cópia gravada antes da exclusão, disponível por um prazo curto. 404 enquanto o protocolo "
                    + "não foi processado, e para pedidos de exclusão de familiar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aviso encontrado; membros vazio se não havia ninguém a avisar"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Token sem a role de serviço"),
            @ApiResponse(responseCode = "404", description = "Protocolo ainda não processado")
    })
    public AvisoExclusaoRedeResponseDTO consultarAvisoDaRede(@PathVariable UUID protocolo) {
        return avisoService.consultar(protocolo);
    }
}
