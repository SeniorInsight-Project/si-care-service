package com.seniorinsight.backendcore.mensageria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.seniorinsight.backendcore.service.LimpezaAvisosExclusaoRede;

/** O job apaga, no banco, os avisos vencidos com os destinatários, e deixa os dentro do prazo. */
@Transactional
class LimpezaAvisosIntegracaoTest extends IntegracaoBase {

    @Autowired
    LimpezaAvisosExclusaoRede limpeza;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void removeOsAvisosVencidosEMantemOsRecentes() {
        UUID vencido = aviso("now() - interval '8 days'");
        UUID recente = aviso("now() - interval '6 days'");

        limpeza.removerVencidos();

        assertThat(contar("aviso_exclusao_rede", vencido)).isZero();
        assertThat(contar("aviso_exclusao_destinatario", vencido)).isZero();
        assertThat(contar("aviso_exclusao_rede", recente)).isEqualTo(1);
        assertThat(contar("aviso_exclusao_destinatario", recente)).isEqualTo(1);
    }

    private UUID aviso(String criadoEm) {
        UUID protocolo = UUID.randomUUID();
        jdbc.update("INSERT INTO aviso_exclusao_rede (protocolo, primeiro_nome_idoso, criado_em) VALUES (?, 'Dona', "
                + criadoEm + ")", protocolo);
        jdbc.update("""
                INSERT INTO aviso_exclusao_destinatario (protocolo, usuario_id, nome_completo, email)
                VALUES (?, 'sub-ana', 'Ana Lima', 'ana@exemplo.com')
                """, protocolo);
        return protocolo;
    }

    private int contar(String tabela, UUID protocolo) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela + " WHERE protocolo = ?", Integer.class, protocolo);
    }
}
