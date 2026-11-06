package com.seniorinsight.backendcore.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Usuário autenticado pelo Keycloak, montado a partir das claims do access token.
 *
 * @param keycloakSub claim {@code sub}: identifica o usuário no Keycloak e liga ao perfil (Familiar/Médico)
 * @param email       claim {@code email} (pode ser nula)
 * @param nome        claim {@code name} (pode ser nula)
 * @param papeis      roles do realm ({@code realm_access.roles}), sem o prefixo {@code ROLE_}
 */
public record UsuarioAutenticado(String keycloakSub, String email, String nome, List<String> papeis) {

    public UsuarioAutenticado {
        papeis = papeis == null ? List.of() : List.copyOf(papeis);
    }

    public static UsuarioAutenticado de(Jwt jwt) {
        return new UsuarioAutenticado(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"),
                papeisDoRealm(jwt));
    }

    public boolean temPapel(String papel) {
        return papeis.contains(papel);
    }

    private static List<String> papeisDoRealm(Jwt jwt) {
        if (!(jwt.getClaims().get("realm_access") instanceof Map<?, ?> realmAccess)
                || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }
}
