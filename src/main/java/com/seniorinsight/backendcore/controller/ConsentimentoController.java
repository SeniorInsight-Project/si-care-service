package com.seniorinsight.backendcore.controller;

import java.util.UUID;

import com.seniorinsight.backendcore.dto.ConsentimentosDTO;
import com.seniorinsight.backendcore.service.ConsentimentoService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/idosos/{idosoId}/consentimentos")
class ConsentimentoController {

    private final ConsentimentoService consentimentos;

    ConsentimentoController(ConsentimentoService consentimentos) {
        this.consentimentos = consentimentos;
    }

    @GetMapping
    ConsentimentosDTO consultar(@PathVariable UUID idosoId, @AuthenticationPrincipal Jwt jwt) {
        return consentimentos.consultar(idosoId, jwt);
    }

    @PutMapping
    ConsentimentosDTO salvar(@PathVariable UUID idosoId, @AuthenticationPrincipal Jwt jwt,
                              @Valid @RequestBody ConsentimentosDTO consentimentosNovos) {
        return consentimentos.salvar(idosoId, jwt, consentimentosNovos);
    }
}
