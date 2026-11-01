package com.seniorinsight.backendcore.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Entidade JPA para o perfil de Médico (HU-002 / DRS).
 * Ligado ao usuário do Keycloak pela claim sub (keycloakSub).
 */
@Entity
@Table(name = "medicos")
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_sub", nullable = false, unique = true, length = 64)
    private String keycloakSub;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(nullable = false, unique = true, length = 50)
    private String crm;

    @Column(nullable = false, length = 100)
    private String especialidade;

    @Column(name = "telefone_consultorio", length = 20)
    private String telefoneConsultorio;

    @Column(name = "nome_clinica")
    private String nomeClinica;

    @Column(length = 20)
    private String cnpj;

    @ManyToMany
    @JoinTable(
            name = "medicos_idosos",
            joinColumns = @JoinColumn(name = "medico_id"),
            inverseJoinColumns = @JoinColumn(name = "idoso_id")
    )
    private List<Idoso> idosos = new ArrayList<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public Medico() {
    }

    public Medico(UUID id, String keycloakSub, String nomeCompleto, String crm, String especialidade,
                  String telefoneConsultorio, String nomeClinica, String cnpj) {
        this.id = id;
        this.keycloakSub = keycloakSub;
        this.nomeCompleto = nomeCompleto;
        this.crm = crm;
        this.especialidade = especialidade;
        this.telefoneConsultorio = telefoneConsultorio;
        this.nomeClinica = nomeClinica;
        this.cnpj = cnpj;
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

    public String getKeycloakSub() {
        return keycloakSub;
    }

    public void setKeycloakSub(String keycloakSub) {
        this.keycloakSub = keycloakSub;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getCrm() {
        return crm;
    }

    public void setCrm(String crm) {
        this.crm = crm;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
    }

    public String getTelefoneConsultorio() {
        return telefoneConsultorio;
    }

    public void setTelefoneConsultorio(String telefoneConsultorio) {
        this.telefoneConsultorio = telefoneConsultorio;
    }

    public String getNomeClinica() {
        return nomeClinica;
    }

    public void setNomeClinica(String nomeClinica) {
        this.nomeClinica = nomeClinica;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public List<Idoso> getIdosos() {
        return idosos;
    }

    public void setIdosos(List<Idoso> idosos) {
        this.idosos = idosos;
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

    public static MedicoBuilder builder() {
        return new MedicoBuilder();
    }

    public static class MedicoBuilder {
        private UUID id;
        private String keycloakSub;
        private String nomeCompleto;
        private String crm;
        private String especialidade;
        private String telefoneConsultorio;
        private String nomeClinica;
        private String cnpj;

        MedicoBuilder() {
        }

        public MedicoBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public MedicoBuilder keycloakSub(String keycloakSub) {
            this.keycloakSub = keycloakSub;
            return this;
        }

        public MedicoBuilder nomeCompleto(String nomeCompleto) {
            this.nomeCompleto = nomeCompleto;
            return this;
        }

        public MedicoBuilder crm(String crm) {
            this.crm = crm;
            return this;
        }

        public MedicoBuilder especialidade(String especialidade) {
            this.especialidade = especialidade;
            return this;
        }

        public MedicoBuilder telefoneConsultorio(String telefoneConsultorio) {
            this.telefoneConsultorio = telefoneConsultorio;
            return this;
        }

        public MedicoBuilder nomeClinica(String nomeClinica) {
            this.nomeClinica = nomeClinica;
            return this;
        }

        public MedicoBuilder cnpj(String cnpj) {
            this.cnpj = cnpj;
            return this;
        }

        public Medico build() {
            return new Medico(id, keycloakSub, nomeCompleto, crm, especialidade, telefoneConsultorio, nomeClinica, cnpj);
        }
    }
}
