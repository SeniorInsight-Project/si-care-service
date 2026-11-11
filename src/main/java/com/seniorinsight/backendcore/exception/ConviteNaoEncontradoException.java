package com.seniorinsight.backendcore.exception;

/**
 * Exceção lançada quando não existe convite para o token informado na consulta.
 */
public class ConviteNaoEncontradoException extends RuntimeException {

    public ConviteNaoEncontradoException(String message) {
        super(message);
    }
}
