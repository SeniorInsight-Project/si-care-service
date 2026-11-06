package com.seniorinsight.backendcore.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class ConversorRolesKeycloakTest {

    private final ConversorRolesKeycloak conversor = new ConversorRolesKeycloak();

    @Test
    void mapeiaRolesDeRealmParaAuthoritiesRole() {
        Jwt jwt = token().claim("realm_access", Map.of("roles", List.of("familiar", "medico"))).build();

        assertThat(conversor.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_familiar", "ROLE_medico");
    }

    @Test
    void tokenSemRealmAccessNaoTemRoles() {
        assertThat(conversor.convert(token().build())).isEmpty();
        assertThat(conversor.convert(token().claim("realm_access", Map.of()).build())).isEmpty();
    }

    @Test
    void usuarioAutenticadoVemDasClaimsDoToken() {
        Jwt jwt = token()
                .claim("email", "ana@seniorinsight.com")
                .claim("name", "Ana Souza")
                .claim("realm_access", Map.of("roles", List.of("familiar")))
                .build();

        UsuarioAutenticado usuario = UsuarioAutenticado.de(jwt);

        assertThat(usuario.keycloakSub()).isEqualTo("sub-123");
        assertThat(usuario.email()).isEqualTo("ana@seniorinsight.com");
        assertThat(usuario.nome()).isEqualTo("Ana Souza");
        assertThat(usuario.papeis()).containsExactly("familiar");
        assertThat(usuario.temPapel("familiar")).isTrue();
    }

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("sub-123");
    }
}
