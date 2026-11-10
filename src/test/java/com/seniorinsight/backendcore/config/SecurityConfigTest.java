package com.seniorinsight.backendcore.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import com.seniorinsight.backendcore.controller.ComorbidadeController;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;
import com.seniorinsight.backendcore.service.AcessoIdosoService;
import com.seniorinsight.backendcore.service.ComorbidadeService;

/**
 * Resource server do Keycloak de ponta a ponta na camada web: o filtro de segurança, o
 * {@link com.seniorinsight.backendcore.security.UsuarioAutenticado} injetado no controller e a checagem do vínculo pelo sub.
 */
@WebMvcTest(controllers = ComorbidadeController.class)
@Import({SecurityConfig.class, ComorbidadeService.class, AcessoIdosoService.class})
class SecurityConfigTest {

    private static final String SUB_FAMILIAR = "f3c1a9e2-6b7d-4c1e-9a0b-2d5e8f7a1c34";
    private static final String URL = "/api/v1/idosos/{idosoId}/comorbidades";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private IdosoRepository idosoRepository;

    @MockBean
    private VinculoRedeRepository vinculoRedeRepository;

    @Test
    @DisplayName("Sem token responde 401 com o corpo JSON padrão")
    void semTokenResponde401() throws Exception {
        mockMvc.perform(get(URL, UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Não Autorizado"));
    }

    @Test
    @DisplayName("Token recusado pelo decoder (ex.: outro emissor) responde 401")
    void tokenDeOutroEmissorResponde401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("The iss claim is not valid"));

        mockMvc.perform(get(URL, UUID.randomUUID()).header("Authorization", "Bearer token-de-outro-realm"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Token válido sem vínculo com o idoso responde 403")
    void tokenValidoSemVinculoResponde403() throws Exception {
        UUID idosoId = UUID.randomUUID();
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(Idoso.builder().id(idosoId).build()));

        mockMvc.perform(get(URL, idosoId).with(tokenFamiliar()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Você não faz parte da rede de proteção deste idoso."));
    }

    @Test
    @DisplayName("Token de familiar com vínculo com o idoso chega ao endpoint e responde 200")
    void tokenDeFamiliarComPerfilResponde200() throws Exception {
        UUID idosoId = UUID.randomUUID();
        Idoso idoso = Idoso.builder()
                .id(idosoId)
                .nomeCompleto("Dona Nair")
                .comorbidades(new ArrayList<>(List.of("Hipertensão")))
                .build();
        when(vinculoRedeRepository.findByFamiliar_KeycloakSubAndIdoso_Id(SUB_FAMILIAR, idosoId))
                .thenReturn(Optional.of(VinculoRede.builder().papelAcesso("FAMILIAR").build()));
        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(idoso));

        mockMvc.perform(get(URL, idosoId).with(tokenFamiliar()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comorbidades[0]").value("Hipertensão"));
    }

    @Test
    @DisplayName("Autenticação usa o sub como nome e junta scopes e roles do realm")
    void autenticacaoUsaOSubComoNomeEJuntaScopesERoles() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(SUB_FAMILIAR)
                .claim("scope", "openid profile")
                .claim("realm_access", Map.of("roles", List.of("familiar")))
                .build();

        AbstractAuthenticationToken autenticacao = SecurityConfig.conversorAutenticacao().convert(jwt);

        assertThat(autenticacao.getName()).isEqualTo(SUB_FAMILIAR);
        assertThat(autenticacao.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("SCOPE_openid", "SCOPE_profile", "ROLE_familiar");
    }

    private static JwtRequestPostProcessor tokenFamiliar() {
        return jwt().jwt(j -> j.subject(SUB_FAMILIAR)
                .claim("email", "familiar@seniorinsight.com")
                .claim("realm_access", Map.of("roles", List.of("familiar"))));
    }
}
