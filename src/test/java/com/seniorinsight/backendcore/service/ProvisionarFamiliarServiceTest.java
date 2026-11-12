package com.seniorinsight.backendcore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.repository.FamiliarRepository;
import com.seniorinsight.backendcore.repository.TitularExcluidoRepository;

/** user.created não recria o familiar de um titular que já pediu a exclusão. */
class ProvisionarFamiliarServiceTest {

    private static final String SUB = "sub-1";

    private final FamiliarRepository familiares = mock(FamiliarRepository.class);
    private final TitularExcluidoRepository titulares = mock(TitularExcluidoRepository.class);
    private final ProvisionarFamiliarService servico = new ProvisionarFamiliarService(familiares, titulares);

    @Test
    void criaOFamiliarQuandoOTitularNaoFoiExcluido() {
        when(titulares.contem(SUB)).thenReturn(false);

        assertThat(servico.criarSeAusente(SUB, "Ana", "ana@exemplo.com")).isTrue();

        InOrder ordem = inOrder(titulares, familiares);
        ordem.verify(titulares).travar(SUB);
        ordem.verify(familiares).save(any(Familiar.class));
    }

    @Test
    void naoRecriaOFamiliarDeTitularExcluido() {
        when(titulares.contem(SUB)).thenReturn(true);

        assertThat(servico.criarSeAusente(SUB, "Ana", "ana@exemplo.com")).isFalse();

        verify(familiares, never()).save(any());
    }
}
