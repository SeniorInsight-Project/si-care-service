package com.seniorinsight.backendcore.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelefonesTest {

    @ParameterizedTest
    @ValueSource(strings = {"(11) 98765-4321", "11987654321", "+55 11 98765-4321", "+5511987654321", " 11 98765 4321 "})
    void celularBrasileiroViraE164(String entrada) {
        assertThat(Telefones.paraE164(entrada)).contains("+5511987654321");
    }

    @Test
    void fixoBrasileiroEValido() {
        assertThat(Telefones.paraE164("(11) 3456-7890")).contains("+551134567890");
    }

    @Test
    void numeroInternacionalComCodigoDePaisEValido() {
        assertThat(Telefones.paraE164("+351 912 345 678")).contains("+351912345678");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "abc", "(11) 1234-567", "00000000000", "+55 11 9876", "11 8765-4321x"})
    void numeroInvalidoNaoNormaliza(String entrada) {
        assertThat(Telefones.paraE164(entrada)).isEmpty();
    }

    @Test
    void nuloOuVazioNaoNormaliza() {
        assertThat(Telefones.paraE164(null)).isEmpty();
        assertThat(Telefones.paraE164("  ")).isEmpty();
    }
}
