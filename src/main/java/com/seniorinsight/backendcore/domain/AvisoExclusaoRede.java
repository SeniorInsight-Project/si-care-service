package com.seniorinsight.backendcore.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Cópia de quem deve ser avisado quando os dados de um idoso são excluídos (HU-022, cenário 1). É gravada antes da
 * exclusão, porque depois dela a rede não existe mais, e fica disponível por um prazo curto para o notification-service
 * consultar pelo protocolo. Um aviso sem destinatários significa que o pedido foi tratado e não havia ninguém a avisar.
 */
@Entity
@Table(name = "aviso_exclusao_rede")
public class AvisoExclusaoRede {

    @Id
    private UUID protocolo;

    @Column(name = "primeiro_nome_idoso", length = 150)
    private String primeiroNomeIdoso;

    @ElementCollection
    @CollectionTable(name = "aviso_exclusao_destinatario", joinColumns = @JoinColumn(name = "protocolo"))
    private List<DestinatarioAviso> destinatarios = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected AvisoExclusaoRede() {
    }

    public AvisoExclusaoRede(UUID protocolo, String primeiroNomeIdoso, List<DestinatarioAviso> destinatarios,
                             Instant criadoEm) {
        this.protocolo = protocolo;
        this.primeiroNomeIdoso = primeiroNomeIdoso;
        this.destinatarios = new ArrayList<>(destinatarios);
        this.criadoEm = criadoEm;
    }

    public UUID getProtocolo() {
        return protocolo;
    }

    public String getPrimeiroNomeIdoso() {
        return primeiroNomeIdoso;
    }

    public List<DestinatarioAviso> getDestinatarios() {
        return destinatarios;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
