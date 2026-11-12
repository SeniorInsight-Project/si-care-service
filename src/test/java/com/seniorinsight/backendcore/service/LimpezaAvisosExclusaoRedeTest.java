package com.seniorinsight.backendcore.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.seniorinsight.backendcore.repository.AvisoExclusaoRedeRepository;

/** O job apaga os avisos mais antigos que o prazo configurado. */
class LimpezaAvisosExclusaoRedeTest {

    private static final Instant AGORA = Instant.parse("2026-11-12T10:00:00Z");

    private final AvisoExclusaoRedeRepository avisos = mock(AvisoExclusaoRedeRepository.class);

    @Test
    void apagaOsAvisosGravadosAntesDoPrazoDeRetencao() {
        LimpezaAvisosExclusaoRede limpeza = new LimpezaAvisosExclusaoRede(avisos, Clock.fixed(AGORA, ZoneOffset.UTC),
                Duration.ofDays(7));

        limpeza.removerVencidos();

        verify(avisos).apagarCriadosAntes(Instant.parse("2026-11-05T10:00:00Z"));
    }

    @Test
    void oPrazoVemDaConfiguracao() {
        LimpezaAvisosExclusaoRede limpeza = new LimpezaAvisosExclusaoRede(avisos, Clock.fixed(AGORA, ZoneOffset.UTC),
                Duration.ofHours(1));

        limpeza.removerVencidos();

        verify(avisos).apagarCriadosAntes(Instant.parse("2026-11-12T09:00:00Z"));
    }
}
