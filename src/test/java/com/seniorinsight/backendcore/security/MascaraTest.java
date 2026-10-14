package com.seniorinsight.backendcore.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MascaraTest {

    @Test
    @DisplayName("Mantém só a primeira letra e o domínio do e-mail")
    void mascaraEmail() {
        assertEquals("m***@gmail.com", Mascara.email("maria.silva@gmail.com"));
    }

    @Test
    @DisplayName("Trata e-mail vazio, nulo ou sem arroba sem expor o valor")
    void emailInvalido() {
        assertEquals("(vazio)", Mascara.email(null));
        assertEquals("(vazio)", Mascara.email(" "));
        assertEquals("***", Mascara.email("sem-arroba"));
    }
}
