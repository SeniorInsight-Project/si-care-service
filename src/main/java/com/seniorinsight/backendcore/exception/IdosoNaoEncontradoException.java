package com.seniorinsight.backendcore.exception;

import java.util.UUID;

/**
 * Exceção lançada quando um idoso não é localizado na base de dados.
 */
public class IdosoNaoEncontradoException extends RuntimeException {

    public IdosoNaoEncontradoException(UUID idosoId) {
        super("Idoso não encontrado com o identificador: " + idosoId);
    }

    public IdosoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
