package com.seniorinsight.backendcore.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Entidade JPA para Telemetria de Dispositivos Wearable (Smartwatches).
 * Persiste leituras em tempo real ou em lote (frequência cardíaca, passos diários, detecção de queda,
 * nível de bateria e saturação de O2) enviadas pelo aplicativo React Native e alinhadas ao padrão OpenWearables.
 */
@Entity
@Table(
        name = "telemetria_wearable",
        indexes = {
                @Index(name = "idx_telemetria_wearable_idoso_coletado", columnList = "idoso_id, coletado_em DESC"),
                @Index(name = "idx_telemetria_wearable_coletado_em", columnList = "coletado_em DESC"),
                @Index(name = "idx_telemetria_wearable_dispositivo", columnList = "dispositivo_id")
        }
)
public class TelemetriaWearable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idoso_id", nullable = false)
    private Idoso idoso;

    @Column(name = "dispositivo_id", length = 100)
    private String dispositivoId;

    @Column(name = "modelo_dispositivo", length = 100)
    private String modeloDispositivo;

    @Column(name = "provedor", length = 50)
    private String provedor;

    @Column(name = "frequencia_cardiaca")
    private Integer frequenciaCardiaca;

    @Column(name = "frequencia_cardiaca_repouso")
    private Integer frequenciaCardiacaRepouso;

    @Column(name = "variabilidade_cardiaca")
    private Double variabilidadeCardiaca;

    @Column(name = "passos")
    private Integer passos;

    @Column(name = "distancia_metros")
    private Double distanciaMetros;

    @Column(name = "calorias_queimadas")
    private Double caloriasQueimadas;

    @Column(name = "queda_detectada", nullable = false)
    private Boolean quedaDetectada = false;

    @Column(name = "forca_impacto_g")
    private Double forcaImpactoG;

    @Column(name = "confianca_queda")
    private Double confiancaQueda;

    @Column(name = "nivel_bateria")
    private Integer nivelBateria;

    @Column(name = "saturacao_oxigenio")
    private Double saturacaoOxigenio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_brutos", columnDefinition = "jsonb")
    private Map<String, Object> dadosBrutos = new HashMap<>();

    @Column(name = "coletado_em", nullable = false)
    private Instant coletadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    public TelemetriaWearable() {
    }

    public TelemetriaWearable(UUID id, Idoso idoso, String dispositivoId, String modeloDispositivo,
                              String provedor, Integer frequenciaCardiaca, Integer frequenciaCardiacaRepouso,
                              Double variabilidadeCardiaca, Integer passos, Double distanciaMetros,
                              Double caloriasQueimadas, Boolean quedaDetectada, Double forcaImpactoG,
                              Double confiancaQueda, Integer nivelBateria, Double saturacaoOxigenio,
                              Map<String, Object> dadosBrutos, Instant coletadoEm, Instant criadoEm) {
        this.id = id;
        this.idoso = idoso;
        this.dispositivoId = dispositivoId;
        this.modeloDispositivo = modeloDispositivo;
        this.provedor = provedor;
        this.frequenciaCardiaca = frequenciaCardiaca;
        this.frequenciaCardiacaRepouso = frequenciaCardiacaRepouso;
        this.variabilidadeCardiaca = variabilidadeCardiaca;
        this.passos = passos;
        this.distanciaMetros = distanciaMetros;
        this.caloriasQueimadas = caloriasQueimadas;
        this.quedaDetectada = quedaDetectada != null ? quedaDetectada : false;
        this.forcaImpactoG = forcaImpactoG;
        this.confiancaQueda = confiancaQueda;
        this.nivelBateria = nivelBateria;
        this.saturacaoOxigenio = saturacaoOxigenio;
        this.dadosBrutos = dadosBrutos != null ? dadosBrutos : new HashMap<>();
        this.coletadoEm = coletadoEm != null ? coletadoEm : Instant.now();
        this.criadoEm = criadoEm;
    }

    @PrePersist
    protected void onCreate() {
        if (this.criadoEm == null) {
            this.criadoEm = Instant.now();
        }
        if (this.coletadoEm == null) {
            this.coletadoEm = Instant.now();
        }
        if (this.quedaDetectada == null) {
            this.quedaDetectada = false;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Idoso idoso;
        private String dispositivoId;
        private String modeloDispositivo;
        private String provedor;
        private Integer frequenciaCardiaca;
        private Integer frequenciaCardiacaRepouso;
        private Double variabilidadeCardiaca;
        private Integer passos;
        private Double distanciaMetros;
        private Double caloriasQueimadas;
        private Boolean quedaDetectada = false;
        private Double forcaImpactoG;
        private Double confiancaQueda;
        private Integer nivelBateria;
        private Double saturacaoOxigenio;
        private Map<String, Object> dadosBrutos = new HashMap<>();
        private Instant coletadoEm;
        private Instant criadoEm;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder idoso(Idoso idoso) {
            this.idoso = idoso;
            return this;
        }

        public Builder dispositivoId(String dispositivoId) {
            this.dispositivoId = dispositivoId;
            return this;
        }

        public Builder modeloDispositivo(String modeloDispositivo) {
            this.modeloDispositivo = modeloDispositivo;
            return this;
        }

        public Builder provedor(String provedor) {
            this.provedor = provedor;
            return this;
        }

        public Builder frequenciaCardiaca(Integer frequenciaCardiaca) {
            this.frequenciaCardiaca = frequenciaCardiaca;
            return this;
        }

        public Builder frequenciaCardiacaRepouso(Integer frequenciaCardiacaRepouso) {
            this.frequenciaCardiacaRepouso = frequenciaCardiacaRepouso;
            return this;
        }

        public Builder variabilidadeCardiaca(Double variabilidadeCardiaca) {
            this.variabilidadeCardiaca = variabilidadeCardiaca;
            return this;
        }

        public Builder passos(Integer passos) {
            this.passos = passos;
            return this;
        }

        public Builder distanciaMetros(Double distanciaMetros) {
            this.distanciaMetros = distanciaMetros;
            return this;
        }

        public Builder caloriasQueimadas(Double caloriasQueimadas) {
            this.caloriasQueimadas = caloriasQueimadas;
            return this;
        }

        public Builder quedaDetectada(Boolean quedaDetectada) {
            this.quedaDetectada = quedaDetectada != null ? quedaDetectada : false;
            return this;
        }

        public Builder forcaImpactoG(Double forcaImpactoG) {
            this.forcaImpactoG = forcaImpactoG;
            return this;
        }

        public Builder confiancaQueda(Double confiancaQueda) {
            this.confiancaQueda = confiancaQueda;
            return this;
        }

        public Builder nivelBateria(Integer nivelBateria) {
            this.nivelBateria = nivelBateria;
            return this;
        }

        public Builder saturacaoOxigenio(Double saturacaoOxigenio) {
            this.saturacaoOxigenio = saturacaoOxigenio;
            return this;
        }

        public Builder dadosBrutos(Map<String, Object> dadosBrutos) {
            this.dadosBrutos = dadosBrutos != null ? dadosBrutos : new HashMap<>();
            return this;
        }

        public Builder coletadoEm(Instant coletadoEm) {
            this.coletadoEm = coletadoEm;
            return this;
        }

        public Builder criadoEm(Instant criadoEm) {
            this.criadoEm = criadoEm;
            return this;
        }

        public TelemetriaWearable build() {
            return new TelemetriaWearable(
                    id, idoso, dispositivoId, modeloDispositivo, provedor,
                    frequenciaCardiaca, frequenciaCardiacaRepouso, variabilidadeCardiaca,
                    passos, distanciaMetros, caloriasQueimadas, quedaDetectada,
                    forcaImpactoG, confiancaQueda, nivelBateria, saturacaoOxigenio,
                    dadosBrutos, coletadoEm, criadoEm
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Idoso getIdoso() {
        return idoso;
    }

    public void setIdoso(Idoso idoso) {
        this.idoso = idoso;
    }

    public String getDispositivoId() {
        return dispositivoId;
    }

    public void setDispositivoId(String dispositivoId) {
        this.dispositivoId = dispositivoId;
    }

    public String getModeloDispositivo() {
        return modeloDispositivo;
    }

    public void setModeloDispositivo(String modeloDispositivo) {
        this.modeloDispositivo = modeloDispositivo;
    }

    public String getProvedor() {
        return provedor;
    }

    public void setProvedor(String provedor) {
        this.provedor = provedor;
    }

    public Integer getFrequenciaCardiaca() {
        return frequenciaCardiaca;
    }

    public void setFrequenciaCardiaca(Integer frequenciaCardiaca) {
        this.frequenciaCardiaca = frequenciaCardiaca;
    }

    public Integer getFrequenciaCardiacaRepouso() {
        return frequenciaCardiacaRepouso;
    }

    public void setFrequenciaCardiacaRepouso(Integer frequenciaCardiacaRepouso) {
        this.frequenciaCardiacaRepouso = frequenciaCardiacaRepouso;
    }

    public Double getVariabilidadeCardiaca() {
        return variabilidadeCardiaca;
    }

    public void setVariabilidadeCardiaca(Double variabilidadeCardiaca) {
        this.variabilidadeCardiaca = variabilidadeCardiaca;
    }

    public Integer getPassos() {
        return passos;
    }

    public void setPassos(Integer passos) {
        this.passos = passos;
    }

    public Double getDistanciaMetros() {
        return distanciaMetros;
    }

    public void setDistanciaMetros(Double distanciaMetros) {
        this.distanciaMetros = distanciaMetros;
    }

    public Double getCaloriasQueimadas() {
        return caloriasQueimadas;
    }

    public void setCaloriasQueimadas(Double caloriasQueimadas) {
        this.caloriasQueimadas = caloriasQueimadas;
    }

    public Boolean getQuedaDetectada() {
        return quedaDetectada;
    }

    public void setQuedaDetectada(Boolean quedaDetectada) {
        this.quedaDetectada = quedaDetectada != null ? quedaDetectada : false;
    }

    public Double getForcaImpactoG() {
        return forcaImpactoG;
    }

    public void setForcaImpactoG(Double forcaImpactoG) {
        this.forcaImpactoG = forcaImpactoG;
    }

    public Double getConfiancaQueda() {
        return confiancaQueda;
    }

    public void setConfiancaQueda(Double confiancaQueda) {
        this.confiancaQueda = confiancaQueda;
    }

    public Integer getNivelBateria() {
        return nivelBateria;
    }

    public void setNivelBateria(Integer nivelBateria) {
        this.nivelBateria = nivelBateria;
    }

    public Double getSaturacaoOxigenio() {
        return saturacaoOxigenio;
    }

    public void setSaturacaoOxigenio(Double saturacaoOxigenio) {
        this.saturacaoOxigenio = saturacaoOxigenio;
    }

    public Map<String, Object> getDadosBrutos() {
        return dadosBrutos;
    }

    public void setDadosBrutos(Map<String, Object> dadosBrutos) {
        this.dadosBrutos = dadosBrutos != null ? dadosBrutos : new HashMap<>();
    }

    public Instant getColetadoEm() {
        return coletadoEm;
    }

    public void setColetadoEm(Instant coletadoEm) {
        this.coletadoEm = coletadoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}
