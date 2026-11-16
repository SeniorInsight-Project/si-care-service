package com.seniorinsight.backendcore.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Remoção dos dados do titular (HU-022). O idoso e o familiar são apagados por SQL: as tabelas ligadas por chave
 * estrangeira com {@code ON DELETE CASCADE} caem junto. O que não tem chave estrangeira (autoria por {@code sub}, e-mail
 * do convidado, eventos pendentes da outbox) é tratado à mão aqui.
 */
@Repository
public class ExclusaoDadosRepository {

    /** Autor gravado no lugar do {@code sub} de um titular excluído, nos registros que precisam continuar existindo. */
    public static final String AUTORIA_ANONIMIZADA = "titular-excluido";

    private final JdbcTemplate jdbc;

    public ExclusaoDadosRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Apaga o idoso; vínculos, convites, consentimentos, curatelas, contatos, rotinas, histórico, alarmes, telemetria e auditoria caem em cascata. */
    public void apagarIdoso(UUID idosoId) {
        jdbc.update("DELETE FROM idosos WHERE id = ?", idosoId);
    }

    /** Eventos do idoso (agregado = idoso) que ainda não saíram para o broker. */
    public void apagarEventosPendentesDoIdoso(UUID idosoId) {
        jdbc.update("DELETE FROM outbox_evento WHERE publicado_em IS NULL AND agregado_id = ?", idosoId);
    }

    /** Apaga o familiar; os vínculos com os idosos e as curatelas caem em cascata. */
    public void apagarFamiliar(String sub) {
        jdbc.update("DELETE FROM familiares WHERE keycloak_sub = ?", sub);
    }

    /** Convites em que o familiar era o convidado: o e-mail dele está no convite, que não tem chave para o familiar. */
    public void apagarConvitesDoEmail(String email) {
        jdbc.update("DELETE FROM convites_rede WHERE lower(email) = lower(?)", email);
    }

    /**
     * Tira o {@code sub} do familiar dos registros que ele fez e que continuam na rede do idoso: consentimentos,
     * auditoria, convites emitidos e histórico de medicação.
     */
    public void anonimizarAutoria(String sub) {
        jdbc.update("UPDATE consentimentos SET autor_sub = ? WHERE autor_sub = ?", AUTORIA_ANONIMIZADA, sub);
        jdbc.update("UPDATE auditoria_idosos SET autor_sub = ? WHERE autor_sub = ?", AUTORIA_ANONIMIZADA, sub);
        jdbc.update("UPDATE convites_rede SET criado_por_sub = NULL WHERE criado_por_sub = ?", sub);
        jdbc.update("""
                UPDATE historicos_medicacao SET registrado_por_sub = NULL, registrado_por_nome = NULL
                 WHERE registrado_por_sub = ?
                """, sub);
    }
}
