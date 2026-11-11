package com.seniorinsight.backendcore.repository;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import com.seniorinsight.backendcore.domain.TipoConsentimento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConsentimentoRepository {

    public static final String REDE = "REDE";

    private final JdbcTemplate jdbc;

    public ConsentimentoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean curatelaAtiva(UUID idosoId, String familiarSub) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM curatelas_idoso c
                    JOIN familiares f ON f.id = c.familiar_id
                    JOIN vinculos_rede v ON v.familiar_id = c.familiar_id AND v.idoso_id = c.idoso_id
                    WHERE c.idoso_id = ? AND f.keycloak_sub = ?
                      AND c.registrada_em <= now() AND c.revogada_em IS NULL
                )
                """, Boolean.class, idosoId, familiarSub));
    }

    public boolean existeIdoso(UUID idosoId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM idosos WHERE id = ?)", Boolean.class, idosoId));
    }

    public boolean bloquearIdoso(UUID idosoId) {
        return !jdbc.queryForList("SELECT id FROM idosos WHERE id = ? FOR UPDATE", UUID.class, idosoId).isEmpty();
    }

    public Map<TipoConsentimento, Decisao> buscarAtuais(UUID idosoId) {
        return jdbc.query("""
                SELECT DISTINCT ON (tipo) tipo, autorizado, versao_termo
                  FROM consentimentos
                 WHERE idoso_id = ? AND destinatario = ?
                 ORDER BY tipo, id DESC
                """, rs -> {
                    var atuais = new EnumMap<TipoConsentimento, Decisao>(TipoConsentimento.class);
                    while (rs.next()) {
                        atuais.put(TipoConsentimento.valueOf(rs.getString("tipo")),
                                new Decisao(rs.getBoolean("autorizado"), rs.getString("versao_termo")));
                    }
                    return atuais;
                }, idosoId, REDE);
    }

    public void registrar(UUID idosoId, TipoConsentimento tipo, Decisao decisao, String autorSub) {
        jdbc.update("""
                INSERT INTO consentimentos (idoso_id, tipo, destinatario, autorizado, versao_termo, autor_sub)
                VALUES (?, ?, ?, ?, ?, ?)
                """, idosoId, tipo.name(), REDE, decisao.autorizado(), decisao.versaoTermo(), autorSub);
    }

    public record Decisao(boolean autorizado, String versaoTermo) {
    }
}
