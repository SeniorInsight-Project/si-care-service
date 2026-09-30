package com.seniorinsight.backendcore.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

/**
 * Serviço de integração e ingestão de telemetria de dispositivos wearable (smartwatches).
 * Normaliza os dados coletados pelo aplicativo React Native (Expo) seguindo o padrão OpenWearables,
 * persiste as séries temporais no PostgreSQL, aciona alarmes imediatos em caso de detecção de queda
 * e prepara a infraestrutura para despacho na nuvem openwearables.io via RestTemplate.
 */
@Service
public class WearableIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(WearableIntegrationService.class);

    private final TelemetriaWearableRepository telemetriaRepository;
    private final IdosoRepository idosoRepository;
    private final AlarmeEmergenciaRepository alarmeEmergenciaRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RestTemplate restTemplate;

    @Value("${seniorinsight.integrations.openwearables.enabled:false}")
    private boolean openWearablesHabilitado;

    @Value("${seniorinsight.integrations.openwearables.base-url:https://api.openwearables.io}")
    private String openWearablesBaseUrl;

    @Value("${seniorinsight.integrations.openwearables.api-key:}")
    private String openWearablesApiKey;

    public WearableIntegrationService(
            TelemetriaWearableRepository telemetriaRepository,
            IdosoRepository idosoRepository,
            AlarmeEmergenciaRepository alarmeEmergenciaRepository,
            ApplicationEventPublisher eventPublisher,
            RestTemplate openWearablesRestTemplate) {
        this.telemetriaRepository = telemetriaRepository;
        this.idosoRepository = idosoRepository;
        this.alarmeEmergenciaRepository = alarmeEmergenciaRepository;
        this.eventPublisher = eventPublisher;
        this.restTemplate = openWearablesRestTemplate;
    }

    /**
     * Processa e persiste uma leitura consolidada de telemetria enviada pelo aplicativo móvel.
     */
    @Transactional
    public TelemetriaWearableResponseDTO processarTelemetria(UUID idosoId, TelemetriaWearableRequestDTO request) {
        UUID idosoFinalId = idosoId != null ? idosoId : (request != null ? request.idosoId() : null);
        if (idosoFinalId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        Idoso idoso = buscarIdoso(idosoFinalId);

        Integer bpm = request != null && request.heartRate() != null ? request.heartRate().bpm() : null;
        Integer restingBpm = request != null && request.heartRate() != null ? request.heartRate().restingBpm() : null;
        Double hrvMs = request != null && request.heartRate() != null ? request.heartRate().hrvMs() : null;

        Integer count = request != null && request.steps() != null ? request.steps().count() : null;
        Double distanceMeters = request != null && request.steps() != null ? request.steps().distanceMeters() : null;
        Double caloriesKcal = request != null && request.steps() != null ? request.steps().caloriesKcal() : null;

        boolean quedaDetectada = request != null && request.fallDetection() != null
                && Boolean.TRUE.equals(request.fallDetection().fallDetected());
        Double impactForceG = request != null && request.fallDetection() != null ? request.fallDetection().impactForceG() : null;
        Double confidence = request != null && request.fallDetection() != null ? request.fallDetection().confidence() : null;

        Instant coletadoEm = (request != null && request.timestamp() != null) ? request.timestamp() : Instant.now();

        Map<String, Object> dadosBrutos = (request != null && request.metadata() != null)
                ? new HashMap<>(request.metadata())
                : new HashMap<>();

        TelemetriaWearable telemetria = TelemetriaWearable.builder()
                .idoso(idoso)
                .dispositivoId(request != null ? request.deviceId() : null)
                .modeloDispositivo(request != null ? request.deviceModel() : null)
                .provedor(request != null && request.provider() != null ? request.provider() : "OPEN_WEARABLES")
                .frequenciaCardiaca(bpm)
                .frequenciaCardiacaRepouso(restingBpm)
                .variabilidadeCardiaca(hrvMs)
                .passos(count)
                .distanciaMetros(distanceMeters)
                .caloriasQueimadas(caloriesKcal)
                .quedaDetectada(quedaDetectada)
                .forcaImpactoG(impactForceG)
                .confiancaQueda(confidence)
                .nivelBateria(request != null ? request.batteryLevel() : null)
                .saturacaoOxigenio(request != null ? request.spo2Percentage() : null)
                .dadosBrutos(dadosBrutos)
                .coletadoEm(coletadoEm)
                .build();

        TelemetriaWearable salvo = telemetriaRepository.save(telemetria);
        log.info("[WEARABLE] Telemetria registrada para idoso {}: BPM={}, Passos={}, Queda={}",
                idosoFinalId, bpm, count, quedaDetectada);

        String statusProcessamento = "PROCESSADO_COM_SUCESSO";

        // Protocolo Crítico de Queda: se queda for detectada, aciona alarme de emergência e SOS
        if (quedaDetectada) {
            acionarAlarmeQueda(idoso, salvo, null, null, "Queda detectada automaticamente por sensor wearable (Smartwatch)");
            statusProcessamento = "ALERTA_QUEDA_DISPARADO";
        }

        // Despacho opcional para nuvem OpenWearables
        despacharParaOpenWearablesAssincrono(salvo, request);

        return TelemetriaWearableResponseDTO.fromEntity(salvo, statusProcessamento);
    }

    /**
     * Ingestão em lote de telemetrias acumuladas (modo offline do React Native).
     */
    @Transactional
    public List<TelemetriaWearableResponseDTO> processarTelemetriaLote(UUID idosoId, TelemetriaWearableBatchRequestDTO batchRequest) {
        if (batchRequest == null || batchRequest.readings() == null || batchRequest.readings().isEmpty()) {
            throw new IllegalArgumentException("O lote de telemetrias não contém leituras.");
        }

        return batchRequest.readings().stream()
                .map(reading -> processarTelemetria(idosoId, reading))
                .toList();
    }

    /**
     * Ingestão granular para frequência cardíaca em tempo real.
     */
    @Transactional
    public TelemetriaWearableResponseDTO processarFrequenciaCardiaca(UUID idosoId, FrequenciaCardiacaRequestDTO request) {
        UUID idosoFinalId = idosoId != null ? idosoId : (request != null ? request.idosoId() : null);
        if (idosoFinalId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        Idoso idoso = buscarIdoso(idosoFinalId);

        Instant coletadoEm = (request != null && request.timestamp() != null) ? request.timestamp() : Instant.now();

        TelemetriaWearable telemetria = TelemetriaWearable.builder()
                .idoso(idoso)
                .dispositivoId(request != null ? request.deviceId() : null)
                .modeloDispositivo(request != null ? request.deviceModel() : null)
                .provedor("OPEN_WEARABLES")
                .frequenciaCardiaca(request != null ? request.bpm() : null)
                .frequenciaCardiacaRepouso(request != null ? request.restingBpm() : null)
                .variabilidadeCardiaca(request != null ? request.hrvMs() : null)
                .coletadoEm(coletadoEm)
                .build();

        TelemetriaWearable salvo = telemetriaRepository.save(telemetria);
        log.info("[WEARABLE] Frequência cardíaca registrada para idoso {}: {} BPM", idosoFinalId, request != null ? request.bpm() : null);

        despacharParaOpenWearablesAssincrono(salvo, null);

        return TelemetriaWearableResponseDTO.fromEntity(salvo, "PROCESSADO_COM_SUCESSO");
    }

    /**
     * Ingestão granular para passos e locomoção.
     */
    @Transactional
    public TelemetriaWearableResponseDTO processarPassos(UUID idosoId, PassosRequestDTO request) {
        UUID idosoFinalId = idosoId != null ? idosoId : (request != null ? request.idosoId() : null);
        if (idosoFinalId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        Idoso idoso = buscarIdoso(idosoFinalId);

        Instant coletadoEm = (request != null && request.timestamp() != null) ? request.timestamp() : Instant.now();

        TelemetriaWearable telemetria = TelemetriaWearable.builder()
                .idoso(idoso)
                .dispositivoId(request != null ? request.deviceId() : null)
                .modeloDispositivo(request != null ? request.deviceModel() : null)
                .provedor("OPEN_WEARABLES")
                .passos(request != null ? request.count() : null)
                .distanciaMetros(request != null ? request.distanceMeters() : null)
                .caloriasQueimadas(request != null ? request.caloriesKcal() : null)
                .coletadoEm(coletadoEm)
                .build();

        TelemetriaWearable salvo = telemetriaRepository.save(telemetria);
        log.info("[WEARABLE] Passos registrados para idoso {}: {} passos", idosoFinalId, request != null ? request.count() : null);

        despacharParaOpenWearablesAssincrono(salvo, null);

        return TelemetriaWearableResponseDTO.fromEntity(salvo, "PROCESSADO_COM_SUCESSO");
    }

    /**
     * Ingestão crítica para alerta de queda detectada pelo smartwatch.
     */
    @Transactional
    public TelemetriaWearableResponseDTO processarDeteccaoQueda(UUID idosoId, DeteccaoQuedaRequestDTO request) {
        UUID idosoFinalId = idosoId != null ? idosoId : (request != null ? request.idosoId() : null);
        if (idosoFinalId == null) {
            throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");
        }

        Idoso idoso = buscarIdoso(idosoFinalId);

        boolean quedaConfirmada = request != null && Boolean.TRUE.equals(request.fallDetected());
        Instant coletadoEm = (request != null && request.timestamp() != null) ? request.timestamp() : Instant.now();

        Map<String, Object> dadosBrutos = new HashMap<>();
        if (request != null) {
            if (request.latitude() != null) dadosBrutos.put("latitude", request.latitude());
            if (request.longitude() != null) dadosBrutos.put("longitude", request.longitude());
            if (request.observacao() != null) dadosBrutos.put("observacao", request.observacao());
        }

        TelemetriaWearable telemetria = TelemetriaWearable.builder()
                .idoso(idoso)
                .dispositivoId(request != null ? request.deviceId() : null)
                .modeloDispositivo(request != null ? request.deviceModel() : null)
                .provedor("OPEN_WEARABLES")
                .quedaDetectada(quedaConfirmada)
                .forcaImpactoG(request != null ? request.impactForceG() : null)
                .confiancaQueda(request != null ? request.confidence() : null)
                .nivelBateria(request != null ? request.batteryLevel() : null)
                .dadosBrutos(dadosBrutos)
                .coletadoEm(coletadoEm)
                .build();

        TelemetriaWearable salvo = telemetriaRepository.save(telemetria);

        String statusProcessamento = "PROCESSADO_COM_SUCESSO";
        if (quedaConfirmada) {
            Double lat = request != null ? request.latitude() : null;
            Double lng = request != null ? request.longitude() : null;
            String obs = request != null && request.observacao() != null ? request.observacao() : "Queda confirmada por acelerômetro e giroscópio de smartwatch";
            acionarAlarmeQueda(idoso, salvo, lat, lng, obs);
            statusProcessamento = "ALERTA_QUEDA_DISPARADO";
        }

        despacharParaOpenWearablesAssincrono(salvo, null);

        return TelemetriaWearableResponseDTO.fromEntity(salvo, statusProcessamento);
    }

    /**
     * Consulta as leituras mais recentes para renderização dos gráficos no app.
     */
    @Transactional(readOnly = true)
    public List<TelemetriaWearableResponseDTO> buscarUltimasTelemetrias(UUID idosoId, int limite) {
        buscarIdoso(idosoId);
        int max = Math.max(1, Math.min(limite, 500));
        return telemetriaRepository.findByIdosoIdOrderByColetadoEmDesc(idosoId, PageRequest.of(0, max))
                .stream()
                .map(t -> TelemetriaWearableResponseDTO.fromEntity(t, "HISTORICO"))
                .toList();
    }

    /**
     * Obtém a leitura mais recente de um idoso.
     */
    @Transactional(readOnly = true)
    public TelemetriaWearableResponseDTO buscarUltimaLeitura(UUID idosoId) {
        buscarIdoso(idosoId);
        return telemetriaRepository.findTopByIdosoIdOrderByColetadoEmDesc(idosoId)
                .map(t -> TelemetriaWearableResponseDTO.fromEntity(t, "RECENTE"))
                .orElse(null);
    }

    /**
     * Despacha os dados de telemetria normalizados para a nuvem da OpenWearables via RestTemplate.
     * Trata falhas de rede de forma tolerante a falhas (resiliente), sem interromper o fluxo operacional.
     */
    public void despacharParaOpenWearables(OpenWearablesPayloadDTO payload) {
        if (!openWearablesHabilitado) {
            log.debug("[OPENWEARABLES] Integração em nuvem desabilitada. Omissão de despacho para {}", payload.userId());
            return;
        }

        if (payload == null) {
            log.warn("[OPENWEARABLES] Tentativa de despachar payload nulo para OpenWearables.");
            return;
        }

        try {
            String endpoint = openWearablesBaseUrl + "/v1/telemetry";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (openWearablesApiKey != null && !openWearablesApiKey.isBlank()) {
                headers.setBearerAuth(openWearablesApiKey);
            }

            HttpEntity<OpenWearablesPayloadDTO> entity = new HttpEntity<>(payload, headers);
            log.info("[OPENWEARABLES] Enviando dados para {}: user_id={}, device={}",
                    endpoint, payload.userId(), payload.deviceId());

            restTemplate.postForEntity(endpoint, entity, Void.class);
            log.info("[OPENWEARABLES] Dados despachados com sucesso para {}", endpoint);
        } catch (RestClientException ex) {
            log.error("[OPENWEARABLES] Falha de comunicação ao despachar telemetria para a nuvem OpenWearables: {}",
                    ex.getMessage());
        }
    }

    private void despacharParaOpenWearablesAssincrono(TelemetriaWearable telemetria, TelemetriaWearableRequestDTO requestOriginal) {
        if (!openWearablesHabilitado) {
            return;
        }

        HeartRateDataDTO heartRate = telemetria.getFrequenciaCardiaca() != null
                ? new HeartRateDataDTO(telemetria.getFrequenciaCardiaca(), telemetria.getFrequenciaCardiacaRepouso(), telemetria.getVariabilidadeCardiaca())
                : null;

        StepsDataDTO steps = telemetria.getPassos() != null
                ? new StepsDataDTO(telemetria.getPassos(), telemetria.getDistanciaMetros(), telemetria.getCaloriasQueimadas())
                : null;

        FallDetectionDataDTO fallDetection = new FallDetectionDataDTO(
                telemetria.getQuedaDetectada(),
                telemetria.getForcaImpactoG(),
                telemetria.getConfiancaQueda(),
                telemetria.getColetadoEm()
        );

        OpenWearablesPayloadDTO payload = new OpenWearablesPayloadDTO(
                telemetria.getIdoso() != null ? telemetria.getIdoso().getId().toString() : null,
                telemetria.getDispositivoId(),
                telemetria.getModeloDispositivo(),
                telemetria.getProvedor(),
                "telemetry",
                telemetria.getColetadoEm(),
                heartRate,
                steps,
                fallDetection,
                telemetria.getNivelBateria(),
                telemetria.getSaturacaoOxigenio(),
                telemetria.getDadosBrutos()
        );

        despacharParaOpenWearables(payload);
    }

    private void acionarAlarmeQueda(Idoso idoso, TelemetriaWearable telemetria, Double latitude, Double longitude, String observacao) {
        try {
            AlarmeEmergencia alarme = new AlarmeEmergencia(
                    null,
                    idoso,
                    latitude,
                    longitude,
                    telemetria.getNivelBateria(),
                    StatusAlarme.DISPARADO,
                    observacao
            );

            AlarmeEmergencia alarmeSalvo = alarmeEmergenciaRepository.save(alarme);
            log.warn("[ALERTA CRÍTICO] Queda confirmada para idoso {} ({})! Alarme SOS ID: {}",
                    idoso.getId(), idoso.getNomeCompleto(), alarmeSalvo.getId());

            // Publica o evento assíncrono para notificação imediata da rede de proteção
            eventPublisher.publishEvent(new SosAcionadoEvent(
                    alarmeSalvo.getId(),
                    idoso.getId(),
                    idoso.getNomeCompleto(),
                    latitude,
                    longitude,
                    telemetria.getNivelBateria(),
                    observacao,
                    alarmeSalvo.getCriadoEm()
            ));
        } catch (Exception ex) {
            log.error("[ALERTA CRÍTICO] Falha ao acionar alarme de emergência para detecção de queda: {}", ex.getMessage(), ex);
        }
    }

    private Idoso buscarIdoso(UUID idosoId) {
        return idosoRepository.findById(idosoId)
                .orElseThrow(() -> {
                    log.warn("[WEARABLE] Idoso não encontrado: {}", idosoId);
                    return new IdosoNaoEncontradoException(idosoId);
                });
    }

    // Getters e setters para testes e inspeção
    public boolean isOpenWearablesHabilitado() {
        return openWearablesHabilitado;
    }

    public void setOpenWearablesHabilitado(boolean openWearablesHabilitado) {
        this.openWearablesHabilitado = openWearablesHabilitado;
    }

    public String getOpenWearablesBaseUrl() {
        return openWearablesBaseUrl;
    }

    public void setOpenWearablesBaseUrl(String openWearablesBaseUrl) {
        this.openWearablesBaseUrl = openWearablesBaseUrl;
    }

    public String getOpenWearablesApiKey() {
        return openWearablesApiKey;
    }

    public void setOpenWearablesApiKey(String openWearablesApiKey) {
        this.openWearablesApiKey = openWearablesApiKey;
    }
}
