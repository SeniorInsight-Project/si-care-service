package com.seniorinsight.backendcore.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import com.seniorinsight.backendcore.config.SecurityConfig;
import com.seniorinsight.backendcore.dto.AvisoExclusaoRedeResponseDTO;
import com.seniorinsight.backendcore.exception.AvisoExclusaoNaoEncontradoException;
import com.seniorinsight.backendcore.service.AvisoExclusaoRedeService;

/** GET /internal/exclusoes/{protocolo}/rede: só o serviço (notification-service) com a role de serviço consulta. */
@WebMvcTest(controllers = ExclusaoInternaController.class)
@Import(SecurityConfig.class)
class ExclusaoInternaControllerTest {

    private static final UUID PROTOCOLO = UUID.randomUUID();
    private static final String URL = "/internal/exclusoes/{protocolo}/rede";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AvisoExclusaoRedeService avisoService;

    @Test
    void semTokenResponde401() throws Exception {
        mockMvc.perform(get(URL, PROTOCOLO)).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeUsuarioSemARoleDeServicoResponde403() throws Exception {
        mockMvc.perform(get(URL, PROTOCOLO).with(token("familiar"))).andExpect(status().isForbidden());
    }

    @Test
    void servicoDevolveOPrimeiroNomeEOsMembros() throws Exception {
        when(avisoService.consultar(PROTOCOLO)).thenReturn(new AvisoExclusaoRedeResponseDTO(PROTOCOLO, "Dona",
                List.of(new AvisoExclusaoRedeResponseDTO.Membro("sub-ana", "Ana Lima", "ana@exemplo.com"))));

        mockMvc.perform(get(URL, PROTOCOLO).with(token("servico-interno")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo").value(PROTOCOLO.toString()))
                .andExpect(jsonPath("$.primeiroNomeIdoso").value("Dona"))
                .andExpect(jsonPath("$.membros[0].usuarioId").value("sub-ana"))
                .andExpect(jsonPath("$.membros[0].nomeCompleto").value("Ana Lima"))
                .andExpect(jsonPath("$.membros[0].email").value("ana@exemplo.com"));
    }

    @Test
    void protocoloAindaNaoProcessadoResponde404() throws Exception {
        when(avisoService.consultar(PROTOCOLO))
                .thenThrow(new AvisoExclusaoNaoEncontradoException("Exclusão ainda não processada"));

        mockMvc.perform(get(URL, PROTOCOLO).with(token("servico-interno"))).andExpect(status().isNotFound());
    }

    @Test
    void protocoloQueNaoEUuidResponde400() throws Exception {
        mockMvc.perform(get(URL, "nao-e-uuid").with(token("servico-interno"))).andExpect(status().isBadRequest());
    }

    private static JwtRequestPostProcessor token(String role) {
        return jwt().jwt(jwt -> jwt.subject("conta")).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
