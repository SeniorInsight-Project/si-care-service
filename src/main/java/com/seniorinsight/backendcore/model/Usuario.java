package com.seniorinsight.backendcore.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade JPA para mapeamento da tabela 'usuarios'.
 * Suporta HU-001 (SSO Google/Apple), HU-002 (Login Local), HU-003 (Recuperação) e HU-004 (MFA).
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    @Builder.Default
    private StatusUsuario status = StatusUsuario.ATIVO;

    @Column(name = "mfa_habilitado", nullable = false)
    @Builder.Default
    private Boolean mfaHabilitado = false;

    @Column(name = "mfa_segredo")
    private String mfaSegredo;

    @Enumerated(EnumType.STRING)
    @Column(name = "provedor_sso", nullable = false, length = 20)
    @Builder.Default
    private ProvedorSso provedorSso = ProvedorSso.LOCAL;

    @Column(name = "sso_id")
    private String ssoId;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

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
}
