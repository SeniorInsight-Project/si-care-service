package com.seniorinsight.backendcore.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Entidade JPA para o Alarme de Emergência / Botão SOS (HU35).
 * Registra o acionamento em tempo real de socorro de idosos monitorados,
 * incluindo coordenadas geográficas, nível de bateria do dispositivo e status de atendimento.
 */
@Entity
@Table(
        name = "alarmes_emergencia",
        indexes = {
                @Index(name = "idx_alarmes_emergencia_idoso", columnList = "idoso_id"),
                @Index(name = "idx_alarmes_emergencia_status", columnList = "status"),
                @Index(name = "idx_alarmes_emergencia_criado_em", columnList = "criado_em")
        }
)
public class AlarmeEmergencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idoso_id", nullable = false)
    private Idoso idoso;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "nivel_bateria")
    private Integer nivelBateria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAlarme status;

    @Column(name = "observacao", columnDefinition = "text")
    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public AlarmeEmergencia() {
    }

    public AlarmeEmergencia(UUID id, Idoso idoso, Double latitude, Double longitude,
                           Integer nivelBateria, StatusAlarme status, String observacao) {
        this.id = id;
        this.idoso = idoso;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nivelBateria = nivelBateria;
        this.status = status != null ? status : StatusAlarme.DISPARADO;
        this.observacao = observacao;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.criadoEm = now;
        this.atualizadoEm = now;
        if (this.status == null) {
            this.status = StatusAlarme.DISPARADO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = Instant.now();
    }

    /**
     * Atualiza o status do alarme com validação de transição para estados terminais.
     *
     * @param novoStatus Novo status desejado
     * @param observacao Justificativa ou notas do atendimento
     */
    public void atualizarStatus(StatusAlarme novoStatus, String observacao) {
        if (this.status != null && this.status.isFinalizado()) {
            throw new IllegalArgumentException(
                    "O alarme já se encontra finalizado com o status " + this.status + " e não pode sofrer novas alterações."
            );
        }
        if (novoStatus == null) {
            throw new IllegalArgumentException("O novo status não pode ser nulo.");
        }
        this.status = novoStatus;
        if (observacao != null && !observacao.isBlank()) {
            this.observacao = observacao;
        }
    }

    // --- Getters e Setters ---

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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getNivelBateria() {
        return nivelBateria;
    }

    public void setNivelBateria(Integer nivelBateria) {
        this.nivelBateria = nivelBateria;
    }

    public StatusAlarme getStatus() {
        return status;
    }

    public void setStatus(StatusAlarme status) {
        this.status = status;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    // --- Builder Pattern ---

    public static AlarmeEmergenciaBuilder builder() {
        return new AlarmeEmergenciaBuilder();
    }

    public static class AlarmeEmergenciaBuilder {
        private UUID id;
        private Idoso idoso;
        private Double latitude;
        private Double longitude;
        private Integer nivelBateria;
        private StatusAlarme status = StatusAlarme.DISPARADO;
        private String observacao;

        AlarmeEmergenciaBuilder() {
        }

        public AlarmeEmergenciaBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public AlarmeEmergenciaBuilder idoso(Idoso idoso) {
            this.idoso = idoso;
            return this;
        }

        public AlarmeEmergenciaBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public AlarmeEmergenciaBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public AlarmeEmergenciaBuilder nivelBateria(Integer nivelBateria) {
            this.nivelBateria = nivelBateria;
            return this;
        }

        public AlarmeEmergenciaBuilder status(StatusAlarme status) {
            this.status = status;
            return this;
        }

        public AlarmeEmergenciaBuilder observacao(String observacao) {
            this.observacao = observacao;
            return this;
        }

        public AlarmeEmergencia build() {
            return new AlarmeEmergencia(id, idoso, latitude, longitude, nivelBateria, status, observacao);
        }
    }
}
