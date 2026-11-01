package com.seniorinsight.backendcore.domain;

import java.time.Instant;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Entidade JPA para o perfil de Familiar (HU-002 / DRS).
 * Ligado ao usuário do Keycloak pela claim sub (keycloakSub).
 */
@Entity
@Table(name = "familiares")
public class Familiar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_sub", nullable = false, unique = true, length = 64)
    private String keycloakSub;

    @Column(length = 320)
    private String email;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(length = 20)
    private String telefone;

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @OneToMany(mappedBy = "familiar", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VinculoRede> vinculos = new ArrayList<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public Familiar() {
    }

    public Familiar(UUID id, String keycloakSub, String nomeCompleto, String telefone, String cpf) {
        this.id = id;
        this.keycloakSub = keycloakSub;
        this.nomeCompleto = nomeCompleto;
        this.telefone = telefone;
        this.cpf = cpf;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public List<VinculoRede> getVinculos() {
        return vinculos;
    }

    public void setVinculos(List<VinculoRede> vinculos) {
        this.vinculos = vinculos;
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

    public void addVinculo(VinculoRede vinculo) {
        vinculos.add(vinculo);
        vinculo.setFamiliar(this);
    }

    public void removeVinculo(VinculoRede vinculo) {
        vinculos.remove(vinculo);
        vinculo.setFamiliar(null);
    }

    // --- Builder Pattern ---

    public static FamiliarBuilder builder() {
        return new FamiliarBuilder();
    }

    public static class FamiliarBuilder {
        private UUID id;
        private String keycloakSub;
        private String email;
        private String nomeCompleto;
        private String telefone;
        private String cpf;

        FamiliarBuilder() {
        }

        public FamiliarBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public FamiliarBuilder keycloakSub(String keycloakSub) {
            this.keycloakSub = keycloakSub;
            return this;
        }

        public FamiliarBuilder email(String email) {
            this.email = email;
            return this;
        }

        public FamiliarBuilder nomeCompleto(String nomeCompleto) {
            this.nomeCompleto = nomeCompleto;
            return this;
        }

        public FamiliarBuilder telefone(String telefone) {
            this.telefone = telefone;
            return this;
        }

        public FamiliarBuilder cpf(String cpf) {
            this.cpf = cpf;
            return this;
        }

        public Familiar build() {
            Familiar familiar = new Familiar(id, keycloakSub, nomeCompleto, telefone, cpf);
            familiar.setEmail(email);
            return familiar;
        }
    }
}
