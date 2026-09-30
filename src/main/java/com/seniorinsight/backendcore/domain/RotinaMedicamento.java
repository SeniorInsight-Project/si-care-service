package com.seniorinsight.backendcore.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Entidade JPA para Rotina de Medicamentos (HU26).
 * Associa um Medicamento prescrito a um Idoso monitorado, definindo o horário agendado,
 * a frequência, vigência do tratamento e controle de status ativo.
 */
@Entity
@Table(name = "rotinas_medicamentos")
public class RotinaMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idoso_id", nullable = false)
    private Idoso idoso;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Column(nullable = false)
    private LocalTime horario;

    @Column(nullable = false, length = 100)
    private String frequencia;

    @Column(name = "dias_semana", length = 100)
    private String diasSemana = "TODOS";

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(columnDefinition = "text")
    private String observacoes;

    @OneToMany(mappedBy = "rotina", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistoricoMedicacao> historicos = new ArrayList<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public RotinaMedicamento() {
    }

    public RotinaMedicamento(UUID id, Idoso idoso, Medicamento medicamento, LocalTime horario,
                             String frequencia, String diasSemana, LocalDate dataInicio,
                             LocalDate dataFim, Boolean ativo, String observacoes) {
        this.id = id;
        this.idoso = idoso;
        this.medicamento = medicamento;
        this.horario = horario;
        this.frequencia = frequencia;
        this.diasSemana = diasSemana != null ? diasSemana : "TODOS";
        this.dataInicio = dataInicio != null ? dataInicio : LocalDate.now();
        this.dataFim = dataFim;
        this.ativo = ativo != null ? ativo : true;
        this.observacoes = observacoes;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.criadoEm = now;
        this.atualizadoEm = now;
        if (this.diasSemana == null) {
            this.diasSemana = "TODOS";
        }
        if (this.dataInicio == null) {
            this.dataInicio = LocalDate.now();
        }
        if (this.ativo == null) {
            this.ativo = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = Instant.now();
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

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public void setMedicamento(Medicamento medicamento) {
        this.medicamento = medicamento;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public void setHorario(LocalTime horario) {
        this.horario = horario;
    }

    public String getFrequencia() {
        return frequencia;
    }

    public void setFrequencia(String frequencia) {
        this.frequencia = frequencia;
    }

    public String getDiasSemana() {
        return diasSemana;
    }

    public void setDiasSemana(String diasSemana) {
        this.diasSemana = diasSemana;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public List<HistoricoMedicacao> getHistoricos() {
        return historicos;
    }

    public void setHistoricos(List<HistoricoMedicacao> historicos) {
        this.historicos = historicos;
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

    // --- Helper Methods ---

    public void addHistorico(HistoricoMedicacao historico) {
        this.historicos.add(historico);
        historico.setRotina(this);
    }

    // --- Builder Pattern ---

    public static RotinaMedicamentoBuilder builder() {
        return new RotinaMedicamentoBuilder();
    }

    public static class RotinaMedicamentoBuilder {
        private UUID id;
        private Idoso idoso;
        private Medicamento medicamento;
        private LocalTime horario;
        private String frequencia;
        private String diasSemana = "TODOS";
        private LocalDate dataInicio = LocalDate.now();
        private LocalDate dataFim;
        private Boolean ativo = true;
        private String observacoes;

        RotinaMedicamentoBuilder() {
        }

        public RotinaMedicamentoBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public RotinaMedicamentoBuilder idoso(Idoso idoso) {
            this.idoso = idoso;
            return this;
        }

        public RotinaMedicamentoBuilder medicamento(Medicamento medicamento) {
            this.medicamento = medicamento;
            return this;
        }

        public RotinaMedicamentoBuilder horario(LocalTime horario) {
            this.horario = horario;
            return this;
        }

        public RotinaMedicamentoBuilder frequencia(String frequencia) {
            this.frequencia = frequencia;
            return this;
        }

        public RotinaMedicamentoBuilder diasSemana(String diasSemana) {
            this.diasSemana = diasSemana;
            return this;
        }

        public RotinaMedicamentoBuilder dataInicio(LocalDate dataInicio) {
            this.dataInicio = dataInicio;
            return this;
        }

        public RotinaMedicamentoBuilder dataFim(LocalDate dataFim) {
            this.dataFim = dataFim;
            return this;
        }

        public RotinaMedicamentoBuilder ativo(Boolean ativo) {
            this.ativo = ativo;
            return this;
        }

        public RotinaMedicamentoBuilder observacoes(String observacoes) {
            this.observacoes = observacoes;
            return this;
        }

        public RotinaMedicamento build() {
            return new RotinaMedicamento(id, idoso, medicamento, horario, frequencia, diasSemana, dataInicio, dataFim, ativo, observacoes);
        }
    }
}
