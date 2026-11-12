package com.seniorinsight.backendcore.mensageria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.repository.ExclusaoDadosRepository;

/**
 * Eventos da outbox do idoso. Roda numa transação que volta atrás, para o relay não publicar o evento pendente de
 * teste antes da exclusão.
 */
@Transactional
class ExclusaoDadosRepositoryIntegracaoTest extends IntegracaoBase {

    @Autowired
    ExclusaoDadosRepository dados;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void apagaSoOsEventosPendentesDoIdoso() {
        UUID idoso = UUID.randomUUID();
        UUID pendente = evento(idoso, false);
        UUID publicado = evento(idoso, true);
        UUID pendenteDeOutroIdoso = evento(UUID.randomUUID(), false);

        dados.apagarEventosPendentesDoIdoso(idoso);

        assertThat(existe(pendente)).isFalse();
        assertThat(existe(publicado)).isTrue();
        assertThat(existe(pendenteDeOutroIdoso)).isTrue();
    }

    private UUID evento(UUID agregado, boolean publicado) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO outbox_evento (id, tipo_agregado, agregado_id, tipo_evento, payload, publicado_em)
                VALUES (?, 'consentimento', ?, 'consent.changed', '{}'::jsonb, CASE WHEN ? THEN now() END)
                """, id, agregado, publicado);
        return id;
    }

    private boolean existe(UUID id) {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE id = ?", Integer.class, id) == 1;
    }
}
