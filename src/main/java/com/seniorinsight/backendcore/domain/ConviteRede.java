package com.seniorinsight.backendcore.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;


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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Entidade JPA para Convite da Rede de Proteção (HU16).
 * Modela o convite com token temporal gerado via UUID, id do idoso, papel de acesso sugerido e expiração de 48h.
 */
@Entity
@Table(name = "convites_rede")
public class ConviteRede {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idoso_id", nullable = false)
    private Idoso idoso;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(name = "papel_acesso_sugerido", nullable = false, length = 50)
    private String papelAcessoSugerido;

    @Column(name = "grau_parentesco_sugerido", length = 50)
    private String grauParentescoSugerido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConvite status = StatusConvite.PENDENTE;

    @Column(name = "data_expiracao", nullable = false)
    private Instant dataExpiracao;

    @Column(name = "criado_por_sub", length = 64)
    private String criadoPorSub;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public ConviteRede() {
    }

    public ConviteRede(UUID id, Idoso idoso, String email, String token, String papelAcessoSugerido,
                       String grauParentescoSugerido, StatusConvite status, Instant dataExpiracao,
                       String criadoPorSub, Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.idoso = idoso;
        this.email = email;
        this.token = token;
        this.papelAcessoSugerido = papelAcessoSugerido;
        this.grauParentescoSugerido = grauParentescoSugerido;
        this.status = status != null ? status : StatusConvite.PENDENTE;
        this.dataExpiracao = dataExpiracao;
        this.criadoPorSub = criadoPorSub;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.criadoEm = now;
        this.atualizadoEm = now;

        if (this.token == null || this.token.isBlank()) {
            this.token = UUID.randomUUID().toString();
        }

        if (this.dataExpiracao == null) {
            this.dataExpiracao = now.plus(48, ChronoUnit.HOURS);
        }

        if (this.status == null) {
            this.status = StatusConvite.PENDENTE;
        }

        if (this.grauParentescoSugerido == null || this.grauParentescoSugerido.isBlank()) {
            this.grauParentescoSugerido = "Familiar";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = Instant.now();
    }

    // --- Métodos de Domínio e Ciclo de Vida ---

    /**
     * Verifica se o convite já ultrapassou o prazo de validade temporal de 48 horas.
     */
    public boolean isExpirado() {
        return this.status == StatusConvite.EXPIRADO || Instant.now().isAfter(this.dataExpiracao);
    }

    /**
     * Verifica se o convite se encontra pendente e ainda dentro da janela de validade.
     */
    public boolean isPendente() {
        return this.status == StatusConvite.PENDENTE && !isExpirado();
    }

    /**
     * Marca o convite como aceito.
     */
    public void aceitar() {
        this.status = StatusConvite.ACEITO;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Marca o convite como expirado.
     */
    public void expirar() {
        this.status = StatusConvite.EXPIRADO;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Cancela o convite (por exemplo, quando substituído por um novo).
     */
    public void cancelar() {
        this.status = StatusConvite.CANCELADO;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPapelAcessoSugerido() {
        return papelAcessoSugerido;
    }

    public void setPapelAcessoSugerido(String papelAcessoSugerido) {
        this.papelAcessoSugerido = papelAcessoSugerido;
    }

    public String getGrauParentescoSugerido() {
        return grauParentescoSugerido;
    }

    public void setGrauParentescoSugerido(String grauParentescoSugerido) {
        this.grauParentescoSugerido = grauParentescoSugerido;
    }

    public StatusConvite getStatus() {
        return status;
    }

    public void setStatus(StatusConvite status) {
        this.status = status;
    }

    public Instant getDataExpiracao() {
        return dataExpiracao;
    }

    public void setDataExpiracao(Instant dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    public String getCriadoPorSub() {
        return criadoPorSub;
    }

    public void setCriadoPorSub(String criadoPorSub) {
        this.criadoPorSub = criadoPorSub;
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

    public static ConviteRedeBuilder builder() {
        return new ConviteRedeBuilder();
    }

    public static class ConviteRedeBuilder {
        private UUID id;
        private Idoso idoso;
        private String email;
        private String token;
        private String papelAcessoSugerido;
        private String grauParentescoSugerido;
        private StatusConvite status = StatusConvite.PENDENTE;
        private Instant dataExpiracao;
        private String criadoPorSub;
        private Instant criadoEm;
        private Instant atualizadoEm;

        ConviteRedeBuilder() {
        }

        public ConviteRedeBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public ConviteRedeBuilder idoso(Idoso idoso) {
            this.idoso = idoso;
            return this;
        }

        public ConviteRedeBuilder email(String email) {
            this.email = email;
            return this;
        }

        public ConviteRedeBuilder token(String token) {
            this.token = token;
            return this;
        }

        public ConviteRedeBuilder papelAcessoSugerido(String papelAcessoSugerido) {
            this.papelAcessoSugerido = papelAcessoSugerido;
            return this;
        }

        public ConviteRedeBuilder grauParentescoSugerido(String grauParentescoSugerido) {
            this.grauParentescoSugerido = grauParentescoSugerido;
            return this;
        }

        public ConviteRedeBuilder status(StatusConvite status) {
            this.status = status;
            return this;
        }

        public ConviteRedeBuilder dataExpiracao(Instant dataExpiracao) {
            this.dataExpiracao = dataExpiracao;
            return this;
        }

        public ConviteRedeBuilder criadoPorSub(String criadoPorSub) {
            this.criadoPorSub = criadoPorSub;
            return this;
        }

        public ConviteRedeBuilder criadoEm(Instant criadoEm) {
            this.criadoEm = criadoEm;
            return this;
        }

        public ConviteRedeBuilder atualizadoEm(Instant atualizadoEm) {
            this.atualizadoEm = atualizadoEm;
            return this;
        }

        public ConviteRede build() {
            return new ConviteRede(id, idoso, email, token, papelAcessoSugerido, grauParentescoSugerido,
                    status, dataExpiracao, criadoPorSub, criadoEm, atualizadoEm);
        }
    }
}
