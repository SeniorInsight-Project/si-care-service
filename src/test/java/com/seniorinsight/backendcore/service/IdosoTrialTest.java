package com.seniorinsight.backendcore.service;

import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.seniorinsight.backendcore.domain.*;
import com.seniorinsight.backendcore.dto.IdosoRequestDTO;
import com.seniorinsight.backendcore.mensageria.TrialOutbox;
import com.seniorinsight.backendcore.repository.*;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;

class IdosoTrialTest {
    @Test
    void registraTrialSomenteNoPrimeiroCadastro() {
        var idosos = mock(IdosoRepository.class);
        var familiares = mock(FamiliarRepository.class);
        var vinculos = mock(VinculoRedeRepository.class);
        var outbox = mock(TrialOutbox.class);
        var familiar = new Familiar(UUID.randomUUID(), "sub", "Ana", null, null);
        when(familiares.travarPorSub("sub")).thenReturn(Optional.of(familiar));
        when(vinculos.existsByFamiliarAndPapelAcesso(familiar, PapelAcesso.ADMINISTRADOR)).thenReturn(false, true);
        when(idosos.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var service = new IdosoService(idosos, familiares, vinculos, mock(AuditoriaIdosoRepository.class),
                mock(AcessoIdosoService.class), Clock.systemUTC(), outbox);
        var usuario = new UsuarioAutenticado("sub", null, null, List.of());
        var request = new IdosoRequestDTO("Maria", LocalDate.of(1940, 1, 1), null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        service.cadastrar(usuario, request);
        service.cadastrar(usuario, request);
        verify(outbox, times(1)).registrar(familiar.getId(), "sub");
    }
}
