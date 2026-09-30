package com.seniorinsight.backendcore.exception;

import java.util.UUID;

/**
 * Exceção lançada quando um alarme de emergência não é localizado na base de dados (HU35).
 */
public class AlarmeNaoEncontradoException extends RuntimeException {

    public AlarmeNaoEncontradoException(UUID alarmeId) {
        super("Alarme de emergência não encontrado com o identificador: " + alarmeId);
    }

    public AlarmeNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
