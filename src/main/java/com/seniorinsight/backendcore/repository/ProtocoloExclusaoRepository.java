package com.seniorinsight.backendcore.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Protocolos de exclusão já tratados, na tabela {@code exclusao_protocolo}. */
@Repository
public class ProtocoloExclusaoRepository {

    private final JdbcTemplate jdbc;

    public ProtocoloExclusaoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Reserva o protocolo. Devolve {@code false} se ele já tinha sido reservado: a mesma exclusão pode chegar de novo
     * com outro {@code message_id} e não deve ser refeita nem confirmada outra vez. O INSERT trava a chave, então duas
     * entregas simultâneas não passam as duas.
     */
    public boolean reservar(UUID protocolo) {
        return jdbc.update("""
                INSERT INTO exclusao_protocolo (protocolo)
                VALUES (?)
                ON CONFLICT DO NOTHING
                """, protocolo) == 1;
    }
}
