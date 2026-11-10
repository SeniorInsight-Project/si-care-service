package com.seniorinsight.backendcore.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import com.seniorinsight.backendcore.config.SecurityConfig;
import com.seniorinsight.backendcore.dto.IdosoResponseDTO;
import com.seniorinsight.backendcore.exception.FamiliarNaoEncontradoException;
import com.seniorinsight.backendcore.exception.PerfilNaoAutorizadoException;
import com.seniorinsight.backendcore.service.IdosoService;

@WebMvcTest(controllers = FamiliarController.class)
@Import(SecurityConfig.class)
class FamiliarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdosoService idosoService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private final UUID familiarId = UUID.randomUUID();

    @Test
    @DisplayName("sem token responde 401")
    void semToken() throws Exception {
        mockMvc.perform(get("/api/v1/familiares/{id}/idosos", familiarId)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("responde 200 com a lista de idosos do familiar")
    void lista() throws Exception {
        UUID idosoId = UUID.randomUUID();
        when(idosoService.listarDoFamiliar(any(), eq(familiarId))).thenReturn(List.of(new IdosoResponseDTO(
                idosoId, "Maria", LocalDate.of(1945, 3, 12), "F", null, null, null, null, null, null, null, null,
                null, null, null, null, List.of(), Map.of(), Instant.now(), Instant.now(), List.of())));

        mockMvc.perform(get("/api/v1/familiares/{id}/idosos", familiarId).with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(idosoId.toString()));
    }

    @Test
    @DisplayName("quem não é o familiar nem Administrador recebe 403")
    void semPermissao() throws Exception {
        when(idosoService.listarDoFamiliar(any(), eq(familiarId)))
                .thenThrow(new PerfilNaoAutorizadoException("Só o próprio familiar"));

        mockMvc.perform(get("/api/v1/familiares/{id}/idosos", familiarId).with(token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("familiar inexistente responde 404 e id que não é UUID responde 400")
    void inexistenteEInvalido() throws Exception {
        when(idosoService.listarDoFamiliar(any(), eq(familiarId)))
                .thenThrow(new FamiliarNaoEncontradoException(familiarId));

        mockMvc.perform(get("/api/v1/familiares/{id}/idosos", familiarId).with(token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/familiares/{id}/idosos", "abc").with(token()))
                .andExpect(status().isBadRequest());
    }

    private static JwtRequestPostProcessor token() {
        return jwt().jwt(j -> j.subject("sub-familiar")
                .claim("realm_access", Map.of("roles", List.of("familiar"))));
    }
}
