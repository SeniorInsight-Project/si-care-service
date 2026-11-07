package com.seniorinsight.backendcore.mensageria;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Controle de mensagens já tratadas, na tabela {@code evento_processado}. */
@Component
class RegistroEventosProcessados {

    private final JdbcTemplate jdbc;

    RegistroEventosProcessados(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Registra a mensagem para o consumidor. Devolve {@code false} se ela já tinha sido registrada. O INSERT trava a
     * chave: duas entregas simultâneas da mesma mensagem não passam as duas.
     */
    boolean registrarSeNovo(String mensagemId, String consumidor) {
        return jdbc.update("""
                INSERT INTO evento_processado (mensagem_id, consumidor)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """, mensagemId, consumidor) == 1;
    }
}
