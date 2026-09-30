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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entidade JPA para Vínculo de Rede entre Familiar e Idoso (HU-002 / DRS).
 * Mapeia permissões, papel de acesso e restrições de visibilidade em formato JSONB.
 */
@Entity
@Table(
        name = "vinculos_rede",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_vinculos_rede_familiar_idoso",
                columnNames = {"familiar_id", "idoso_id"}
        )
)
public class VinculoRede {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familiar_id", nullable = false)
    private Familiar familiar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idoso_id", nullable = false)
    private Idoso idoso;

    @Column(name = "grau_parentesco", nullable = false, length = 50)
    private String grauParentesco;

    @Column(name = "papel_acesso", nullable = false, length = 50)
    private String papelAcesso;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "restricoes_acesso", columnDefinition = "jsonb")
    private Map<String, Object> restricoesAcesso = new HashMap<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public VinculoRede() {
    }

    public VinculoRede(UUID id, Familiar familiar, Idoso idoso, String grauParentesco,
                        String papelAcesso, Map<String, Object> restricoesAcesso) {
        this.id = id;
        this.familiar = familiar;
        this.idoso = idoso;
        this.grauParentesco = grauParentesco;
        this.papelAcesso = papelAcesso;
        this.restricoesAcesso = restricoesAcesso != null ? restricoesAcesso : new HashMap<>();
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.criadoEm = now;
        this.atualizadoEm = now;
        if (this.restricoesAcesso == null) {
            this.restricoesAcesso = new HashMap<>();
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

    public Familiar getFamiliar() {
        return familiar;
    }

    public void setFamiliar(Familiar familiar) {
        this.familiar = familiar;
    }

    public Idoso getIdoso() {
        return idoso;
    }

    public void setIdoso(Idoso idoso) {
        this.idoso = idoso;
    }

    public String getGrauParentesco() {
        return grauParentesco;
    }

    public void setGrauParentesco(String grauParentesco) {
        this.grauParentesco = grauParentesco;
    }

    public String getPapelAcesso() {
        return papelAcesso;
    }

    public void setPapelAcesso(String papelAcesso) {
        this.papelAcesso = papelAcesso;
    }

    public Map<String, Object> getRestricoesAcesso() {
        return restricoesAcesso;
    }

    public void setRestricoesAcesso(Map<String, Object> restricoesAcesso) {
        this.restricoesAcesso = restricoesAcesso;
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

    public static VinculoRedeBuilder builder() {
        return new VinculoRedeBuilder();
    }

    public static class VinculoRedeBuilder {
        private UUID id;
        private Familiar familiar;
        private Idoso idoso;
        private String grauParentesco;
        private String papelAcesso;
        private Map<String, Object> restricoesAcesso = new HashMap<>();

        VinculoRedeBuilder() {
        }

        public VinculoRedeBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public VinculoRedeBuilder familiar(Familiar familiar) {
            this.familiar = familiar;
            return this;
        }

        public VinculoRedeBuilder idoso(Idoso idoso) {
            this.idoso = idoso;
            return this;
        }

        public VinculoRedeBuilder grauParentesco(String grauParentesco) {
            this.grauParentesco = grauParentesco;
            return this;
        }

        public VinculoRedeBuilder papelAcesso(String papelAcesso) {
            this.papelAcesso = papelAcesso;
            return this;
        }

        public VinculoRedeBuilder restricoesAcesso(Map<String, Object> restricoesAcesso) {
            this.restricoesAcesso = restricoesAcesso;
            return this;
        }

        public VinculoRede build() {
            return new VinculoRede(id, familiar, idoso, grauParentesco, papelAcesso, restricoesAcesso);
        }
    }
}
