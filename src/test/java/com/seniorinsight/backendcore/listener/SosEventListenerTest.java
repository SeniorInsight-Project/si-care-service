package com.seniorinsight.backendcore.listener;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.VinculoRede;
import com.seniorinsight.backendcore.event.SosAcionadoEvent;
import com.seniorinsight.backendcore.security.UsuarioAutenticado;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.VinculoRedeRepository;

import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
class SosEventListenerTest {

    @Mock
    private VinculoRedeRepository vinculoRedeRepository;

    @Mock
    private IdosoRepository idosoRepository;

    @InjectMocks
    private SosEventListener sosEventListener;

    private UUID alarmeId;
    private UUID idosoId;
    private Idoso mockIdoso;
    private VinculoRede mockVinculo;

    @BeforeEach
    void setUp() {
        alarmeId = UUID.randomUUID();
        idosoId = UUID.randomUUID();

        mockIdoso = Idoso.builder()
                .id(idosoId)
                .nomeCompleto("Sebastião da Silva")
                .tipoSanguineo("O+")
                .alergias("Dipirona")
                .observacoesSocorros("Diabético insulino-dependente")
                .build();

        UsuarioAutenticado familiarUsuario = new UsuarioAutenticado(UUID.randomUUID().toString(), "filha@seniorinsight.com", null, List.of());

        Familiar mockFamiliar = Familiar.builder()
                .id(UUID.randomUUID())
                .nomeCompleto("Maria da Silva")
                .keycloakSub(familiarUsuario.keycloakSub())
                .build();

        mockVinculo = new VinculoRede(
                UUID.randomUUID(),
                mockFamiliar,
                mockIdoso,
                "Filha",
                "ADMINISTRADOR",
                null
        );
    }

    @Test
    @DisplayName("Deve processar notificação de emergência para membros da rede de proteção")
    void deveProcessarNotificacoesSosComSucesso() {
        SosAcionadoEvent event = new SosAcionadoEvent(
                alarmeId,
                idosoId,
                "Sebastião da Silva",
                -23.550520,
                -46.633308,
                90,
                "Queda no banheiro",
                Instant.now()
        );

        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(mockIdoso));
        when(vinculoRedeRepository.findByIdosoIdWithFamiliar(idosoId)).thenReturn(List.of(mockVinculo));

        assertThatNoException().isThrownBy(() -> sosEventListener.processarNotificacoesSos(event));

        verify(vinculoRedeRepository).findByIdosoIdWithFamiliar(idosoId);
        verify(idosoRepository).findById(idosoId);
    }

    @Test
    @DisplayName("Deve tratar adequadamente caso o idoso não possua membros na rede de proteção")
    void deveTratarQuandoRedeProtecaoVazia() {
        SosAcionadoEvent event = new SosAcionadoEvent(
                alarmeId,
                idosoId,
                "Sebastião da Silva",
                -23.550520,
                -46.633308,
                75,
                "Botão SOS acionado",
                Instant.now()
        );

        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(mockIdoso));
        when(vinculoRedeRepository.findByIdosoIdWithFamiliar(idosoId)).thenReturn(Collections.emptyList());

        assertThatNoException().isThrownBy(() -> sosEventListener.processarNotificacoesSos(event));

        verify(vinculoRedeRepository).findByIdosoIdWithFamiliar(idosoId);
    }
}
