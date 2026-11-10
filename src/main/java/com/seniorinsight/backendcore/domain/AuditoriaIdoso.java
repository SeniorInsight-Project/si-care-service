package com.seniorinsight.backendcore.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro de auditoria da edição do cadastro do idoso (HU-009): quem alterou, quando, e os valores de cada campo
 * alterado antes e depois.
 */
@Entity
@Table(name = "auditoria_idosos")
public class AuditoriaIdoso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idoso_id", nullable = false)
    private UUID idosoId;

    @Column(name = "autor_sub", nullable = false, length = 64)
    private String autorSub;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valores_anteriores", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> valoresAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valores_novos", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> valoresNovos;

    protected AuditoriaIdoso() {
    }

    public AuditoriaIdoso(UUID idosoId, String autorSub, Instant ocorridoEm,
                          Map<String, Object> valoresAnteriores, Map<String, Object> valoresNovos) {
        this.idosoId = idosoId;
        this.autorSub = autorSub;
        this.ocorridoEm = ocorridoEm;
        this.valoresAnteriores = valoresAnteriores;
        this.valoresNovos = valoresNovos;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdosoId() {
        return idosoId;
    }

    public String getAutorSub() {
        return autorSub;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public Map<String, Object> getValoresAnteriores() {
        return valoresAnteriores;
    }

    public Map<String, Object> getValoresNovos() {
        return valoresNovos;
    }
}
