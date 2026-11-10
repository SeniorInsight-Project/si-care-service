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

/** Contato de emergência de um idoso (HU-011). O telefone fica gravado em E.164. */
@Entity
@Table(name = "contatos_emergencia")
public class ContatoEmergencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idoso_id", nullable = false)
    private UUID idosoId;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(name = "grau_parentesco", length = 50)
    private String grauParentesco;

    @Column(nullable = false)
    private int prioridade;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ContatoEmergencia() {
    }

    public ContatoEmergencia(UUID idosoId, String nome, String telefone, String grauParentesco, int prioridade) {
        this.idosoId = idosoId;
        preencher(nome, telefone, grauParentesco, prioridade);
    }

    public void preencher(String nome, String telefone, String grauParentesco, int prioridade) {
        this.nome = nome;
        this.telefone = telefone;
        this.grauParentesco = grauParentesco;
        this.prioridade = prioridade;
    }

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdosoId() {
        return idosoId;
    }

    public String getNome() {
        return nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getGrauParentesco() {
        return grauParentesco;
    }

    public int getPrioridade() {
        return prioridade;
    }
}
