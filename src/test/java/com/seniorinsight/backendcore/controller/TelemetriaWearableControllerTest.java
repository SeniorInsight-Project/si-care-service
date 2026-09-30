package com.seniorinsight.backendcore.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorinsight.backendcore.config.SecurityConfig;
import com.seniorinsight.backendcore.dto.DeteccaoQuedaRequestDTO;
import com.seniorinsight.backendcore.dto.FallDetectionDataDTO;
import com.seniorinsight.backendcore.dto.FrequenciaCardiacaRequestDTO;
import com.seniorinsight.backendcore.dto.HeartRateDataDTO;
import com.seniorinsight.backendcore.dto.PassosRequestDTO;
import com.seniorinsight.backendcore.dto.StepsDataDTO;
import com.seniorinsight.backendcore.dto.TelemetriaWearableBatchRequestDTO;
import com.seniorinsight.backendcore.dto.TelemetriaWearableRequestDTO;
import com.seniorinsight.backendcore.dto.TelemetriaWearableResponseDTO;
import com.seniorinsight.backendcore.repository.UsuarioRepository;
import com.seniorinsight.backendcore.security.JwtAuthenticationFilter;
import com.seniorinsight.backendcore.security.JwtService;
import com.seniorinsight.backendcore.service.WearableIntegrationService;

@WebMvcTest(controllers = TelemetriaWearableController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TelemetriaWearableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WearableIntegrationService wearableIntegrationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    private UUID idosoId;
    private UUID telemetriaId;

    @BeforeEach
    void setUp() {
        idosoId = UUID.randomUUID();
        telemetriaId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/idosos/{idosoId}/telemetria-wearable - Sucesso 201 Created")
    @WithMockUser
    void deveReceberTelemetriaComSucesso() throws Exception {
        HeartRateDataDTO heartRate = new HeartRateDataDTO(76, 62, 40.0);
        StepsDataDTO steps = new StepsDataDTO(3000, 2100.0, 120.0);
        FallDetectionDataDTO fall = new FallDetectionDataDTO(false, null, null, null);
        TelemetriaWearableRequestDTO request = new TelemetriaWearableRequestDTO(
                idosoId, "dev-1", "Xiaomi Redmi Watch 4", "XIAOMI_HEALTH", Instant.now(),
                heartRate, steps, fall, 85, 98.0, null
        );

        TelemetriaWearableResponseDTO responseDTO = new TelemetriaWearableResponseDTO(
                telemetriaId, idosoId, "dev-1", "Xiaomi Redmi Watch 4", "XIAOMI_HEALTH",
                76, 62, 40.0, 3000, 2100.0, 120.0, false, null, null, 85, 98.0,
                Instant.now(), Instant.now(), "PROCESSADO_COM_SUCESSO"
        );

        when(wearableIntegrationService.processarTelemetria(eq(idosoId), any(TelemetriaWearableRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/telemetria-wearable", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(telemetriaId.toString()))
                .andExpect(jsonPath("$.frequencia_cardiaca_bpm").value(76))
                .andExpect(jsonPath("$.passos_contagem").value(3000))
                .andExpect(jsonPath("$.queda_detectada").value(false))
                .andExpect(jsonPath("$.status_processamento").value("PROCESSADO_COM_SUCESSO"));
    }

    @Test
    @DisplayName("POST /api/v1/idosos/{idosoId}/telemetria-wearable - Detecção de queda responde 202 Accepted")
    @WithMockUser
    void deveResponder202AcceptedQuandoQuedaDetectada() throws Exception {
        FallDetectionDataDTO fall = new FallDetectionDataDTO(true, 4.5, 0.98, Instant.now());
        TelemetriaWearableRequestDTO request = new TelemetriaWearableRequestDTO(
                idosoId, "dev-1", "Xiaomi Redmi Watch 4", "XIAOMI_HEALTH", Instant.now(),
                null, null, fall, 85, 98.0, null
        );

        TelemetriaWearableResponseDTO responseDTO = new TelemetriaWearableResponseDTO(
                telemetriaId, idosoId, "dev-1", "Xiaomi Redmi Watch 4", "XIAOMI_HEALTH",
                null, null, null, null, null, null, true, 4.5, 0.98, 85, 98.0,
                Instant.now(), Instant.now(), "ALERTA_QUEDA_DISPARADO"
        );

        when(wearableIntegrationService.processarTelemetria(eq(idosoId), any(TelemetriaWearableRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/telemetria-wearable", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.queda_detectada").value(true))
                .andExpect(jsonPath("$.status_processamento").value("ALERTA_QUEDA_DISPARADO"));
    }

    @Test
    @DisplayName("POST /api/v1/idosos/{idosoId}/telemetria-wearable/lote - Sincronização em lote responde 201 Created")
    @WithMockUser
    void deveReceberTelemetriaLote() throws Exception {
        TelemetriaWearableRequestDTO t1 = new TelemetriaWearableRequestDTO(
                idosoId, "dev-1", "Watch", "OPEN_WEARABLES", Instant.now(),
                new HeartRateDataDTO(80, null, null), null, null, 90, null, null
        );
        TelemetriaWearableBatchRequestDTO batchRequest = new TelemetriaWearableBatchRequestDTO(List.of(t1));

        TelemetriaWearableResponseDTO responseDTO = new TelemetriaWearableResponseDTO(
                telemetriaId, idosoId, "dev-1", "Watch", "OPEN_WEARABLES",
                80, null, null, null, null, null, false, null, null, 90, null,
                Instant.now(), Instant.now(), "PROCESSADO_COM_SUCESSO"
        );

        when(wearableIntegrationService.processarTelemetriaLote(eq(idosoId), any(TelemetriaWearableBatchRequestDTO.class)))
                .thenReturn(List.of(responseDTO));

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/telemetria-wearable/lote", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(telemetriaId.toString()))
                .andExpect(jsonPath("$[0].frequencia_cardiaca_bpm").value(80));
    }

    @Test
    @DisplayName("POST /api/v1/idosos/{idosoId}/telemetria-wearable/frequencia-cardiaca - Sucesso 201 Created")
    @WithMockUser
    void deveReceberFrequenciaCardiacaGranular() throws Exception {
        FrequenciaCardiacaRequestDTO req = new FrequenciaCardiacaRequestDTO(
                idosoId, 72, 60, 48.0, Instant.now(), "dev-1", "Xiaomi Redmi Watch 4"
        );

        TelemetriaWearableResponseDTO responseDTO = new TelemetriaWearableResponseDTO(
                telemetriaId, idosoId, "dev-1", "Xiaomi Redmi Watch 4", "OPEN_WEARABLES",
                72, 60, 48.0, null, null, null, false, null, null, null, null,
                Instant.now(), Instant.now(), "PROCESSADO_COM_SUCESSO"
        );

        when(wearableIntegrationService.processarFrequenciaCardiaca(eq(idosoId), any(FrequenciaCardiacaRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/telemetria-wearable/frequencia-cardiaca", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.frequencia_cardiaca_bpm").value(72));
    }

    @Test
    @DisplayName("POST /api/v1/idosos/{idosoId}/telemetria-wearable/passos - Sucesso 201 Created")
    @WithMockUser
    void deveReceberPassosGranular() throws Exception {
        PassosRequestDTO req = new PassosRequestDTO(
                idosoId, 6000, 4200.0, 250.0, Instant.now(), "dev-1", "Xiaomi Redmi Watch 4"
        );

        TelemetriaWearableResponseDTO responseDTO = new TelemetriaWearableResponseDTO(
                telemetriaId, idosoId, "dev-1", "Xiaomi Redmi Watch 4", "OPEN_WEARABLES",
                null, null, null, 6000, 4200.0, 250.0, false, null, null, null, null,
                Instant.now(), Instant.now(), "PROCESSADO_COM_SUCESSO"
        );

        when(wearableIntegrationService.processarPassos(eq(idosoId), any(PassosRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/telemetria-wearable/passos", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passos_contagem").value(6000));
    }

    @Test
    @DisplayName("POST /api/v1/idosos/{idosoId}/telemetria-wearable/deteccao-queda - Alerta responde 202 Accepted")
    @WithMockUser
    void deveReceberDeteccaoQuedaGranular() throws Exception {
        DeteccaoQuedaRequestDTO req = new DeteccaoQuedaRequestDTO(
                idosoId, true, 3.9, 0.91, Instant.now(), -23.55, -46.63, 70, "dev-1", "Xiaomi", "Queda"
        );

        TelemetriaWearableResponseDTO responseDTO = new TelemetriaWearableResponseDTO(
                telemetriaId, idosoId, "dev-1", "Xiaomi", "OPEN_WEARABLES",
                null, null, null, null, null, null, true, 3.9, 0.91, 70, null,
                Instant.now(), Instant.now(), "ALERTA_QUEDA_DISPARADO"
        );

        when(wearableIntegrationService.processarDeteccaoQueda(eq(idosoId), any(DeteccaoQuedaRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/telemetria-wearable/deteccao-queda", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.queda_detectada").value(true))
                .andExpect(jsonPath("$.status_processamento").value("ALERTA_QUEDA_DISPARADO"));
    }

    @Test
    @DisplayName("GET /api/v1/idosos/{idosoId}/telemetria-wearable/ultimas - Retorna 200 OK com histórico")
    @WithMockUser
    void deveConsultarUltimasTelemetrias() throws Exception {
        TelemetriaWearableResponseDTO r1 = new TelemetriaWearableResponseDTO(
                telemetriaId, idosoId, "dev-1", "Watch", "OPEN_WEARABLES",
                75, null, null, 1500, null, null, false, null, null, 80, null,
                Instant.now(), Instant.now(), "HISTORICO"
        );

        when(wearableIntegrationService.buscarUltimasTelemetrias(idosoId, 30))
                .thenReturn(List.of(r1));

        mockMvc.perform(get("/api/v1/idosos/{idosoId}/telemetria-wearable/ultimas", idosoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(telemetriaId.toString()))
                .andExpect(jsonPath("$[0].frequencia_cardiaca_bpm").value(75));
    }

    @Test
    @DisplayName("GET /api/v1/idosos/{idosoId}/telemetria-wearable/recente - Retorna 200 OK com última leitura")
    @WithMockUser
    void deveConsultarUltimaLeitura() throws Exception {
        TelemetriaWearableResponseDTO r1 = new TelemetriaWearableResponseDTO(
                telemetriaId, idosoId, "dev-1", "Watch", "OPEN_WEARABLES",
                82, null, null, 2500, null, null, false, null, null, 77, null,
                Instant.now(), Instant.now(), "RECENTE"
        );

        when(wearableIntegrationService.buscarUltimaLeitura(idosoId))
                .thenReturn(r1);

        mockMvc.perform(get("/api/v1/idosos/{idosoId}/telemetria-wearable/recente", idosoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(telemetriaId.toString()))
                .andExpect(jsonPath("$.frequencia_cardiaca_bpm").value(82));
    }

    @Test
    @DisplayName("Rejeição 401 Unauthorized se token JWT estiver ausente")
    void deveRejeitarSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/idosos/{idosoId}/telemetria-wearable/recente", idosoId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rejeição 400 Bad Request se parâmetros forem inválidos")
    @WithMockUser
    void deveRejeitarPayloadInvalido() throws Exception {
        // Frequência cardíaca inválida: BPM = 10 (mínimo permitido é 30)
        HeartRateDataDTO invalidHeartRate = new HeartRateDataDTO(10, null, null);
        TelemetriaWearableRequestDTO invalidRequest = new TelemetriaWearableRequestDTO(
                idosoId, "dev-1", "Watch", "OPEN_WEARABLES", Instant.now(),
                invalidHeartRate, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/idosos/{idosoId}/telemetria-wearable", idosoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
