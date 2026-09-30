package com.seniorinsight.backendcore.domain;

import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.backendcore.model.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Entidade JPA para Histórico de Medicação (HU26).
 * Registra o log de ingestão com status (TOMADO, ATRASADO, IGNORADO), timestamp exato
 * e referência temporal prevista para auditoria da tolerância clínica de 30 minutos.
 */
@Entity
@Table(name = "historicos_medicacao")
public class HistoricoMedicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rotina_medicamento_id", nullable = false)
    private RotinaMedicamento rotina;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusIngestao status;

    @Column(name = "data_hora_ingestao", nullable = false)
    private Instant dataHoraIngestao;

    @Column(name = "data_hora_prevista")
    private Instant dataHoraPrevista;

    @Column(length = 500)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id")
    private Usuario registradoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    public HistoricoMedicacao() {
    }

    public HistoricoMedicacao(UUID id, RotinaMedicamento rotina, StatusIngestao status,
                             Instant dataHoraIngestao, Instant dataHoraPrevista,
                             String observacao, Usuario registradoPor) {
        this.id = id;
        this.rotina = rotina;
        this.status = status;
        this.dataHoraIngestao = dataHoraIngestao;
        this.dataHoraPrevista = dataHoraPrevista;
        this.observacao = observacao;
        this.registradoPor = registradoPor;
    }

    @PrePersist
    protected void onCreate() {
        if (this.criadoEm == null) {
            this.criadoEm = Instant.now();
        }
        if (this.dataHoraIngestao == null) {
            this.dataHoraIngestao = this.criadoEm;
        }
    }

    // --- Getters e Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RotinaMedicamento getRotina() {
        return rotina;
    }

    public void setRotina(RotinaMedicamento rotina) {
        this.rotina = rotina;
    }

    public StatusIngestao getStatus() {
        return status;
    }

    public void setStatus(StatusIngestao status) {
        this.status = status;
    }

    public Instant getDataHoraIngestao() {
        return dataHoraIngestao;
    }

    public void setDataHoraIngestao(Instant dataHoraIngestao) {
        this.dataHoraIngestao = dataHoraIngestao;
    }

    public Instant getDataHoraPrevista() {
        return dataHoraPrevista;
    }

    public void setDataHoraPrevista(Instant dataHoraPrevista) {
        this.dataHoraPrevista = dataHoraPrevista;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Usuario registradoPor) {
        this.registradoPor = registradoPor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    // --- Builder Pattern ---

    public static HistoricoMedicacaoBuilder builder() {
        return new HistoricoMedicacaoBuilder();
    }

    public static class HistoricoMedicacaoBuilder {
        private UUID id;
        private RotinaMedicamento rotina;
        private StatusIngestao status;
        private Instant dataHoraIngestao;
        private Instant dataHoraPrevista;
        private String observacao;
        private Usuario registradoPor;

        HistoricoMedicacaoBuilder() {
        }

        public HistoricoMedicacaoBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public HistoricoMedicacaoBuilder rotina(RotinaMedicamento rotina) {
            this.rotina = rotina;
            return this;
        }

        public HistoricoMedicacaoBuilder status(StatusIngestao status) {
            this.status = status;
            return this;
        }

        public HistoricoMedicacaoBuilder dataHoraIngestao(Instant dataHoraIngestao) {
            this.dataHoraIngestao = dataHoraIngestao;
            return this;
        }

        public HistoricoMedicacaoBuilder dataHoraPrevista(Instant dataHoraPrevista) {
            this.dataHoraPrevista = dataHoraPrevista;
            return this;
        }

        public HistoricoMedicacaoBuilder observacao(String observacao) {
            this.observacao = observacao;
            return this;
        }

        public HistoricoMedicacaoBuilder registradoPor(Usuario registradoPor) {
            this.registradoPor = registradoPor;
            return this;
        }

        public HistoricoMedicacao build() {
            return new HistoricoMedicacao(id, rotina, status, dataHoraIngestao, dataHoraPrevista, observacao, registradoPor);
        }
    }
}
