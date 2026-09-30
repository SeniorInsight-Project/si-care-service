package com.seniorinsight.backendcore.model;

import java.time.Instant;
import java.util.UUID;

import com.seniorinsight.backendcore.domain.Familiar;
import com.seniorinsight.backendcore.domain.Medico;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Entidade JPA para mapeamento da tabela 'usuarios'.
 * Suporta HU-001 (SSO Google/Apple), HU-002 (Login Local), HU-003 (Recuperação) e HU-004 (MFA).
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash")
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatusUsuario status = StatusUsuario.ATIVO;

    @Column(name = "mfa_habilitado", nullable = false)
    private Boolean mfaHabilitado = false;

    @Column(name = "mfa_segredo")
    private String mfaSegredo;

    @Enumerated(EnumType.STRING)
    @Column(name = "provedor_sso", nullable = false, length = 20)
    private ProvedorSso provedorSso = ProvedorSso.LOCAL;

    @Column(name = "sso_id")
    private String ssoId;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Familiar familiar;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Medico medico;

    public Usuario() {
    }

    public Usuario(UUID id, String email, String senhaHash, StatusUsuario status, Boolean mfaHabilitado,
                   String mfaSegredo, ProvedorSso provedorSso, String ssoId, Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.email = email;
        this.senhaHash = senhaHash;
        this.status = status != null ? status : StatusUsuario.ATIVO;
        this.mfaHabilitado = mfaHabilitado != null ? mfaHabilitado : false;
        this.mfaSegredo = mfaSegredo;
        this.provedorSso = provedorSso != null ? provedorSso : ProvedorSso.LOCAL;
        this.ssoId = ssoId;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.criadoEm = now;
        this.atualizadoEm = now;
        if (this.status == null) {
            this.status = StatusUsuario.ATIVO;
        }
        if (this.mfaHabilitado == null) {
            this.mfaHabilitado = false;
        }
        if (this.provedorSso == null) {
            this.provedorSso = ProvedorSso.LOCAL;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public StatusUsuario getStatus() {
        return status;
    }

    public void setStatus(StatusUsuario status) {
        this.status = status;
    }

    public Boolean getMfaHabilitado() {
        return mfaHabilitado;
    }

    public void setMfaHabilitado(Boolean mfaHabilitado) {
        this.mfaHabilitado = mfaHabilitado;
    }

    public String getMfaSegredo() {
        return mfaSegredo;
    }

    public void setMfaSegredo(String mfaSegredo) {
        this.mfaSegredo = mfaSegredo;
    }

    public ProvedorSso getProvedorSso() {
        return provedorSso;
    }

    public void setProvedorSso(ProvedorSso provedorSso) {
        this.provedorSso = provedorSso;
    }

    public String getSsoId() {
        return ssoId;
    }

    public void setSsoId(String ssoId) {
        this.ssoId = ssoId;
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

    public Familiar getFamiliar() {
        return familiar;
    }

    public void setFamiliar(Familiar familiar) {
        this.familiar = familiar;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    // --- Builder Pattern ---

    public static UsuarioBuilder builder() {
        return new UsuarioBuilder();
    }

    public static class UsuarioBuilder {
        private UUID id;
        private String email;
        private String senhaHash;
        private StatusUsuario status = StatusUsuario.ATIVO;
        private Boolean mfaHabilitado = false;
        private String mfaSegredo;
        private ProvedorSso provedorSso = ProvedorSso.LOCAL;
        private String ssoId;
        private Instant criadoEm;
        private Instant atualizadoEm;

        UsuarioBuilder() {
        }

        public UsuarioBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public UsuarioBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UsuarioBuilder senhaHash(String senhaHash) {
            this.senhaHash = senhaHash;
            return this;
        }

        public UsuarioBuilder status(StatusUsuario status) {
            this.status = status;
            return this;
        }

        public UsuarioBuilder mfaHabilitado(Boolean mfaHabilitado) {
            this.mfaHabilitado = mfaHabilitado;
            return this;
        }

        public UsuarioBuilder mfaSegredo(String mfaSegredo) {
            this.mfaSegredo = mfaSegredo;
            return this;
        }

        public UsuarioBuilder provedorSso(ProvedorSso provedorSso) {
            this.provedorSso = provedorSso;
            return this;
        }

        public UsuarioBuilder ssoId(String ssoId) {
            this.ssoId = ssoId;
            return this;
        }

        public UsuarioBuilder criadoEm(Instant criadoEm) {
            this.criadoEm = criadoEm;
            return this;
        }

        public UsuarioBuilder atualizadoEm(Instant atualizadoEm) {
            this.atualizadoEm = atualizadoEm;
            return this;
        }

        public Usuario build() {
            return new Usuario(id, email, senhaHash, status, mfaHabilitado, mfaSegredo, provedorSso, ssoId, criadoEm, atualizadoEm);
        }
    }
}
