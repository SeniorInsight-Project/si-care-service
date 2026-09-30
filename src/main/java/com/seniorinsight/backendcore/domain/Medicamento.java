package com.seniorinsight.backendcore.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Entidade JPA para Medicamento (HU26).
 * Armazena informações cadastrais do fármaco, dosagem prescrita e instruções clínicas de uso.
 */
@Entity
@Table(name = "medicamentos")
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 100)
    private String dosagem;

    @Column(columnDefinition = "text")
    private String instrucoes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public Medicamento() {
    }

    public Medicamento(UUID id, String nome, String dosagem, String instrucoes) {
        this.id = id;
        this.nome = nome;
        this.dosagem = dosagem;
        this.instrucoes = instrucoes;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.criadoEm = now;
        this.atualizadoEm = now;
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDosagem() {
        return dosagem;
    }

    public void setDosagem(String dosagem) {
        this.dosagem = dosagem;
    }

    public String getInstrucoes() {
        return instrucoes;
    }

    public void setInstrucoes(String instrucoes) {
        this.instrucoes = instrucoes;
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

    public static MedicamentoBuilder builder() {
        return new MedicamentoBuilder();
    }

    public static class MedicamentoBuilder {
        private UUID id;
        private String nome;
        private String dosagem;
        private String instrucoes;

        MedicamentoBuilder() {
        }

        public MedicamentoBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public MedicamentoBuilder nome(String nome) {
            this.nome = nome;
            return this;
        }

        public MedicamentoBuilder dosagem(String dosagem) {
            this.dosagem = dosagem;
            return this;
        }

        public MedicamentoBuilder instrucoes(String instrucoes) {
            this.instrucoes = instrucoes;
            return this;
        }

        public Medicamento build() {
            Medicamento medicamento = new Medicamento(id, nome, dosagem, instrucoes);
            return medicamento;
        }
    }
}
