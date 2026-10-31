package com.seniorinsight.backendcore.security;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.seniorinsight.backendcore.model.ProvedorSso;
import com.seniorinsight.backendcore.model.StatusUsuario;
import com.seniorinsight.backendcore.model.Usuario;

class JwtServiceTest {

    private static final String SEGREDO_VALIDO = "segredo-de-teste-com-mais-de-32-caracteres";

    @Test
    @DisplayName("Não sobe sem JWT_SECRET: não existe mais segredo padrão no código")
    void recusaSegredoVazio() {
        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> new JwtService("", 3600));
        assertTrue(erro.getMessage().contains("JWT_SECRET"));
    }

    @Test
    @DisplayName("Recusa JWT_SECRET com menos de 32 caracteres")
    void recusaSegredoCurto() {
        assertThrows(IllegalStateException.class, () -> new JwtService("curto-demais", 3600));
    }

    @Test
    @DisplayName("Gera e valida o token com o segredo informado")
    void geraEValidaToken() {
        JwtService jwtService = new JwtService(SEGREDO_VALIDO, 3600);
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("familiar@seniorinsight.com")
                .status(StatusUsuario.ATIVO)
                .provedorSso(ProvedorSso.LOCAL)
                .build();

        String token = jwtService.generateToken(usuario);

        assertEquals("familiar@seniorinsight.com", jwtService.validateAndGetSubject(token));
    }
}
