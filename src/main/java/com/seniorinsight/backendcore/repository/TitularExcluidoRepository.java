package com.seniorinsight.backendcore.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Titulares que já tiveram os dados excluídos, na tabela {@code titular_excluido}. Guarda só o hash SHA-256 do
 * identificador (sub ou id), que serve para recusar a recriação por um evento antigo sem manter o identificador.
 */
@Repository
public class TitularExcluidoRepository {

    private static final String ALGORITMO = "SHA-256";

    private final JdbcTemplate jdbc;

    public TitularExcluidoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void registrar(String identificador) {
        jdbc.update("""
                INSERT INTO titular_excluido (impressao)
                VALUES (?)
                ON CONFLICT DO NOTHING
                """, impressao(identificador));
    }

    public boolean contem(String identificador) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM titular_excluido WHERE impressao = ?)",
                Boolean.class, impressao(identificador)));
    }

    /**
     * Trava, até o fim da transação, o identificador: a exclusão e a criação do familiar com o mesmo sub não rodam
     * ao mesmo tempo, então o familiar não é recriado entre a exclusão e o seu commit.
     */
    public void travar(String identificador) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", resultado -> null, impressao(identificador));
    }

    private static String impressao(String identificador) {
        try {
            byte[] hash = MessageDigest.getInstance(ALGORITMO).digest(identificador.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITMO + " indisponível na JVM", e);
        }
    }
}
