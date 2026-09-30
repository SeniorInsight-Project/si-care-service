package com.seniorinsight.backendcore.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.seniorinsight.backendcore.domain.AlarmeEmergencia;
import com.seniorinsight.backendcore.domain.Idoso;
import com.seniorinsight.backendcore.domain.StatusAlarme;
import com.seniorinsight.backendcore.domain.TelemetriaWearable;
import com.seniorinsight.backendcore.dto.DeteccaoQuedaRequestDTO;
import com.seniorinsight.backendcore.dto.FallDetectionDataDTO;
import com.seniorinsight.backendcore.dto.FrequenciaCardiacaRequestDTO;
import com.seniorinsight.backendcore.dto.HeartRateDataDTO;
import com.seniorinsight.backendcore.dto.OpenWearablesPayloadDTO;
import com.seniorinsight.backendcore.dto.PassosRequestDTO;
import com.seniorinsight.backendcore.dto.StepsDataDTO;
import com.seniorinsight.backendcore.dto.TelemetriaWearableBatchRequestDTO;
import com.seniorinsight.backendcore.dto.TelemetriaWearableRequestDTO;
import com.seniorinsight.backendcore.dto.TelemetriaWearableResponseDTO;
import com.seniorinsight.backendcore.event.SosAcionadoEvent;
import com.seniorinsight.backendcore.exception.IdosoNaoEncontradoException;
import com.seniorinsight.backendcore.repository.AlarmeEmergenciaRepository;
import com.seniorinsight.backendcore.repository.IdosoRepository;
import com.seniorinsight.backendcore.repository.TelemetriaWearableRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class WearableIntegrationServiceTest {

    @Mock
    private TelemetriaWearableRepository telemetriaRepository;

    @Mock
    private IdosoRepository idosoRepository;

    @Mock
    private AlarmeEmergenciaRepository alarmeEmergenciaRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WearableIntegrationService wearableIntegrationService;

    private UUID idosoId;
    private Idoso mockIdoso;

    @BeforeEach
    void setUp() {
        idosoId = UUID.randomUUID();
        mockIdoso = new Idoso();
        mockIdoso.setId(idosoId);
        mockIdoso.setNomeCompleto("Dona Maria Silva");
        mockIdoso.setDataNascimento(LocalDate.of(1948, 5, 20));
    }

    @Test
    @DisplayName("Deve processar e persistir telemetria consolidada com sucesso")
    void deveProcessarTelemetriaComSucesso() {
        HeartRateDataDTO heartRate = new HeartRateDataDTO(78, 64, 42.5);
        StepsDataDTO steps = new StepsDataDTO(4500, 3100.0, 180.0);
        FallDetectionDataDTO fall = new FallDetectionDataDTO(false, 0.0, 0.0, null);
        TelemetriaWearableRequestDTO request = new TelemetriaWearableRequestDTO(
                idosoId,
                "redmi-watch-4-01",
                "Xiaomi Redmi Watch 4",
                "XIAOMI_HEALTH",
                Instant.now(),
                heartRate,
                steps,
                fall,
                88,
                98.0,
                null
        );

        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(mockIdoso));
        when(telemetriaRepository.save(any(TelemetriaWearable.class))).thenAnswer(invocation -> {
            TelemetriaWearable t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            t.setCriadoEm(Instant.now());
            return t;
        });

        TelemetriaWearableResponseDTO response = wearableIntegrationService.processarTelemetria(idosoId, request);

        assertThat(response).isNotNull();
        assertThat(response.idosoId()).isEqualTo(idosoId);
        assertThat(response.frequenciaCardiacaBpm()).isEqualTo(78);
        assertThat(response.passosContagem()).isEqualTo(4500);
        assertThat(response.quedaDetectada()).isFalse();
        assertThat(response.statusProcessamento()).isEqualTo("PROCESSADO_COM_SUCESSO");

        verify(telemetriaRepository).save(any(TelemetriaWearable.class));
        verify(alarmeEmergenciaRepository, never()).save(any(AlarmeEmergencia.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Deve acionar alarme de emergência e SOS quando queda for detectada no wearable")
    void deveAcionarAlarmeSosQuandoQuedaDetectada() {
        HeartRateDataDTO heartRate = new HeartRateDataDTO(110, null, null);
        FallDetectionDataDTO fall = new FallDetectionDataDTO(true, 4.2, 0.95, Instant.now());
        TelemetriaWearableRequestDTO request = new TelemetriaWearableRequestDTO(
                idosoId,
                "apple-watch-s9",
                "Apple Watch Series 9",
                "APPLE_HEALTH",
                Instant.now(),
                heartRate,
                null,
                fall,
                65,
                96.0,
                null
        );

        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(mockIdoso));
        when(telemetriaRepository.save(any(TelemetriaWearable.class))).thenAnswer(invocation -> {
            TelemetriaWearable t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            t.setCriadoEm(Instant.now());
            return t;
        });

        UUID alarmeId = UUID.randomUUID();
        when(alarmeEmergenciaRepository.save(any(AlarmeEmergencia.class))).thenAnswer(invocation -> {
            AlarmeEmergencia a = invocation.getArgument(0);
            return new AlarmeEmergencia(alarmeId, mockIdoso, null, null, 65, StatusAlarme.DISPARADO, "Queda");
        });

        TelemetriaWearableResponseDTO response = wearableIntegrationService.processarTelemetria(idosoId, request);

        assertThat(response).isNotNull();
        assertThat(response.quedaDetectada()).isTrue();
        assertThat(response.statusProcessamento()).isEqualTo("ALERTA_QUEDA_DISPARADO");

        verify(alarmeEmergenciaRepository).save(any(AlarmeEmergencia.class));
        verify(eventPublisher).publishEvent(any(SosAcionadoEvent.class));
    }

    @Test
    @DisplayName("Deve lançar IdosoNaoEncontradoException se idoso não existir na ingestão")
    void deveLancarExcecaoSeIdosoNaoEncontrado() {
        UUID inexistente = UUID.randomUUID();
        TelemetriaWearableRequestDTO request = new TelemetriaWearableRequestDTO(
                inexistente, "dev-1", "Xiaomi Redmi Watch 4", "XIAOMI",
                Instant.now(), null, null, null, 80, 97.0, null
        );

        when(idosoRepository.findById(inexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wearableIntegrationService.processarTelemetria(inexistente, request))
                .isInstanceOf(IdosoNaoEncontradoException.class);

        verify(telemetriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve processar lote de telemetrias acumuladas (modo offline)")
    void deveProcessarLoteTelemetrias() {
        HeartRateDataDTO hr1 = new HeartRateDataDTO(72, null, null);
        HeartRateDataDTO hr2 = new HeartRateDataDTO(75, null, null);
        TelemetriaWearableRequestDTO t1 = new TelemetriaWearableRequestDTO(
                idosoId, "dev-1", "Watch", "OPEN_WEARABLES", Instant.now(), hr1, null, null, 90, 99.0, null
        );
        TelemetriaWearableRequestDTO t2 = new TelemetriaWearableRequestDTO(
                idosoId, "dev-1", "Watch", "OPEN_WEARABLES", Instant.now(), hr2, null, null, 89, 98.0, null
        );
        TelemetriaWearableBatchRequestDTO batch = new TelemetriaWearableBatchRequestDTO(List.of(t1, t2));

        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(mockIdoso));
        when(telemetriaRepository.save(any(TelemetriaWearable.class))).thenAnswer(inv -> {
            TelemetriaWearable tw = inv.getArgument(0);
            tw.setId(UUID.randomUUID());
            tw.setCriadoEm(Instant.now());
            return tw;
        });

        List<TelemetriaWearableResponseDTO> resultado = wearableIntegrationService.processarTelemetriaLote(idosoId, batch);

        assertThat(resultado).hasSize(2);
        verify(telemetriaRepository, org.mockito.Mockito.times(2)).save(any(TelemetriaWearable.class));
    }

    @Test
    @DisplayName("Deve processar frequência cardíaca pontual com sucesso")
    void deveProcessarFrequenciaCardiaca() {
        FrequenciaCardiacaRequestDTO req = new FrequenciaCardiacaRequestDTO(
                idosoId, 85, 68, 50.0, Instant.now(), "dev-1", "Xiaomi Redmi Watch 4"
        );

        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(mockIdoso));
        when(telemetriaRepository.save(any(TelemetriaWearable.class))).thenAnswer(inv -> {
            TelemetriaWearable tw = inv.getArgument(0);
            tw.setId(UUID.randomUUID());
            tw.setCriadoEm(Instant.now());
            return tw;
        });

        TelemetriaWearableResponseDTO resp = wearableIntegrationService.processarFrequenciaCardiaca(idosoId, req);

        assertThat(resp).isNotNull();
        assertThat(resp.frequenciaCardiacaBpm()).isEqualTo(85);
        assertThat(resp.frequenciaCardiacaRepousoBpm()).isEqualTo(68);
    }

    @Test
    @DisplayName("Deve processar contagem de passos pontual com sucesso")
    void deveProcessarPassos() {
        PassosRequestDTO req = new PassosRequestDTO(
                idosoId, 5200, 3700.0, 220.0, Instant.now(), "dev-1", "Xiaomi Redmi Watch 4"
        );

        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(mockIdoso));
        when(telemetriaRepository.save(any(TelemetriaWearable.class))).thenAnswer(inv -> {
            TelemetriaWearable tw = inv.getArgument(0);
            tw.setId(UUID.randomUUID());
            tw.setCriadoEm(Instant.now());
            return tw;
        });

        TelemetriaWearableResponseDTO resp = wearableIntegrationService.processarPassos(idosoId, req);

        assertThat(resp).isNotNull();
        assertThat(resp.passosContagem()).isEqualTo(5200);
        assertThat(resp.distanciaMetros()).isEqualTo(3700.0);
    }

    @Test
    @DisplayName("Deve processar alerta pontual de detecção de queda e disparar SOS")
    void deveProcessarDeteccaoQuedaPontual() {
        DeteccaoQuedaRequestDTO req = new DeteccaoQuedaRequestDTO(
                idosoId, true, 4.0, 0.96, Instant.now(), -23.55, -46.63, 75, "dev-1", "Redmi", "Queda na sala"
        );

        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(mockIdoso));
        when(telemetriaRepository.save(any(TelemetriaWearable.class))).thenAnswer(inv -> {
            TelemetriaWearable tw = inv.getArgument(0);
            tw.setId(UUID.randomUUID());
            tw.setCriadoEm(Instant.now());
            return tw;
        });

        when(alarmeEmergenciaRepository.save(any(AlarmeEmergencia.class))).thenAnswer(inv -> {
            return new AlarmeEmergencia(UUID.randomUUID(), mockIdoso, -23.55, -46.63, 75, StatusAlarme.DISPARADO, "Queda na sala");
        });

        TelemetriaWearableResponseDTO resp = wearableIntegrationService.processarDeteccaoQueda(idosoId, req);

        assertThat(resp).isNotNull();
        assertThat(resp.quedaDetectada()).isTrue();
        assertThat(resp.statusProcessamento()).isEqualTo("ALERTA_QUEDA_DISPARADO");

        verify(alarmeEmergenciaRepository).save(any(AlarmeEmergencia.class));
        verify(eventPublisher).publishEvent(any(SosAcionadoEvent.class));
    }

    @Test
    @DisplayName("Deve consultar histórico recente e última leitura")
    void deveConsultarHistoricoERecente() {
        TelemetriaWearable t1 = TelemetriaWearable.builder()
                .id(UUID.randomUUID())
                .idoso(mockIdoso)
                .frequenciaCardiaca(70)
                .coletadoEm(Instant.now())
                .criadoEm(Instant.now())
                .build();

        when(idosoRepository.findById(idosoId)).thenReturn(Optional.of(mockIdoso));
        when(telemetriaRepository.findByIdosoIdOrderByColetadoEmDesc(eq(idosoId), any(Pageable.class)))
                .thenReturn(List.of(t1));
        when(telemetriaRepository.findTopByIdosoIdOrderByColetadoEmDesc(idosoId))
                .thenReturn(Optional.of(t1));

        List<TelemetriaWearableResponseDTO> historico = wearableIntegrationService.buscarUltimasTelemetrias(idosoId, 10);
        TelemetriaWearableResponseDTO recente = wearableIntegrationService.buscarUltimaLeitura(idosoId);

        assertThat(historico).hasSize(1);
        assertThat(recente).isNotNull();
        assertThat(recente.frequenciaCardiacaBpm()).isEqualTo(70);
    }

    @Test
    @DisplayName("Deve despachar para nuvem OpenWearables quando habilitado via RestTemplate")
    void deveDespacharParaOpenWearablesQuandoHabilitado() {
        wearableIntegrationService.setOpenWearablesHabilitado(true);
        wearableIntegrationService.setOpenWearablesBaseUrl("https://api.openwearables.io");
        wearableIntegrationService.setOpenWearablesApiKey("test-key-123");

        OpenWearablesPayloadDTO payload = new OpenWearablesPayloadDTO(
                idosoId.toString(), "dev-1", "Redmi", "OPEN_WEARABLES", "telemetry",
                Instant.now(), new HeartRateDataDTO(75, 60, 40.0), null, null, 80, 98.0, null
        );

        when(restTemplate.postForEntity(eq("https://api.openwearables.io/v1/telemetry"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        wearableIntegrationService.despacharParaOpenWearables(payload);

        verify(restTemplate).postForEntity(eq("https://api.openwearables.io/v1/telemetry"), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    @DisplayName("Falha de rede ao despachar para OpenWearables não deve interromper fluxo (resiliência)")
    void falhaDeRedeOpenWearablesNaoInterrompeFluxo() {
        wearableIntegrationService.setOpenWearablesHabilitado(true);
        wearableIntegrationService.setOpenWearablesBaseUrl("https://api.openwearables.io");

        OpenWearablesPayloadDTO payload = new OpenWearablesPayloadDTO(
                idosoId.toString(), "dev-1", "Redmi", "OPEN_WEARABLES", "telemetry",
                Instant.now(), null, null, null, 80, 98.0, null
        );

        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RestClientException("Connection timed out"));

        // Não deve propagar exceção
        wearableIntegrationService.despacharParaOpenWearables(payload);

        verify(restTemplate).postForEntity(any(String.class), any(HttpEntity.class), eq(Void.class));
    }
}
